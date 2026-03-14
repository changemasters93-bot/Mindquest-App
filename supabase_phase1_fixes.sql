-- ================================================================
-- PHASE 1: Performance & Reliability Fixes
-- Run this migration against your Supabase database.
-- Safe to run multiple times (all CREATE INDEX IF NOT EXISTS).
-- ================================================================

-- ────────────────────────────────────────────────────────────────
-- 1. CRITICAL INDEXES (Zero custom indexes exist on core tables)
--    Without these, every RPC does sequential scans on 300K+ rows.
-- ────────────────────────────────────────────────────────────────

-- ─── Users ──────────────────────────────────────────────────────
-- Leaderboard: ORDER BY total_xp DESC
CREATE INDEX IF NOT EXISTS idx_users_total_xp ON public.users(total_xp DESC);
-- User lookup by grade (dashboard, filtering)
CREATE INDEX IF NOT EXISTS idx_users_grade ON public.users(grade_id);
-- Streak/active tracking
CREATE INDEX IF NOT EXISTS idx_users_last_active ON public.users(last_active_at DESC);

-- ─── Modules ────────────────────────────────────────────────────
-- Module listing by grade (most common query)
CREATE INDEX IF NOT EXISTS idx_modules_grade_status ON public.modules(grade_id, status) WHERE is_active = true;
-- Module ordering (column is display_order, not sort_order)
CREATE INDEX IF NOT EXISTS idx_modules_sort ON public.modules(display_order);

-- ─── Chapters ───────────────────────────────────────────────────
-- Chapters by module (chapter listing screen)
CREATE INDEX IF NOT EXISTS idx_chapters_module ON public.chapters(module_id, sort_order);
-- Chapter status filtering
CREATE INDEX IF NOT EXISTS idx_chapters_status ON public.chapters(status) WHERE is_active = true;

-- ─── Quizzes ────────────────────────────────────────────────────
-- Quizzes by chapter (quiz listing screen)
CREATE INDEX IF NOT EXISTS idx_quizzes_chapter ON public.quizzes(chapter_id, sort_order);
-- Quiz type filtering (IQ, practice, etc.)
CREATE INDEX IF NOT EXISTS idx_quizzes_type ON public.quizzes(quiz_type);

-- ─── Questions ──────────────────────────────────────────────────
-- Questions by quiz (the hottest query: 50 questions per quiz)
CREATE INDEX IF NOT EXISTS idx_questions_quiz ON public.questions(quiz_id, sort_order);

-- ─── Question Options ───────────────────────────────────────────
-- Options by question (loaded for every question render)
CREATE INDEX IF NOT EXISTS idx_options_question ON public.question_options(question_id, sort_order);

-- ─── Match Pairs ────────────────────────────────────────────────
-- Match pairs by question
CREATE INDEX IF NOT EXISTS idx_match_pairs_question ON public.match_pairs(question_id);

-- ─── Quiz Attempts ──────────────────────────────────────────────
-- Attempts by user+quiz (idempotency check, cooldown, best score)
CREATE INDEX IF NOT EXISTS idx_attempts_user_quiz ON public.quiz_attempts(user_id, quiz_id, created_at DESC);

-- ─── Daily Challenges ───────────────────────────────────────────
-- Today's challenge lookup
CREATE INDEX IF NOT EXISTS idx_daily_challenge_date ON public.daily_challenges(challenge_date, grade_id);

-- ─── Tournaments ────────────────────────────────────────────────
-- Active tournament lookup
CREATE INDEX IF NOT EXISTS idx_tournaments_status ON public.tournaments(status);
-- Tournament entries by tournament (leaderboard, participant count)
CREATE INDEX IF NOT EXISTS idx_tournament_entries_tid ON public.tournament_entries(tournament_id, score DESC);
-- Tournament entry lookup by user
CREATE INDEX IF NOT EXISTS idx_tournament_entries_user ON public.tournament_entries(user_id, tournament_id);
-- Tournament questions
CREATE INDEX IF NOT EXISTS idx_tournament_questions ON public.tournament_questions(tournament_id, sort_order);


