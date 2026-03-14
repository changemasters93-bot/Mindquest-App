-- ============================================================
-- FIX 1: get_quiz_with_questions — use direct quiz_id column
--        instead of non-existent quiz_questions join table
-- ============================================================

CREATE OR REPLACE FUNCTION public.get_quiz_with_questions(p_quiz_id UUID, p_user_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  v_result JSONB;
BEGIN
  SELECT jsonb_build_object(
    'id', q.id,
    'title', q.title,
    'quiz_type', COALESCE(q.quiz_type, 'multiple_choice'),
    'question_count', q.question_count,
    'time_limit_secs', q.time_limit_secs,
    'max_xp', q.max_xp,
    'difficulty', q.difficulty,
    'sort_order', q.sort_order,
    'cooldown_hours', q.cooldown_hours,
    'best_score', (
      SELECT MAX(qa.score)
      FROM quiz_attempts qa
      WHERE qa.quiz_id = q.id AND qa.user_id = p_user_id
    ),
    'attempt_count', (
      SELECT COUNT(*)::INT
      FROM quiz_attempts qa
      WHERE qa.quiz_id = q.id AND qa.user_id = p_user_id
    ),
    'last_attempt_at', (
      SELECT MAX(qa.created_at)
      FROM quiz_attempts qa
      WHERE qa.quiz_id = q.id AND qa.user_id = p_user_id
    ),
    'is_locked', CASE
      WHEN q.cooldown_hours IS NOT NULL AND EXISTS(
        SELECT 1 FROM quiz_attempts qa
        WHERE qa.quiz_id = q.id
          AND qa.user_id = p_user_id
          AND qa.created_at > NOW() - (q.cooldown_hours || ' hours')::INTERVAL
      ) THEN TRUE
      ELSE FALSE
    END,
    'unlocks_at', (
      SELECT MAX(qa.created_at) + (q.cooldown_hours || ' hours')::INTERVAL
      FROM quiz_attempts qa
      WHERE qa.quiz_id = q.id
        AND qa.user_id = p_user_id
        AND qa.created_at > NOW() - (q.cooldown_hours || ' hours')::INTERVAL
    ),
    'questions', COALESCE((
      SELECT jsonb_agg(
        jsonb_build_object(
          'id', qq.id,
          'question_type', qq.question_type,
          'title', qq.title,
          'prompt', qq.prompt,
          'explanation', qq.explanation,
          'difficulty', qq.difficulty,
          'time_limit_secs', qq.time_limit_secs,
          'allow_multiple', COALESCE(qq.allow_multiple, false),
          'prompt_config', qq.prompt_config,
          'metadata', qq.metadata,
          'media_url', qq.media_url,
          'options', COALESCE((
            SELECT jsonb_agg(
              jsonb_build_object(
                'id', qo.id,
                'label', qo.label,
                'is_correct', qo.is_correct,
                'sort_order', qo.sort_order,
                'correct_position', qo.correct_position,
                'media_url', qo.media_url,
                'visual_label', qo.visual_label
              ) ORDER BY qo.sort_order
            )
            FROM question_options qo
            WHERE qo.question_id = qq.id
          ), '[]'::JSONB),
          'match_pairs', COALESCE((
            SELECT jsonb_agg(
              jsonb_build_object(
                'id', mp.id,
                'left_text', mp.left_text,
                'right_text', mp.right_text,
                'sort_order', mp.sort_order
              ) ORDER BY mp.sort_order
            )
            FROM match_pairs mp
            WHERE mp.question_id = qq.id
          ), NULL)
        ) ORDER BY qq.sort_order
      )
      -- FIX: use direct quiz_id column instead of quiz_questions join table
      FROM questions qq
      WHERE qq.quiz_id = q.id
    ), '[]'::JSONB)
  ) INTO v_result
  FROM quizzes q
  WHERE q.id = p_quiz_id;

  RETURN v_result;
END;
$$;

GRANT EXECUTE ON FUNCTION public.get_quiz_with_questions(UUID, UUID) TO anon, authenticated;


-- ============================================================
-- FIX 2: get_user_stats — add last_week, last_month, last_6_months
--        period support (previously only 'week' and 'month')
-- ============================================================

CREATE OR REPLACE FUNCTION public.get_user_stats(p_user_id UUID, p_period TEXT DEFAULT 'last_6_months')
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_stats JSON; v_acc DOUBLE PRECISION; v_daily JSON; v_subj JSON; v_since TIMESTAMPTZ; v_grade UUID;
BEGIN
    SELECT grade_id INTO v_grade FROM public.users WHERE id = p_user_id;

    v_since := CASE
        WHEN p_period = 'week'           THEN now() - '7 days'::interval
        WHEN p_period = 'last_week'      THEN now() - '7 days'::interval
        WHEN p_period = 'month'          THEN now() - '30 days'::interval
        WHEN p_period = 'last_month'     THEN now() - '30 days'::interval
        WHEN p_period = 'last_6_months'  THEN now() - '180 days'::interval
        ELSE '1970-01-01'::timestamptz
    END;

    SELECT json_build_object(
        'total_xp', u.total_xp, 'level', u.level, 'streak_current', u.streak_current, 'streak_best', u.streak_best,
        'quizzes_completed', (SELECT COUNT(*) FROM public.quiz_attempts WHERE user_id=p_user_id AND created_at>=v_since),
        'accuracy_pct', COALESCE((SELECT ROUND(AVG(score::numeric/NULLIF(total_questions,0)*100),1)
            FROM public.quiz_attempts WHERE user_id=p_user_id AND created_at>=v_since),0),
        'tournaments_played', (SELECT COUNT(*) FROM public.tournament_entries
            WHERE user_id=p_user_id AND status IN ('completed','auto_submitted') AND created_at>=v_since),
        'best_tournament_rank', (SELECT MIN(rank) FROM public.tournament_entries
            WHERE user_id=p_user_id AND rank IS NOT NULL AND created_at>=v_since),
        'iq_best_score', u.iq_best_score
    ) INTO v_stats FROM public.users u WHERE u.id = p_user_id;

    SELECT COALESCE(ROUND(AVG(score::numeric/NULLIF(total_questions,0)*100),1),0) INTO v_acc
    FROM public.quiz_attempts WHERE user_id=p_user_id AND created_at>=v_since;

    SELECT COALESCE(json_agg(row_to_json(d) ORDER BY d.date),'[]'::json) INTO v_daily
    FROM (SELECT a.created_at::date::text AS date, COUNT(*)::int AS quizzes,
        SUM(a.xp_earned)::int AS xp, SUM(a.time_taken_secs/60)::int AS time_spent_minutes
        FROM public.quiz_attempts a WHERE a.user_id=p_user_id AND a.created_at>=v_since
        GROUP BY a.created_at::date) d;

    -- Subject performance with per-module rank (user's rank among all users in same grade)
    WITH all_user_module_acc AS (
        SELECT rch.module_id, rqa.user_id,
            COALESCE(ROUND(AVG(rqa.score::numeric / NULLIF(rqa.total_questions, 0) * 100))::int, 0) AS acc
        FROM public.quiz_attempts rqa
        JOIN public.quizzes rq ON rq.id = rqa.quiz_id AND rq.is_active
        JOIN public.chapters rch ON rch.id = rq.chapter_id AND rch.is_active
        JOIN public.modules rm ON rm.id = rch.module_id AND rm.is_active
        WHERE rm.grade_id = v_grade
        GROUP BY rch.module_id, rqa.user_id
    ),
    user_module_rank AS (
        SELECT module_id, user_id,
            RANK() OVER (PARTITION BY module_id ORDER BY acc DESC) AS module_rank
        FROM all_user_module_acc
    )
    SELECT COALESCE(json_agg(row_to_json(s) ORDER BY s.rank),'[]'::json) INTO v_subj
    FROM (SELECT m.id AS module_id, m.title, m.emoji,
        COALESCE(MAX(ROUND(a.score::numeric/NULLIF(a.total_questions,0)*100))::int,0) AS best_score_pct,
        COALESCE(ROUND(AVG(a.score::numeric/NULLIF(a.total_questions,0)*100))::int,0) AS accuracy_pct,
        (SELECT COUNT(*) FROM public.chapters ch2
            WHERE ch2.module_id = m.id AND ch2.is_active
            AND (SELECT COUNT(DISTINCT a2.quiz_id)
                FROM public.quiz_attempts a2 JOIN public.quizzes q2 ON q2.id=a2.quiz_id
                WHERE q2.chapter_id=ch2.id AND q2.is_active AND a2.user_id=p_user_id)
            >= (SELECT COUNT(*) FROM public.quizzes q3
                WHERE q3.chapter_id=ch2.id AND q3.is_active)
        )::int AS chapters_completed,
        COUNT(DISTINCT ch.id)::int AS total_chapters,
        COALESCE((SELECT umr.module_rank FROM user_module_rank umr
            WHERE umr.module_id = m.id AND umr.user_id = p_user_id), 0)::int AS rank
        FROM public.modules m JOIN public.chapters ch ON ch.module_id=m.id AND ch.is_active
        LEFT JOIN public.quizzes q ON q.chapter_id=ch.id AND q.is_active
        LEFT JOIN public.quiz_attempts a ON a.quiz_id=q.id AND a.user_id=p_user_id AND a.created_at>=v_since
        WHERE m.is_active AND m.grade_id = v_grade
        GROUP BY m.id, m.title, m.emoji) s;

    RETURN json_build_object('stats',v_stats,'accuracy_pct',v_acc,'daily_activity',v_daily,'subject_performance',v_subj);
END; $$;

GRANT EXECUTE ON FUNCTION public.get_user_stats(UUID, TEXT) TO anon, authenticated;


-- ============================================================
-- FIX 3: get_user_dashboard — add chapter progress counts
--        (completed_chapters, total_chapters, total_xp_earned)
--        + proper is_completed & current_chapter_id computation
-- ============================================================

CREATE OR REPLACE FUNCTION public.get_user_dashboard(p_user_id UUID)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_user JSON; v_stats JSON; v_mods JSON; v_tourn JSON; v_grade UUID; v_iq_quiz_id UUID;
BEGIN
    SELECT json_build_object(
        'id', u.id, 'display_name', u.display_name, 'avatar_id', u.avatar_id,
        'grade_id', u.grade_id, 'auth_provider', u.auth_provider
    ) INTO v_user FROM public.users u WHERE u.id = p_user_id;
    IF v_user IS NULL THEN RAISE EXCEPTION 'User not found: %', p_user_id; END IF;

    SELECT u.grade_id INTO v_grade FROM public.users u WHERE u.id = p_user_id;

    -- Find IQ quiz for user's grade (prefer grade-specific, fallback to grade-null)
    SELECT iq.id INTO v_iq_quiz_id FROM public.quizzes iq
        JOIN public.chapters iqch ON iqch.id = iq.chapter_id
        JOIN public.modules iqm ON iqm.id = iqch.module_id
        WHERE iq.quiz_type = 'iq' AND iq.is_active
        AND (iqm.grade_id = v_grade OR iqm.grade_id IS NULL)
        ORDER BY CASE WHEN iqm.grade_id IS NOT NULL THEN 0 ELSE 1 END
        LIMIT 1;

    SELECT json_build_object(
        'total_xp',           u.total_xp,
        'level',              u.level,
        'streak_current',     u.streak_current,
        'streak_best',        u.streak_best,
        'quizzes_completed',  (SELECT COUNT(*) FROM public.quiz_attempts WHERE user_id = p_user_id),
        'accuracy_pct',       COALESCE((SELECT ROUND(AVG(score::numeric / NULLIF(total_questions,0) * 100),1) FROM public.quiz_attempts WHERE user_id = p_user_id), 0),
        'tournaments_played', (SELECT COUNT(*) FROM public.tournament_entries WHERE user_id = p_user_id AND status IN ('completed','auto_submitted')),
        'best_tournament_rank', (SELECT MIN(rank) FROM public.tournament_entries WHERE user_id = p_user_id AND rank IS NOT NULL),
        'iq_best_score', u.iq_best_score,
        'iq_quiz_id', v_iq_quiz_id,
        'last_iq_attempt_at', (SELECT MAX(qa.created_at) FROM public.quiz_attempts qa
            WHERE qa.user_id = p_user_id AND qa.quiz_id = v_iq_quiz_id),
        'iq_cooldown_hours', (SELECT iq.cooldown_hours FROM public.quizzes iq WHERE iq.id = v_iq_quiz_id)
    ) INTO v_stats FROM public.users u WHERE u.id = p_user_id;

    SELECT COALESCE(json_agg(row_to_json(m_row) ORDER BY m_row.sort_order), '[]'::json) INTO v_mods
    FROM (
        SELECT m.id, m.title, m.subtitle, m.emoji, m.accent_color, m.display_order AS sort_order,
            (SELECT json_build_object(
                'current_chapter_id', (
                    -- First active chapter that is NOT yet completed
                    SELECT ch.id FROM public.chapters ch
                    WHERE ch.module_id = m.id AND ch.is_active
                    AND (
                        SELECT COUNT(DISTINCT a.quiz_id)
                        FROM public.quiz_attempts a
                        JOIN public.quizzes q2 ON q2.id = a.quiz_id
                        WHERE q2.chapter_id = ch.id AND q2.is_active AND a.user_id = p_user_id
                    ) < (
                        SELECT COUNT(*) FROM public.quizzes q3
                        WHERE q3.chapter_id = ch.id AND q3.is_active
                    )
                    ORDER BY ch.chapter_number
                    LIMIT 1
                ),
                'current_quiz_id', NULL,
                'best_score_pct', (SELECT MAX(ROUND(a.score::numeric / NULLIF(a.total_questions,0) * 100))::int
                    FROM public.quiz_attempts a JOIN public.quizzes q2 ON q2.id = a.quiz_id
                    JOIN public.chapters ch2 ON ch2.id = q2.chapter_id
                    WHERE ch2.module_id = m.id AND a.user_id = p_user_id),
                'is_completed', (
                    -- True when: has chapters with quizzes AND no chapter has incomplete quizzes
                    (SELECT COUNT(*) FROM public.chapters ch
                     WHERE ch.module_id = m.id AND ch.is_active
                     AND (SELECT COUNT(*) FROM public.quizzes q WHERE q.chapter_id = ch.id AND q.is_active) > 0
                    ) > 0
                    AND NOT EXISTS (
                        SELECT 1 FROM public.chapters ch
                        WHERE ch.module_id = m.id AND ch.is_active
                        AND (SELECT COUNT(*) FROM public.quizzes q WHERE q.chapter_id = ch.id AND q.is_active) > 0
                        AND (
                            SELECT COUNT(DISTINCT a.quiz_id)
                            FROM public.quiz_attempts a
                            JOIN public.quizzes q2 ON q2.id = a.quiz_id
                            WHERE q2.chapter_id = ch.id AND q2.is_active AND a.user_id = p_user_id
                        ) < (
                            SELECT COUNT(*) FROM public.quizzes q3
                            WHERE q3.chapter_id = ch.id AND q3.is_active
                        )
                    )
                ),
                'completed_chapters', (
                    SELECT COUNT(*)::INT FROM public.chapters ch
                    WHERE ch.module_id = m.id AND ch.is_active
                    AND (SELECT COUNT(*) FROM public.quizzes q4 WHERE q4.chapter_id = ch.id AND q4.is_active) > 0
                    AND (
                        SELECT COUNT(DISTINCT a.quiz_id)
                        FROM public.quiz_attempts a
                        JOIN public.quizzes q2 ON q2.id = a.quiz_id
                        WHERE q2.chapter_id = ch.id AND q2.is_active AND a.user_id = p_user_id
                    ) >= (
                        SELECT COUNT(*) FROM public.quizzes q3
                        WHERE q3.chapter_id = ch.id AND q3.is_active
                    )
                ),
                'total_chapters', (
                    SELECT COUNT(*)::INT FROM public.chapters ch
                    WHERE ch.module_id = m.id AND ch.is_active
                ),
                'total_xp_earned', COALESCE((
                    SELECT SUM(qa.xp_earned)
                    FROM public.quiz_attempts qa
                    JOIN public.quizzes qz ON qa.quiz_id = qz.id
                    JOIN public.chapters ch ON qz.chapter_id = ch.id
                    WHERE ch.module_id = m.id AND qa.user_id = p_user_id
                ), 0)
            )) AS progress
        FROM public.modules m
        WHERE m.is_active = true AND m.grade_id = v_grade
    ) m_row;

    -- Only look for tournaments if user has a grade set
    IF v_grade IS NOT NULL THEN
        SELECT json_build_object(
            'id', t.id, 'title', t.title, 'starts_at', t.starts_at, 'ends_at', t.ends_at,
            'status', t.status,
            'user_entry_status', (SELECT te.status FROM public.tournament_entries te WHERE te.tournament_id = t.id AND te.user_id = p_user_id),
            'question_count', t.question_count, 'time_limit_seconds', t.time_limit_seconds,
            'participant_count', (SELECT COUNT(*) FROM public.tournament_entries te2 WHERE te2.tournament_id = t.id)::int
        ) INTO v_tourn
        FROM public.tournaments t
        WHERE t.grade_id = v_grade AND t.status IN ('live','scheduled') AND t.ends_at > now()
        ORDER BY t.starts_at LIMIT 1;
    END IF;

    UPDATE public.users SET last_active_at = now() WHERE id = p_user_id;

    RETURN json_build_object('user', v_user, 'stats', v_stats, 'modules', v_mods, 'active_tournament', v_tourn);
END;
$$;

GRANT EXECUTE ON FUNCTION public.get_user_dashboard(UUID) TO anon, authenticated;


-- ============================================================
-- FIX 4: get_module_full — add 'completed' chapter state
--        (was only returning 'unlocked' or 'locked')
-- ============================================================

CREATE OR REPLACE FUNCTION public.get_module_full(p_module_id UUID, p_user_id UUID)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_module JSON; v_chapters JSON;
BEGIN
    SELECT json_build_object(
        'id', m.id, 'title', m.title, 'subtitle', m.subtitle, 'emoji', m.emoji,
        'accent_color', m.accent_color, 'display_order', m.display_order
    ) INTO v_module FROM public.modules m WHERE m.id = p_module_id;
    IF v_module IS NULL THEN RAISE EXCEPTION 'Module not found: %', p_module_id; END IF;

    SELECT COALESCE(json_agg(row_to_json(ch_row) ORDER BY ch_row.chapter_number), '[]'::json)
    INTO v_chapters
    FROM (
        SELECT ch.id, ch.title, ch.chapter_number,
            (SELECT COUNT(*) FROM public.quizzes q WHERE q.chapter_id = ch.id AND q.is_active)::int AS quiz_count,
            json_build_object(
                'quizzes_done',  (SELECT COUNT(DISTINCT a.quiz_id)
                    FROM public.quiz_attempts a JOIN public.quizzes q ON q.id = a.quiz_id
                    WHERE q.chapter_id = ch.id AND q.is_active AND a.user_id = p_user_id)::int,
                'total_quizzes', (SELECT COUNT(*) FROM public.quizzes q WHERE q.chapter_id = ch.id AND q.is_active)::int,
                'best_score_pct', (SELECT MAX(ROUND(a.score::numeric / NULLIF(a.total_questions,0) * 100))::int
                    FROM public.quiz_attempts a JOIN public.quizzes q ON q.id = a.quiz_id
                    WHERE q.chapter_id = ch.id AND a.user_id = p_user_id),
                'is_completed', (SELECT COUNT(DISTINCT a.quiz_id) >= COUNT(DISTINCT q2.id)
                    FROM public.quizzes q2
                    LEFT JOIN public.quiz_attempts a ON a.quiz_id = q2.id AND a.user_id = p_user_id
                    WHERE q2.chapter_id = ch.id AND q2.is_active)
            ) AS progress,
            CASE
                -- 1) Completed: all active quizzes attempted AND has at least one quiz
                WHEN (SELECT COUNT(DISTINCT a.quiz_id) >= COUNT(DISTINCT q_chk.id)
                      FROM public.quizzes q_chk
                      LEFT JOIN public.quiz_attempts a ON a.quiz_id = q_chk.id AND a.user_id = p_user_id
                      WHERE q_chk.chapter_id = ch.id AND q_chk.is_active)
                     AND (SELECT COUNT(*) FROM public.quizzes q_cnt WHERE q_cnt.chapter_id = ch.id AND q_cnt.is_active) > 0
                THEN 'completed'
                -- 2) First chapter is always unlocked (if not completed)
                WHEN ch.chapter_number = 1 THEN 'unlocked'
                -- 3) Unlocked if previous chapter is completed
                WHEN EXISTS(
                    SELECT 1 FROM public.chapters prev
                    WHERE prev.module_id = ch.module_id AND prev.chapter_number = ch.chapter_number - 1
                    AND (SELECT COUNT(DISTINCT a.quiz_id) >= COUNT(DISTINCT q3.id)
                         FROM public.quizzes q3
                         LEFT JOIN public.quiz_attempts a ON a.quiz_id = q3.id AND a.user_id = p_user_id
                         WHERE q3.chapter_id = prev.id AND q3.is_active)
                    AND (SELECT COUNT(*) FROM public.quizzes q4 WHERE q4.chapter_id = prev.id AND q4.is_active) > 0
                ) THEN 'unlocked'
                -- 4) Otherwise locked
                ELSE 'locked'
            END AS state
        FROM public.chapters ch WHERE ch.module_id = p_module_id AND ch.is_active
    ) ch_row;

    RETURN json_build_object('module', v_module, 'chapters', v_chapters);
