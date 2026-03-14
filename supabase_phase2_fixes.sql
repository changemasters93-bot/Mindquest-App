-- ================================================================
-- PHASE 2: Performance Optimization (10K+ DAU)
-- Run this migration AFTER supabase_phase1_fixes.sql
-- ================================================================


-- ════════════════════════════════════════════════════════════════
-- 1. FIX #7: get_chapter_quizzes — STOP embedding all questions
--
--    Problem: Returns 20 quizzes × 50 questions × 4 options = 4,000
--    rows per chapter. 500KB-1MB JSON per call.
--
--    Fix: Return quiz metadata only (no questions array). Questions
--    are loaded lazily via get_quiz_with_questions when user taps a quiz.
-- ════════════════════════════════════════════════════════════════

DROP FUNCTION IF EXISTS public.get_chapter_quizzes(UUID, UUID);

CREATE OR REPLACE FUNCTION public.get_chapter_quizzes(p_chapter_id UUID, p_user_id UUID)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_result JSON;
BEGIN
    -- Use a CTE to pre-aggregate attempt stats per quiz in one pass,
    -- instead of N correlated subqueries per quiz.
    WITH attempt_stats AS (
        SELECT
            a.quiz_id,
            MAX(a.score) AS best_score,
            COUNT(*)::int AS attempt_count,
            MAX(a.created_at) AS last_attempt_at
        FROM public.quiz_attempts a
        WHERE a.user_id = p_user_id
          AND a.quiz_id IN (SELECT q.id FROM public.quizzes q WHERE q.chapter_id = p_chapter_id AND q.is_active)
        GROUP BY a.quiz_id
    )
    SELECT COALESCE(json_agg(json_build_object(
        'id', q.id,
        'title', q.title,
        'quiz_type', q.quiz_type,
        'question_count', q.question_count,
        'time_limit_secs', q.time_limit_secs,
        'max_xp', q.max_xp,
        'difficulty', q.difficulty,
        'sort_order', q.sort_order,
        'cooldown_hours', q.cooldown_hours,
        'best_score', s.best_score,
        'attempt_count', COALESCE(s.attempt_count, 0),
        'last_attempt_at', s.last_attempt_at,
        'is_locked', CASE
            WHEN q.cooldown_hours IS NULL THEN false
            WHEN s.last_attempt_at IS NULL THEN false
            WHEN s.last_attempt_at + (q.cooldown_hours || ' hours')::interval > now() THEN true
            ELSE false
        END,
        'unlocks_at', CASE
            WHEN q.cooldown_hours IS NULL THEN NULL
            WHEN s.last_attempt_at IS NULL THEN NULL
            ELSE s.last_attempt_at + (q.cooldown_hours || ' hours')::interval
        END
        -- NOTE: 'questions' field REMOVED. Load via get_quiz_with_questions on demand.
    ) ORDER BY q.sort_order), '[]'::json) INTO v_result
    FROM public.quizzes q
    LEFT JOIN attempt_stats s ON s.quiz_id = q.id
    WHERE q.chapter_id = p_chapter_id AND q.is_active;

    RETURN v_result;
END; $$;

GRANT EXECUTE ON FUNCTION public.get_chapter_quizzes(UUID, UUID) TO anon, authenticated;


-- ════════════════════════════════════════════════════════════════
-- 2. FIX #8: get_user_dashboard — Reduce subqueries, remove UPDATE
--
--    Problems:
--    a) Double-reads user row (lines 208 + 214)
--    b) ~1,200 correlated subqueries for module progress (per chapter)
--    c) UPDATE last_active_at inside a read RPC (write-in-read)
--    d) 5+ aggregate scans of quiz_attempts
--
--    Fixes:
--    a) Single user row fetch
--    b) Use CTEs to pre-aggregate attempt data once
--    c) Move last_active_at update to submit_quiz_attempt (already done in Phase 1)
--    d) Single-pass aggregation for stats
-- ════════════════════════════════════════════════════════════════

DROP FUNCTION IF EXISTS public.get_user_dashboard(UUID);