-- ════════════════════════════════════════════════════════════════
-- 2. FIX: submit_quiz_attempt — Race Condition on XP/Streak
--
--    Problem: SELECT level/last_active_at then UPDATE creates a
--    TOCTOU race under concurrent requests. Two simultaneous
--    submissions can both read the same old values.
--
--    Fix: Use SELECT ... FOR UPDATE to lock the user row before
--    reading, and merge the IQ score update into the same UPDATE
--    to avoid a second write.
-- ════════════════════════════════════════════════════════════════

-- Drop existing function first (parameter name changed from 'payload' to 'p_payload')
DROP FUNCTION IF EXISTS public.submit_quiz_attempt(JSON);

CREATE OR REPLACE FUNCTION public.submit_quiz_attempt(p_payload JSON)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_user_id UUID     := (p_payload->>'userId')::UUID;
    v_quiz_id UUID     := (p_payload->>'quizId')::UUID;
    v_answers JSON     := p_payload->'answers';
    v_time    INT      := COALESCE((p_payload->>'timeTakenSecs')::INT, 0);
    v_idemp   TEXT     := p_payload->>'idempotencyKey';
    v_attempt_id UUID;
    v_score INT; v_total INT; v_xp INT; v_max_xp INT;
    v_is_replay BOOLEAN;
    v_chapter_id UUID;
    v_old_level INT; v_new_level INT; v_total_xp BIGINT;
    v_last_active_date DATE;
    v_rank INT;
    v_next_quiz UUID;
    v_cooldown INT;
    v_last_attempt TIMESTAMPTZ;
    v_iq_score NUMERIC;
    v_is_iq BOOLEAN;