END;
$$;

GRANT EXECUTE ON FUNCTION public.get_module_full(UUID, UUID) TO anon, authenticated;


-- ============================================================
-- FIX 5: get_chapter_quizzes — NO CHANGES NEEDED
--        Already returns best_score, attempt_count, is_locked
-- ============================================================
-- Verified: current function already includes all required fields.


-- ============================================================
-- FIX 6: submit_quiz_attempt — add streak update logic
--        streak_current and streak_best were never updated
--
-- ⚠️  SUPERSEDED by supabase_phase1_fixes.sql which:
--     • Renames parameter to p_payload
--     • Switches JSON keys to camelCase (userId, quizId, etc.)
--     • Adds row-level locking (FOR UPDATE) to prevent XP race conditions
--     • Adds cooldown enforcement + idempotency check
--
-- DO NOT RUN this version if Phase 1 has been applied — it will break the app.
-- The app sends parameter key "p_payload" with camelCase JSON keys.
-- ============================================================

-- CREATE OR REPLACE FUNCTION public.submit_quiz_attempt(payload JSON)
-- COMMENTED OUT — Phase 1 version in supabase_phase1_fixes.sql is authoritative.
-- If you need to recreate this function, use supabase_phase1_fixes.sql instead.

/*  ORIGINAL VERSION (superseded):
CREATE OR REPLACE FUNCTION public.submit_quiz_attempt(payload JSON)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_user_id UUID := (payload->>'user_id')::uuid;
    v_quiz_id UUID := (payload->>'quiz_id')::uuid;
    v_time    INT  := (payload->>'time_taken_secs')::int;
    v_idemp   TEXT := payload->>'idempotency_key';
    v_answers JSON := payload->'answers';
    v_score INT := 0; v_total INT := 0; v_xp INT; v_max_xp INT;
    v_is_replay BOOLEAN := false; v_attempt_id UUID;
    v_old_level INT; v_new_level INT; v_total_xp BIGINT; v_rank INT;
    v_next_quiz UUID; v_chapter_id UUID;
    v_cooldown INT; v_last_attempt TIMESTAMPTZ;
    v_last_active_date DATE;
    v_iq_score INT;
BEGIN
    -- 1. Idempotency check
    SELECT id INTO v_attempt_id FROM public.quiz_attempts
    WHERE user_id = v_user_id AND idempotency_key = v_idemp;
    IF v_attempt_id IS NOT NULL THEN
        SELECT a.score, a.total_questions, a.xp_earned INTO v_score, v_total, v_xp
        FROM public.quiz_attempts a WHERE a.id = v_attempt_id;
        SELECT total_xp, level INTO v_total_xp, v_new_level FROM public.users WHERE id = v_user_id;
        RETURN json_build_object(
            'status','duplicate','attempt_id',v_attempt_id,'score',v_score,
            'total_questions',v_total,'xp_earned',v_xp,'total_xp',v_total_xp,
            'level',v_new_level,'level_changed',false,'rank_global',0,
            'is_replay',true,'next_quiz_id',NULL,'iq_score',NULL);
    END IF;

    -- 2. Cooldown enforcement (e.g. IQ quiz = 168 hours = 7 days)
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

    SELECT COUNT(*) INTO v_total FROM json_array_elements(v_answers);
    SELECT COUNT(*) INTO v_score FROM json_array_elements(v_answers) e WHERE (e->>'is_correct')::boolean;
    SELECT EXISTS(SELECT 1 FROM public.quiz_attempts WHERE user_id = v_user_id AND quiz_id = v_quiz_id) INTO v_is_replay;
    SELECT max_xp, chapter_id INTO v_max_xp, v_chapter_id FROM public.quizzes WHERE id = v_quiz_id;
    v_max_xp := COALESCE(v_max_xp, 100);
    v_xp := CASE WHEN v_is_replay THEN 0 ELSE ROUND(v_max_xp * (v_score::numeric / NULLIF(v_total, 0))) END;

    INSERT INTO public.quiz_attempts (user_id, quiz_id, score, total_questions, xp_earned, time_taken_secs, answers, idempotency_key)
    VALUES (v_user_id, v_quiz_id, v_score, v_total, v_xp, v_time, v_answers::jsonb, v_idemp) RETURNING id INTO v_attempt_id;

    -- Get current level and last_active_at date for streak calculation
    SELECT level, last_active_at::date INTO v_old_level, v_last_active_date FROM public.users WHERE id = v_user_id;
    UPDATE public.users SET total_xp = total_xp + v_xp,
        level = CASE WHEN total_xp+v_xp>=5000 THEN 10 WHEN total_xp+v_xp>=4000 THEN 9 WHEN total_xp+v_xp>=3200 THEN 8
            WHEN total_xp+v_xp>=2500 THEN 7 WHEN total_xp+v_xp>=1900 THEN 6 WHEN total_xp+v_xp>=1400 THEN 5
            WHEN total_xp+v_xp>=1000 THEN 4 WHEN total_xp+v_xp>=600 THEN 3 WHEN total_xp+v_xp>=300 THEN 2 ELSE 1 END,
        last_active_at = now(),
        -- Streak logic: consecutive days of quiz activity
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
        END)
    WHERE id = v_user_id RETURNING total_xp, level INTO v_total_xp, v_new_level;

    -- Update IQ best score if this is an IQ quiz (convert to IQ range 55-160)
    IF EXISTS(SELECT 1 FROM public.quizzes WHERE id = v_quiz_id AND quiz_type = 'iq') THEN
        v_iq_score := ROUND(55 + (v_score * 105.0 / NULLIF(v_total, 0)));
        UPDATE public.users
        SET iq_best_score = GREATEST(COALESCE(iq_best_score, 0), v_iq_score)
        WHERE id = v_user_id;
    END IF;

    SELECT COUNT(*)+1 INTO v_rank FROM public.users WHERE total_xp > v_total_xp;
    SELECT q.id INTO v_next_quiz FROM public.quizzes q
    WHERE q.chapter_id = v_chapter_id AND q.sort_order > (SELECT sort_order FROM public.quizzes WHERE id = v_quiz_id) AND q.is_active
    ORDER BY q.sort_order LIMIT 1;

    RETURN json_build_object(
        'status','success','attempt_id',v_attempt_id,'score',v_score,
        'total_questions',v_total,'xp_earned',v_xp,'total_xp',v_total_xp,
        'level',v_new_level,'level_changed',(v_new_level > v_old_level),
        'rank_global',v_rank,'is_replay',v_is_replay,'next_quiz_id',v_next_quiz,
        'iq_score',v_iq_score);
END; $$;

GRANT EXECUTE ON FUNCTION public.submit_quiz_attempt(JSON) TO anon, authenticated;
END OF ORIGINAL VERSION */

-- ONE-TIME MIGRATION: Convert existing raw IQ scores (0-60) to IQ range (55-160)
-- Only runs on scores that look like raw counts (≤60)
UPDATE public.users
SET iq_best_score = ROUND(55 + (iq_best_score * 105.0 / 60))
WHERE iq_best_score IS NOT NULL AND iq_best_score <= 60;