CREATE OR REPLACE FUNCTION public.get_user_dashboard(p_user_id UUID)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_user_row RECORD;
    v_user JSON;
    v_stats JSON;
    v_mods JSON;
    v_tourn JSON;
    v_iq_quiz_id UUID;
    v_quizzes_completed BIGINT;
    v_accuracy NUMERIC;
    v_tournaments_played BIGINT;
    v_best_tournament_rank INT;
BEGIN
    -- 1. Single user row fetch (was 2 separate queries)
    SELECT * INTO v_user_row FROM public.users u WHERE u.id = p_user_id;
    IF v_user_row IS NULL THEN RAISE EXCEPTION 'User not found: %', p_user_id; END IF;

    v_user := json_build_object(
        'id', v_user_row.id,
        'display_name', v_user_row.display_name,
        'avatar_id', v_user_row.avatar_id,
        'grade_id', v_user_row.grade_id,
        'auth_provider', v_user_row.auth_provider
    );

    -- 2. Find IQ quiz for user's grade
    SELECT iq.id INTO v_iq_quiz_id FROM public.quizzes iq
        JOIN public.chapters iqch ON iqch.id = iq.chapter_id
        JOIN public.modules iqm ON iqm.id = iqch.module_id
        WHERE iq.quiz_type = 'iq' AND iq.is_active
        AND (iqm.grade_id = v_user_row.grade_id OR iqm.grade_id IS NULL)
        ORDER BY CASE WHEN iqm.grade_id IS NOT NULL THEN 0 ELSE 1 END
        LIMIT 1;

    -- 3. Pre-aggregate attempt stats in ONE pass (was 5+ separate queries)
    SELECT
        COUNT(*)::bigint,
        COALESCE(ROUND(AVG(score::numeric / NULLIF(total_questions, 0) * 100), 1), 0)
    INTO v_quizzes_completed, v_accuracy
    FROM public.quiz_attempts WHERE user_id = p_user_id;

    SELECT COUNT(*)::bigint INTO v_tournaments_played
    FROM public.tournament_entries
    WHERE user_id = p_user_id AND status IN ('completed', 'auto_submitted');

    SELECT MIN(rank) INTO v_best_tournament_rank
    FROM public.tournament_entries
    WHERE user_id = p_user_id AND rank IS NOT NULL;

    v_stats := json_build_object(
        'total_xp',           v_user_row.total_xp,
        'level',              v_user_row.level,
        'streak_current',     v_user_row.streak_current,
        'streak_best',        v_user_row.streak_best,
        'quizzes_completed',  v_quizzes_completed,
        'accuracy_pct',       v_accuracy,
        'tournaments_played', v_tournaments_played,
        'best_tournament_rank', v_best_tournament_rank,
        'iq_best_score',      v_user_row.iq_best_score,
        'iq_quiz_id',         v_iq_quiz_id,
        'last_iq_attempt_at', (SELECT MAX(qa.created_at) FROM public.quiz_attempts qa
            WHERE qa.user_id = p_user_id AND qa.quiz_id = v_iq_quiz_id),
        'iq_cooldown_hours',  (SELECT iq.cooldown_hours FROM public.quizzes iq WHERE iq.id = v_iq_quiz_id)
    );

    -- 4. Module progress — use CTEs for single-pass aggregation
    --    Instead of N subqueries per chapter per module, aggregate once.
    WITH
    -- Pre-compute per-chapter quiz counts
    chapter_quiz_counts AS (
        SELECT q.chapter_id, COUNT(*)::int AS total_quizzes
        FROM public.quizzes q
        WHERE q.is_active
        GROUP BY q.chapter_id
    ),
    -- Pre-compute per-chapter completed quiz counts for this user
    chapter_user_progress AS (
        SELECT q.chapter_id, COUNT(DISTINCT a.quiz_id)::int AS quizzes_done
        FROM public.quiz_attempts a
        JOIN public.quizzes q ON q.id = a.quiz_id
        WHERE a.user_id = p_user_id AND q.is_active
        GROUP BY q.chapter_id
    ),
    -- Pre-compute per-module XP and best score
    module_attempt_stats AS (
        SELECT ch.module_id,
            COALESCE(SUM(qa.xp_earned), 0)::bigint AS total_xp_earned,
            MAX(ROUND(qa.score::numeric / NULLIF(qa.total_questions, 0) * 100))::int AS best_score_pct
        FROM public.quiz_attempts qa
        JOIN public.quizzes qz ON qa.quiz_id = qz.id
        JOIN public.chapters ch ON qz.chapter_id = ch.id
        WHERE qa.user_id = p_user_id
        GROUP BY ch.module_id
    ),
    -- Pre-compute module-level chapter aggregates
    module_chapter_agg AS (
        SELECT
            ch.module_id,
            COUNT(*)::int AS total_chapters,
            COUNT(*) FILTER (
                WHERE COALESCE(cup.quizzes_done, 0) >= COALESCE(cqc.total_quizzes, 0)
                  AND COALESCE(cqc.total_quizzes, 0) > 0
            )::int AS completed_chapters,
            -- First active chapter that is NOT yet completed
            (SELECT ch2.id FROM public.chapters ch2
             LEFT JOIN chapter_quiz_counts cqc2 ON cqc2.chapter_id = ch2.id
             LEFT JOIN chapter_user_progress cup2 ON cup2.chapter_id = ch2.id
             WHERE ch2.module_id = ch.module_id AND ch2.is_active
               AND COALESCE(cup2.quizzes_done, 0) < COALESCE(cqc2.total_quizzes, 1)
             ORDER BY ch2.chapter_number LIMIT 1
            ) AS current_chapter_id
        FROM public.chapters ch
        LEFT JOIN chapter_quiz_counts cqc ON cqc.chapter_id = ch.id
        LEFT JOIN chapter_user_progress cup ON cup.chapter_id = ch.id
        WHERE ch.is_active
        GROUP BY ch.module_id
    )
    SELECT COALESCE(json_agg(json_build_object(
        'id', m.id,
        'title', m.title,
        'subtitle', m.subtitle,
        'emoji', m.emoji,
        'accent_color', m.accent_color,
        'sort_order', m.display_order,
        'progress', json_build_object(
            'current_chapter_id', mca.current_chapter_id,
            'current_quiz_id', NULL,
            'best_score_pct', mas.best_score_pct,
            'is_completed', (mca.total_chapters > 0 AND mca.completed_chapters >= mca.total_chapters),
            'completed_chapters', mca.completed_chapters,
            'total_chapters', mca.total_chapters,
            'total_xp_earned', COALESCE(mas.total_xp_earned, 0)
        )
    ) ORDER BY m.display_order), '[]'::json) INTO v_mods
    FROM public.modules m
    LEFT JOIN module_chapter_agg mca ON mca.module_id = m.id
    LEFT JOIN module_attempt_stats mas ON mas.module_id = m.id
    WHERE m.is_active = true AND m.grade_id = v_user_row.grade_id;

    -- 5. Active tournament
    IF v_user_row.grade_id IS NOT NULL THEN
        SELECT json_build_object(
            'id', t.id, 'title', t.title, 'starts_at', t.starts_at, 'ends_at', t.ends_at,
            'status', t.status,
            'user_entry_status', (SELECT te.status FROM public.tournament_entries te WHERE te.tournament_id = t.id AND te.user_id = p_user_id),
            'question_count', t.question_count, 'time_limit_seconds', t.time_limit_seconds,
            'participant_count', (SELECT COUNT(*) FROM public.tournament_entries te2 WHERE te2.tournament_id = t.id)::int
        ) INTO v_tourn
        FROM public.tournaments t
        WHERE t.grade_id = v_user_row.grade_id AND t.status IN ('live','scheduled') AND t.ends_at > now()
        ORDER BY t.starts_at LIMIT 1;
    END IF;

    -- 6. REMOVED: UPDATE users SET last_active_at = now()
    --    (Moved to submit_quiz_attempt in Phase 1 — no writes in read RPCs)

    RETURN json_build_object('user', v_user, 'stats', v_stats, 'modules', v_mods, 'active_tournament', v_tourn);
END;
$$;

GRANT EXECUTE ON FUNCTION public.get_user_dashboard(UUID) TO anon, authenticated;
