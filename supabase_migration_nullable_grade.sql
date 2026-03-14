-- ═══════════════════════════════════════════════════════════════════
-- Migration: Make grade_id nullable in users table
-- Purpose:   Allow anonymous & new users to be created without
--            selecting a grade (grade can be set later).
-- Run this in: Supabase Dashboard → SQL Editor → New query → Run
-- ═══════════════════════════════════════════════════════════════════

-- 1. Drop NOT NULL constraint on grade_id
ALTER TABLE public.users ALTER COLUMN grade_id DROP NOT NULL;

-- 2. Update get_user_dashboard to handle NULL grade_id
--    (tournament query needs COALESCE to avoid NULL = NULL miss)
CREATE OR REPLACE FUNCTION public.get_user_dashboard(p_user_id UUID)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_user JSON; v_stats JSON; v_mods JSON; v_tourn JSON; v_grade UUID;
BEGIN
    SELECT json_build_object(
        'id', u.id, 'display_name', u.display_name, 'avatar_id', u.avatar_id,
        'grade_id', u.grade_id, 'auth_provider', u.auth_provider
    ) INTO v_user FROM public.users u WHERE u.id = p_user_id;
    IF v_user IS NULL THEN RAISE EXCEPTION 'User not found: %', p_user_id; END IF;

    SELECT u.grade_id INTO v_grade FROM public.users u WHERE u.id = p_user_id;

    SELECT json_build_object(
        'total_xp',           u.total_xp,
        'level',              u.level,
        'streak_current',     u.streak_current,
        'streak_best',        u.streak_best,
        'quizzes_completed',  (SELECT COUNT(*) FROM public.quiz_attempts WHERE user_id = p_user_id),
        'accuracy_pct',       COALESCE((SELECT ROUND(AVG(score::numeric / NULLIF(total_questions,0) * 100),1) FROM public.quiz_attempts WHERE user_id = p_user_id), 0),
        'tournaments_played', (SELECT COUNT(*) FROM public.tournament_entries WHERE user_id = p_user_id AND status IN ('completed','auto_submitted')),
        'best_tournament_rank', (SELECT MIN(rank) FROM public.tournament_entries WHERE user_id = p_user_id AND rank IS NOT NULL)
    ) INTO v_stats FROM public.users u WHERE u.id = p_user_id;

    SELECT COALESCE(json_agg(row_to_json(m_row) ORDER BY m_row.sort_order), '[]'::json) INTO v_mods
    FROM (
        SELECT m.id, m.title, m.emoji, m.accent_color, m.display_order AS sort_order,
            (SELECT json_build_object(
                'current_chapter_id', NULL, 'current_quiz_id', NULL,
                'best_score_pct', (SELECT MAX(ROUND(a.score::numeric / NULLIF(a.total_questions,0) * 100))::int
                    FROM public.quiz_attempts a JOIN public.quizzes q2 ON q2.id = a.quiz_id
                    JOIN public.chapters ch2 ON ch2.id = q2.chapter_id
                    WHERE ch2.module_id = m.id AND a.user_id = p_user_id),
                'is_completed', false
            )) AS progress
        FROM public.modules m
        WHERE m.is_active = true AND (m.grade_id IS NULL OR m.grade_id = v_grade)
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
END; $$;