BEGIN
    -- 1. Idempotency check (same key = duplicate submission)
    IF v_idemp IS NOT NULL THEN
        SELECT id INTO v_attempt_id FROM public.quiz_attempts
        WHERE idempotency_key = v_idemp LIMIT 1;
        IF v_attempt_id IS NOT NULL THEN
            SELECT total_xp, level INTO v_total_xp, v_new_level
            FROM public.users WHERE id = v_user_id;
            RETURN json_build_object(
                'status','duplicate','attempt_id',v_attempt_id,'score',v_score,
                'total_questions',v_total,'xp_earned',0,'total_xp',v_total_xp,
                'level',v_new_level,'level_changed',false,'rank_global',0,
                'is_replay',true,'next_quiz_id',NULL,'iq_score',NULL);
        END IF;
    END IF;

    -- 2. Cooldown enforcement
    SELECT cooldown_hours INTO v_cooldown FROM public.quizzes WHERE id = v_quiz_id;
    IF v_cooldown IS NOT NULL THEN
        SELECT MAX(created_at) INTO v_last_attempt FROM public.quiz_attempts
        WHERE user_id = v_user_id AND quiz_id = v_quiz_id;
        IF v_last_attempt IS NOT NULL AND v_last_attempt + (v_cooldown || ' hours')::interval > now() THEN
            RETURN json_build_object(
                'status', 'cooldown',
                'message', 'Quiz is on cooldown. Try again later.',
                'unlocks_at', v_last_attempt + (v_cooldown || ' hours')::interval
            );
        END IF;
    END IF;

    -- 3. Score calculation
    SELECT COUNT(*) INTO v_total FROM json_array_elements(v_answers);
    SELECT COUNT(*) INTO v_score FROM json_array_elements(v_answers) e WHERE (e->>'is_correct')::boolean;
    SELECT EXISTS(SELECT 1 FROM public.quiz_attempts WHERE user_id = v_user_id AND quiz_id = v_quiz_id) INTO v_is_replay;
    SELECT max_xp, chapter_id INTO v_max_xp, v_chapter_id FROM public.quizzes WHERE id = v_quiz_id;
    v_max_xp := COALESCE(v_max_xp, 100);
    v_xp := CASE WHEN v_is_replay THEN 0 ELSE ROUND(v_max_xp * (v_score::numeric / NULLIF(v_total, 0))) END;

    -- 4. Insert attempt
    INSERT INTO public.quiz_attempts (user_id, quiz_id, score, total_questions, xp_earned, time_taken_secs, answers, idempotency_key)
    VALUES (v_user_id, v_quiz_id, v_score, v_total, v_xp, v_time, v_answers::jsonb, v_idemp) RETURNING id INTO v_attempt_id;

    -- 5. *** FIX: Lock user row first to prevent TOCTOU race ***
    SELECT level, last_active_at::date
    INTO v_old_level, v_last_active_date
    FROM public.users
    WHERE id = v_user_id
    FOR UPDATE;  -- Row-level lock: blocks concurrent updates until this TX commits

    -- 6. Check if this is an IQ quiz (do it before the UPDATE so we can merge)
    SELECT EXISTS(SELECT 1 FROM public.quizzes WHERE id = v_quiz_id AND quiz_type = 'iq') INTO v_is_iq;
    IF v_is_iq THEN
        v_iq_score := ROUND(55 + (v_score * 105.0 / NULLIF(v_total, 0)));
    END IF;

    -- 7. *** FIX: Single atomic UPDATE for XP, level, streak, AND IQ score ***
    UPDATE public.users SET
        total_xp = total_xp + v_xp,
        level = CASE
            WHEN total_xp + v_xp >= 5000 THEN 10
            WHEN total_xp + v_xp >= 4000 THEN 9
            WHEN total_xp + v_xp >= 3200 THEN 8
            WHEN total_xp + v_xp >= 2500 THEN 7
            WHEN total_xp + v_xp >= 1900 THEN 6
            WHEN total_xp + v_xp >= 1400 THEN 5
            WHEN total_xp + v_xp >= 1000 THEN 4
            WHEN total_xp + v_xp >= 600  THEN 3
            WHEN total_xp + v_xp >= 300  THEN 2
            ELSE 1
        END,
        last_active_at = now(),
        streak_current = CASE
            WHEN v_last_active_date IS NULL THEN 1
            WHEN v_last_active_date = CURRENT_DATE THEN streak_current
            WHEN v_last_active_date = CURRENT_DATE - 1 THEN streak_current + 1
            ELSE 1
        END,
        streak_best = GREATEST(streak_best, CASE
            WHEN v_last_active_date IS NULL THEN 1
            WHEN v_last_active_date = CURRENT_DATE THEN streak_current
            WHEN v_last_active_date = CURRENT_DATE - 1 THEN streak_current + 1
            ELSE 1
        END),
        -- *** FIX: Merge IQ score into same UPDATE (was a separate UPDATE) ***
        iq_best_score = CASE
            WHEN v_is_iq THEN GREATEST(COALESCE(iq_best_score, 0), v_iq_score)
            ELSE iq_best_score
        END
    WHERE id = v_user_id
    RETURNING total_xp, level INTO v_total_xp, v_new_level;

    -- 8. Global rank (uses the new idx_users_total_xp index)
    SELECT COUNT(*) + 1 INTO v_rank FROM public.users WHERE total_xp > v_total_xp;

    -- 9. Next quiz in chapter
    SELECT q.id INTO v_next_quiz FROM public.quizzes q
    WHERE q.chapter_id = v_chapter_id
      AND q.sort_order > (SELECT sort_order FROM public.quizzes WHERE id = v_quiz_id)
      AND q.is_active
    ORDER BY q.sort_order LIMIT 1;

    RETURN json_build_object(
        'status','success','attempt_id',v_attempt_id,'score',v_score,
        'total_questions',v_total,'xp_earned',v_xp,'total_xp',v_total_xp,
        'level',v_new_level,'level_changed',(v_new_level > v_old_level),
        'rank_global',v_rank,'is_replay',v_is_replay,'next_quiz_id',v_next_quiz,
        'iq_score',v_iq_score);
END; $$;

GRANT EXECUTE ON FUNCTION public.submit_quiz_attempt(JSON) TO anon, authenticated;


-- ════════════════════════════════════════════════════════════════
-- 3. FIX: start_tournament — Race Condition on max_participants
--
--    Problem: COUNT(*) then INSERT allows two users to both read
--    count=99 when max=100, and both get in (overshooting to 101).
--
--    Fix: Use pg_advisory_xact_lock on the tournament ID to
--    serialize the check-and-insert block. Lock is released
--    automatically when the transaction commits/rolls back.
-- ════════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION public.start_tournament(p_user_id UUID, p_tournament_id UUID)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_entry_id UUID;
    v_questions JSON;
    v_time_limit INT;
    v_existing TEXT;
    v_max_part INT;
    v_current_part INT;
BEGIN
    -- Check if user already has an entry
    SELECT te.status INTO v_existing FROM public.tournament_entries te
    WHERE te.tournament_id = p_tournament_id AND te.user_id = p_user_id;

    IF v_existing IS NOT NULL AND v_existing NOT IN ('not_started') THEN
        RAISE EXCEPTION 'Already started or completed';
    END IF;

    -- Get tournament config
    SELECT max_participants, time_limit_seconds INTO v_max_part, v_time_limit
    FROM public.tournaments WHERE id = p_tournament_id;

    -- *** FIX: Advisory lock to serialize participant count check ***
    -- Converts the tournament UUID to two INT4s for pg_advisory_xact_lock
    IF v_max_part IS NOT NULL AND v_existing IS NULL THEN
        PERFORM pg_advisory_xact_lock(
            ('x' || left(replace(p_tournament_id::text, '-', ''), 8))::bit(32)::int,
            ('x' || right(replace(p_tournament_id::text, '-', ''), 8))::bit(32)::int
        );

        SELECT COUNT(*) INTO v_current_part FROM public.tournament_entries
        WHERE tournament_id = p_tournament_id;
        IF v_current_part >= v_max_part THEN
            RAISE EXCEPTION 'Tournament is full (% / % participants)', v_current_part, v_max_part;
        END IF;
    END IF;

    -- Insert or update entry
    INSERT INTO public.tournament_entries (tournament_id, user_id, status, time_remaining_secs, started_at)
    VALUES (p_tournament_id, p_user_id, 'in_progress', v_time_limit, now())
    ON CONFLICT (tournament_id, user_id) DO UPDATE SET status='in_progress', time_remaining_secs=v_time_limit, started_at=now()
    RETURNING id INTO v_entry_id;

    -- Load tournament questions with options and match pairs
    SELECT COALESCE(json_agg(json_build_object(
        'id', qu.id, 'question_type', qu.question_type, 'title', qu.title, 'prompt', qu.prompt,
        'explanation', '', 'difficulty', qu.difficulty, 'time_limit_secs', qu.time_limit_secs,
        'allow_multiple', qu.allow_multiple, 'prompt_config', qu.prompt_config,
        'metadata', qu.metadata, 'media_url', qu.media_url,
        'options', (SELECT COALESCE(json_agg(json_build_object(
            'id',o.id,'label',o.label,'is_correct',o.is_correct,'sort_order',o.sort_order,
            'correct_position',o.correct_position,'media_url',o.media_url,'visual_label',o.visual_label
        ) ORDER BY o.sort_order),'[]'::json) FROM public.question_options o WHERE o.question_id = qu.id),
        'match_pairs', (SELECT json_agg(json_build_object(
            'id',mp.id,'left_text',mp.left_text,'right_text',mp.right_text,'sort_order',mp.sort_order
        ) ORDER BY mp.sort_order) FROM public.match_pairs mp WHERE mp.question_id = qu.id)
    ) ORDER BY tq.sort_order), '[]'::json) INTO v_questions
    FROM public.tournament_questions tq JOIN public.questions qu ON qu.id = tq.question_id
    WHERE tq.tournament_id = p_tournament_id;

    RETURN json_build_object('entry_id', v_entry_id, 'questions', v_questions, 'time_limit_secs', v_time_limit);
END; $$;

GRANT EXECUTE ON FUNCTION public.start_tournament(UUID, UUID) TO authenticated;
