-- ╔══════════════════════════════════════════════════════════════════════╗
-- ║  MINDQUEST V2 — CMS EXTRAS + IQ SEED DATA                        ║
-- ║  Run AFTER supabase_final.sql                                     ║
-- ║  Adds: 15 missing CMS RPCs + 60 IQ questions (all 12 types)      ║
-- ╚══════════════════════════════════════════════════════════════════════╝


-- ═══════════════════════════════════════════════════════════════════════
-- PART A: MISSING CMS RPCs
-- ═══════════════════════════════════════════════════════════════════════

-- ─── A1. admin_list_modules ──────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_list_modules(
    p_status TEXT DEFAULT NULL, p_grade_id UUID DEFAULT NULL,
    p_search TEXT DEFAULT NULL, p_limit INT DEFAULT 50, p_offset INT DEFAULT 0
) RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_result JSON; v_total INT;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    SELECT COUNT(*) INTO v_total FROM public.modules m
    WHERE (p_status IS NULL OR m.status = p_status)
      AND (p_grade_id IS NULL OR m.grade_id = p_grade_id)
      AND (p_search IS NULL OR m.title ILIKE '%' || p_search || '%');

    SELECT json_build_object('total', v_total, 'items',
        COALESCE(json_agg(row_to_json(r) ORDER BY r.display_order), '[]'::json))
    INTO v_result FROM (
        SELECT m.id, m.title, m.subtitle, m.emoji, m.accent_color, m.display_order,
            m.status, m.is_active, g.label AS grade_label, m.created_at, m.updated_at,
            (SELECT COUNT(*) FROM public.chapters WHERE module_id = m.id)::int AS chapter_count,
            (SELECT COUNT(*) FROM public.chapters c JOIN public.quizzes q ON q.chapter_id = c.id WHERE c.module_id = m.id)::int AS quiz_count
        FROM public.modules m LEFT JOIN public.grades g ON g.id = m.grade_id
        WHERE (p_status IS NULL OR m.status = p_status)
          AND (p_grade_id IS NULL OR m.grade_id = p_grade_id)
          AND (p_search IS NULL OR m.title ILIKE '%' || p_search || '%')
        ORDER BY m.display_order LIMIT p_limit OFFSET p_offset
    ) r;
    RETURN v_result;
END; $$;

-- ─── A2. admin_list_chapters ─────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_list_chapters(
    p_module_id UUID DEFAULT NULL, p_status TEXT DEFAULT NULL,
    p_limit INT DEFAULT 50, p_offset INT DEFAULT 0
) RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_result JSON; v_total INT;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    SELECT COUNT(*) INTO v_total FROM public.chapters c
    WHERE (p_module_id IS NULL OR c.module_id = p_module_id)
      AND (p_status IS NULL OR c.status = p_status);

    SELECT json_build_object('total', v_total, 'items',
        COALESCE(json_agg(row_to_json(r) ORDER BY r.chapter_number), '[]'::json))
    INTO v_result FROM (
        SELECT c.id, c.title, c.chapter_number, c.sort_order, c.status, c.is_active,
            c.module_id, m.title AS module_title, c.created_at,
            (SELECT COUNT(*) FROM public.quizzes WHERE chapter_id = c.id)::int AS quiz_count,
            (SELECT COUNT(*) FROM public.quizzes q JOIN public.questions qu ON qu.quiz_id = q.id WHERE q.chapter_id = c.id)::int AS question_count
        FROM public.chapters c LEFT JOIN public.modules m ON m.id = c.module_id
        WHERE (p_module_id IS NULL OR c.module_id = p_module_id)
          AND (p_status IS NULL OR c.status = p_status)
        ORDER BY c.chapter_number LIMIT p_limit OFFSET p_offset
    ) r;
    RETURN v_result;
END; $$;

-- ─── A3. admin_list_quizzes ──────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_list_quizzes(
    p_chapter_id UUID DEFAULT NULL, p_status TEXT DEFAULT NULL,
    p_search TEXT DEFAULT NULL, p_limit INT DEFAULT 50, p_offset INT DEFAULT 0
) RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_result JSON; v_total INT;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    SELECT COUNT(*) INTO v_total FROM public.quizzes q
    WHERE (p_chapter_id IS NULL OR q.chapter_id = p_chapter_id)
      AND (p_status IS NULL OR q.status = p_status)
      AND (p_search IS NULL OR q.title ILIKE '%' || p_search || '%');

    SELECT json_build_object('total', v_total, 'items',
        COALESCE(json_agg(row_to_json(r) ORDER BY r.sort_order), '[]'::json))
    INTO v_result FROM (
        SELECT q.id, q.title, q.quiz_type, q.question_count, q.time_limit_secs, q.max_xp,
            q.difficulty, q.sort_order, q.status, q.is_active, q.chapter_id,
            ch.title AS chapter_title, m.title AS module_title, q.created_at,
            (SELECT COUNT(*) FROM public.quiz_attempts WHERE quiz_id = q.id)::int AS attempt_count
        FROM public.quizzes q
        LEFT JOIN public.chapters ch ON ch.id = q.chapter_id
        LEFT JOIN public.modules m ON m.id = ch.module_id
        WHERE (p_chapter_id IS NULL OR q.chapter_id = p_chapter_id)
          AND (p_status IS NULL OR q.status = p_status)
          AND (p_search IS NULL OR q.title ILIKE '%' || p_search || '%')
        ORDER BY q.sort_order LIMIT p_limit OFFSET p_offset
    ) r;
    RETURN v_result;
END; $$;

-- ─── A4. admin_list_questions ────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_list_questions(
    p_quiz_id UUID DEFAULT NULL, p_type TEXT DEFAULT NULL,
    p_status TEXT DEFAULT NULL, p_limit INT DEFAULT 100, p_offset INT DEFAULT 0
) RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_result JSON; v_total INT;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    SELECT COUNT(*) INTO v_total FROM public.questions qu
    WHERE (p_quiz_id IS NULL OR qu.quiz_id = p_quiz_id)
      AND (p_type IS NULL OR qu.question_type = p_type)
      AND (p_status IS NULL OR qu.status = p_status);

    SELECT json_build_object('total', v_total, 'items',
        COALESCE(json_agg(row_to_json(r) ORDER BY r.sort_order), '[]'::json))
    INTO v_result FROM (
        SELECT qu.id, qu.title, qu.question_type, qu.difficulty, qu.sort_order, qu.status,
            qu.quiz_id, qz.title AS quiz_title,
            (SELECT COUNT(*) FROM public.question_options WHERE question_id = qu.id)::int AS option_count,
            (SELECT COUNT(*) FROM public.match_pairs WHERE question_id = qu.id)::int AS pair_count
        FROM public.questions qu LEFT JOIN public.quizzes qz ON qz.id = qu.quiz_id
        WHERE (p_quiz_id IS NULL OR qu.quiz_id = p_quiz_id)
          AND (p_type IS NULL OR qu.question_type = p_type)
          AND (p_status IS NULL OR qu.status = p_status)
        ORDER BY qu.sort_order LIMIT p_limit OFFSET p_offset
    ) r;
    RETURN v_result;
END; $$;

-- ─── A5. admin_get_question_detail ───────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_get_question_detail(p_question_id UUID)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_result JSON;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    SELECT json_build_object(
        'id', qu.id, 'quiz_id', qu.quiz_id, 'question_type', qu.question_type,
        'title', qu.title, 'prompt', qu.prompt, 'explanation', qu.explanation,
        'difficulty', qu.difficulty, 'time_limit_secs', qu.time_limit_secs,
        'allow_multiple', qu.allow_multiple, 'prompt_config', qu.prompt_config,
        'metadata', qu.metadata, 'media_url', qu.media_url, 'sort_order', qu.sort_order,
        'status', qu.status, 'created_by', qu.created_by, 'created_at', qu.created_at,
        'options', (SELECT COALESCE(json_agg(json_build_object(
            'id', o.id, 'label', o.label, 'is_correct', o.is_correct, 'sort_order', o.sort_order,
            'correct_position', o.correct_position, 'media_url', o.media_url, 'visual_label', o.visual_label
        ) ORDER BY o.sort_order), '[]'::json) FROM public.question_options o WHERE o.question_id = qu.id),
        'match_pairs', (SELECT COALESCE(json_agg(json_build_object(
            'id', mp.id, 'left_text', mp.left_text, 'right_text', mp.right_text, 'sort_order', mp.sort_order
        ) ORDER BY mp.sort_order), '[]'::json) FROM public.match_pairs mp WHERE mp.question_id = qu.id)
    ) INTO v_result FROM public.questions qu WHERE qu.id = p_question_id;
    RETURN v_result;
END; $$;

-- ─── A6. admin_upsert_grade ──────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_upsert_grade(p_code TEXT, p_label TEXT, p_sort_order INT)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_id UUID;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    INSERT INTO public.grades (code, label, sort_order, created_by, updated_at)
    VALUES (p_code, p_label, p_sort_order, auth.uid(), now())
    ON CONFLICT (code) DO UPDATE SET label = p_label, sort_order = p_sort_order, updated_at = now()
    RETURNING id INTO v_id;
    INSERT INTO public.audit_logs (admin_id, action, table_name, record_id, new_values)
    VALUES (auth.uid(), 'update', 'grades', v_id, jsonb_build_object('code', p_code, 'label', p_label));
    RETURN json_build_object('id', v_id, 'status', 'success');
END; $$;

-- ─── A7. admin_upsert_country ────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_upsert_country(p_name TEXT, p_code TEXT, p_is_active BOOLEAN DEFAULT true)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_id UUID;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    INSERT INTO public.countries (name, code, is_active, updated_at)
    VALUES (p_name, p_code, p_is_active, now())
    ON CONFLICT (code) DO UPDATE SET name = p_name, is_active = p_is_active, updated_at = now()
    RETURNING id INTO v_id;
    RETURN json_build_object('id', v_id, 'status', 'success');
END; $$;

-- ─── A8. admin_upsert_city ──────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_upsert_city(p_country_id UUID, p_name TEXT)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_id UUID;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    INSERT INTO public.cities (country_id, name, created_by, updated_at)
    VALUES (p_country_id, p_name, auth.uid(), now())
    ON CONFLICT (country_id, name) DO UPDATE SET updated_at = now()
    RETURNING id INTO v_id;
    RETURN json_build_object('id', v_id, 'status', 'success');
END; $$;

-- ─── A9. admin_register_media ────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_register_media(
    p_file_name TEXT, p_file_size INT, p_mime_type TEXT,
    p_storage_path TEXT, p_cdn_url TEXT, p_alt_text TEXT DEFAULT NULL,
    p_width INT DEFAULT NULL, p_height INT DEFAULT NULL
) RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_id UUID;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    INSERT INTO public.media_uploads (uploaded_by, file_name, file_size, mime_type,
        storage_path, cdn_url, alt_text, width, height)
    VALUES (auth.uid(), p_file_name, p_file_size, p_mime_type,
        p_storage_path, p_cdn_url, p_alt_text, p_width, p_height)
    RETURNING id INTO v_id;
    RETURN json_build_object('id', v_id, 'cdn_url', p_cdn_url);
END; $$;

-- ─── A10. admin_list_media ───────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_list_media(
    p_mime_filter TEXT DEFAULT NULL, p_limit INT DEFAULT 50, p_offset INT DEFAULT 0
) RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_result JSON;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    SELECT json_build_object('total', (SELECT COUNT(*) FROM public.media_uploads
        WHERE (p_mime_filter IS NULL OR mime_type ILIKE p_mime_filter || '%')),
        'items', COALESCE(json_agg(row_to_json(r) ORDER BY r.created_at DESC), '[]'::json))
    INTO v_result FROM (
        SELECT id, file_name, file_size, mime_type, cdn_url, alt_text, width, height, created_at
        FROM public.media_uploads
        WHERE (p_mime_filter IS NULL OR mime_type ILIKE p_mime_filter || '%')
        ORDER BY created_at DESC LIMIT p_limit OFFSET p_offset
    ) r;
    RETURN v_result;
END; $$;

-- ─── A11. admin_get_audit_logs ───────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_get_audit_logs(
    p_table_name TEXT DEFAULT NULL, p_action TEXT DEFAULT NULL,
    p_admin_id UUID DEFAULT NULL, p_limit INT DEFAULT 50, p_offset INT DEFAULT 0
) RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_result JSON;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    SELECT json_build_object('total', (SELECT COUNT(*) FROM public.audit_logs
        WHERE (p_table_name IS NULL OR table_name = p_table_name)
          AND (p_action IS NULL OR action = p_action)
          AND (p_admin_id IS NULL OR admin_id = p_admin_id)),
        'items', COALESCE(json_agg(row_to_json(r) ORDER BY r.created_at DESC), '[]'::json))
    INTO v_result FROM (
        SELECT al.id, al.admin_id, au.display_name AS admin_name, al.action, al.table_name,
            al.record_id, al.old_values, al.new_values, al.created_at
        FROM public.audit_logs al LEFT JOIN public.admin_users au ON au.id = al.admin_id
        WHERE (p_table_name IS NULL OR al.table_name = p_table_name)
          AND (p_action IS NULL OR al.action = p_action)
          AND (p_admin_id IS NULL OR al.admin_id = p_admin_id)
        ORDER BY al.created_at DESC LIMIT p_limit OFFSET p_offset
    ) r;
    RETURN v_result;
END; $$;

-- ─── A12. admin_get_analytics ────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_get_analytics(p_days INT DEFAULT 30)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_since TIMESTAMPTZ; v_growth JSON; v_quiz_stats JSON; v_top_modules JSON;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    v_since := now() - (p_days || ' days')::interval;

    -- Daily user growth
    SELECT COALESCE(json_agg(row_to_json(r) ORDER BY r.date), '[]'::json) INTO v_growth
    FROM (SELECT created_at::date::text AS date, COUNT(*)::int AS new_users
        FROM public.users WHERE created_at >= v_since GROUP BY created_at::date) r;

    -- Quiz stats
    SELECT json_build_object(
        'total_attempts', (SELECT COUNT(*) FROM public.quiz_attempts WHERE created_at >= v_since),
        'unique_users', (SELECT COUNT(DISTINCT user_id) FROM public.quiz_attempts WHERE created_at >= v_since),
        'avg_score_pct', COALESCE((SELECT ROUND(AVG(score::numeric/NULLIF(total_questions,0)*100),1)
            FROM public.quiz_attempts WHERE created_at >= v_since), 0),
        'daily_attempts', (SELECT COALESCE(json_agg(row_to_json(d) ORDER BY d.date), '[]'::json)
            FROM (SELECT created_at::date::text AS date, COUNT(*)::int AS attempts,
                COUNT(DISTINCT user_id)::int AS users
                FROM public.quiz_attempts WHERE created_at >= v_since GROUP BY created_at::date) d)
    ) INTO v_quiz_stats;

    -- Top modules by attempts
    SELECT COALESCE(json_agg(row_to_json(r)), '[]'::json) INTO v_top_modules
    FROM (SELECT m.id, m.title, m.emoji, COUNT(a.id)::int AS attempts,
            COUNT(DISTINCT a.user_id)::int AS unique_users
        FROM public.modules m JOIN public.chapters ch ON ch.module_id = m.id
        JOIN public.quizzes q ON q.chapter_id = ch.id
        JOIN public.quiz_attempts a ON a.quiz_id = q.id AND a.created_at >= v_since
        GROUP BY m.id, m.title, m.emoji ORDER BY attempts DESC LIMIT 10) r;

    RETURN json_build_object(
        'period_days', p_days, 'user_growth', v_growth,
        'quiz_stats', v_quiz_stats, 'top_modules', v_top_modules,
        'active_users_today', (SELECT COUNT(DISTINCT user_id) FROM public.quiz_attempts
            WHERE created_at::date = CURRENT_DATE)
    );
END; $$;

-- ─── A13. admin_list_tournaments ─────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_list_tournaments(
    p_status TEXT DEFAULT NULL, p_grade_id UUID DEFAULT NULL,
    p_limit INT DEFAULT 50, p_offset INT DEFAULT 0
) RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_result JSON;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    SELECT json_build_object('total', (SELECT COUNT(*) FROM public.tournaments
        WHERE (p_status IS NULL OR status = p_status) AND (p_grade_id IS NULL OR grade_id = p_grade_id)),
        'items', COALESCE(json_agg(row_to_json(r) ORDER BY r.starts_at DESC), '[]'::json))
    INTO v_result FROM (
        SELECT t.id, t.title, t.status, t.grade_id, g.label AS grade_label,
            t.question_count, t.time_limit_seconds, t.starts_at, t.ends_at,
            (SELECT COUNT(*) FROM public.tournament_entries WHERE tournament_id = t.id)::int AS participants,
            (SELECT COUNT(*) FROM public.tournament_entries WHERE tournament_id = t.id AND status = 'completed')::int AS completed
        FROM public.tournaments t LEFT JOIN public.grades g ON g.id = t.grade_id
        WHERE (p_status IS NULL OR t.status = p_status) AND (p_grade_id IS NULL OR t.grade_id = p_grade_id)
        ORDER BY t.starts_at DESC LIMIT p_limit OFFSET p_offset
    ) r;
    RETURN v_result;
END; $$;

-- ─── A14. admin_tournament_lifecycle ─────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_tournament_lifecycle(p_tournament_id UUID, p_action TEXT)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_current TEXT; v_new TEXT;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    SELECT status INTO v_current FROM public.tournaments WHERE id = p_tournament_id;

    v_new := CASE p_action
        WHEN 'schedule' THEN CASE WHEN v_current = 'draft' THEN 'scheduled' END
        WHEN 'go_live'  THEN CASE WHEN v_current = 'scheduled' THEN 'live' END
        WHEN 'close'    THEN CASE WHEN v_current = 'live' THEN 'closed' END
        WHEN 'finalize' THEN CASE WHEN v_current = 'closed' THEN 'finalized' END
    END;
    IF v_new IS NULL THEN
        RAISE EXCEPTION 'Invalid transition: % -> %', v_current, p_action;
    END IF;

    UPDATE public.tournaments SET status = v_new, updated_by = auth.uid(), updated_at = now()
    WHERE id = p_tournament_id;

    -- On finalize: compute final rankings
    IF v_new = 'finalized' THEN
        WITH ranked AS (
            SELECT id, ROW_NUMBER() OVER (ORDER BY score DESC, time_taken_seconds ASC)::int AS final_rank
            FROM public.tournament_entries WHERE tournament_id = p_tournament_id AND status IN ('completed','auto_submitted')
        ) UPDATE public.tournament_entries te SET rank = r.final_rank FROM ranked r WHERE te.id = r.id;
    END IF;

    INSERT INTO public.audit_logs (admin_id, action, table_name, record_id, new_values)
    VALUES (auth.uid(), 'update', 'tournaments', p_tournament_id,
        jsonb_build_object('status_from', v_current, 'status_to', v_new));
    RETURN json_build_object('status', v_new, 'previous', v_current);
END; $$;

-- ─── A15. admin_list_daily_challenges ────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_list_daily_challenges(
    p_date DATE DEFAULT CURRENT_DATE, p_grade_id UUID DEFAULT NULL
) RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_result JSON;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    SELECT COALESCE(json_agg(row_to_json(r) ORDER BY r.challenge_date DESC), '[]'::json) INTO v_result
    FROM (
        SELECT dc.id, dc.quiz_id, q.title AS quiz_title, dc.grade_id, g.label AS grade_label,
            dc.challenge_date, dc.is_active
        FROM public.daily_challenges dc
        JOIN public.quizzes q ON q.id = dc.quiz_id
        LEFT JOIN public.grades g ON g.id = dc.grade_id
        WHERE (dc.challenge_date = p_date OR p_date IS NULL)
          AND (p_grade_id IS NULL OR dc.grade_id = p_grade_id)
        ORDER BY dc.challenge_date DESC LIMIT 100
    ) r;
    RETURN v_result;
END; $$;


-- ═══════════════════════════════════════════════════════════════════════
-- GRANTS for new CMS RPCs
-- ═══════════════════════════════════════════════════════════════════════
GRANT EXECUTE ON FUNCTION public.admin_list_modules(TEXT, UUID, TEXT, INT, INT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_list_chapters(UUID, TEXT, INT, INT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_list_quizzes(UUID, TEXT, TEXT, INT, INT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_list_questions(UUID, TEXT, TEXT, INT, INT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_get_question_detail(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_upsert_grade(TEXT, TEXT, INT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_upsert_country(TEXT, TEXT, BOOLEAN) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_upsert_city(UUID, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_register_media(TEXT, INT, TEXT, TEXT, TEXT, TEXT, INT, INT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_list_media(TEXT, INT, INT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_get_audit_logs(TEXT, TEXT, UUID, INT, INT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_get_analytics(INT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_list_tournaments(TEXT, UUID, INT, INT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_tournament_lifecycle(UUID, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_list_daily_challenges(DATE, UUID) TO authenticated;


-- ═══════════════════════════════════════════════════════════════════════
-- PART B: IQ QUIZ SEED — 60 QUESTIONS (ALL 12 QUESTION TYPES)
-- ═══════════════════════════════════════════════════════════════════════

-- ─── IQ Module + Chapter + Quiz ──────────────────────────────────────
INSERT INTO public.modules (id, title, subtitle, description, emoji, accent_color, display_order, is_active, status)
VALUES ('a1000000-0000-0000-0000-000000000099', 'IQ Challenge', 'Test your brainpower',
    'A comprehensive IQ test covering all question formats', '🧠', '#7C3AED', 10, true, 'published')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.chapters (id, module_id, title, description, chapter_number, sort_order, is_active, status)
VALUES ('b1000000-0000-0000-0000-000000000099', 'a1000000-0000-0000-0000-000000000099',
    'Full IQ Test', 'All question types in one quiz', 1, 1, true, 'published')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.quizzes (id, chapter_id, title, quiz_type, question_count, time_limit_secs, max_xp, difficulty, sort_order, is_active, status, cooldown_hours)
VALUES ('c1000000-0000-0000-0000-000000000099', 'b1000000-0000-0000-0000-000000000099',
    'Complete IQ Assessment', 'iq', 60, 3600, 500, 'hard', 1, true, 'published', 168)
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 1: MULTIPLE_CHOICE (Questions 1-5)
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status) VALUES
('iq000000-0000-0000-0001-000000000001', 'c1000000-0000-0000-0000-000000000099', 'multiple_choice', 'What comes next: 2, 6, 18, 54, ?', 'Find the pattern', 'Each number is multiplied by 3. 54 x 3 = 162', 'medium', 1, 'published'),
('iq000000-0000-0000-0001-000000000002', 'c1000000-0000-0000-0000-000000000099', 'multiple_choice', 'If all Bloops are Razzies, and all Razzies are Lazzies, then all Bloops are definitely Lazzies?', 'Logical deduction', 'This is a syllogism: if A⊂B and B⊂C then A⊂C', 'easy', 2, 'published'),
('iq000000-0000-0000-0001-000000000003', 'c1000000-0000-0000-0000-000000000099', 'multiple_choice', 'Which shape completes the pattern? ◯ △ ◯ △ ◯ ?', 'Identify the alternating pattern', 'The pattern alternates between circle and triangle', 'easy', 3, 'published'),
('iq000000-0000-0000-0001-000000000004', 'c1000000-0000-0000-0000-000000000099', 'multiple_choice', 'What is 15% of 200?', 'Calculate the percentage', '15/100 × 200 = 30', 'easy', 4, 'published'),
('iq000000-0000-0000-0001-000000000005', 'c1000000-0000-0000-0000-000000000099', 'multiple_choice', 'Which word does NOT belong: Apple, Banana, Carrot, Mango?', 'Find the odd one out', 'Carrot is a vegetable; the rest are fruits', 'easy', 5, 'published')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('iqo00000-0001-0001-0000-000000000001', 'iq000000-0000-0000-0001-000000000001', '108', false, 1),
('iqo00000-0001-0001-0000-000000000002', 'iq000000-0000-0000-0001-000000000001', '162', true, 2),
('iqo00000-0001-0001-0000-000000000003', 'iq000000-0000-0000-0001-000000000001', '216', false, 3),
('iqo00000-0001-0001-0000-000000000004', 'iq000000-0000-0000-0001-000000000001', '148', false, 4),
('iqo00000-0001-0002-0000-000000000001', 'iq000000-0000-0000-0001-000000000002', 'True', true, 1),
('iqo00000-0001-0002-0000-000000000002', 'iq000000-0000-0000-0001-000000000002', 'False', false, 2),
('iqo00000-0001-0002-0000-000000000003', 'iq000000-0000-0000-0001-000000000002', 'Cannot be determined', false, 3),
('iqo00000-0001-0002-0000-000000000004', 'iq000000-0000-0000-0001-000000000002', 'Only sometimes', false, 4),
('iqo00000-0001-0003-0000-000000000001', 'iq000000-0000-0000-0001-000000000003', '◯', false, 1),
('iqo00000-0001-0003-0000-000000000002', 'iq000000-0000-0000-0001-000000000003', '△', true, 2),
('iqo00000-0001-0003-0000-000000000003', 'iq000000-0000-0000-0001-000000000003', '□', false, 3),
('iqo00000-0001-0003-0000-000000000004', 'iq000000-0000-0000-0001-000000000003', '⬡', false, 4),
('iqo00000-0001-0004-0000-000000000001', 'iq000000-0000-0000-0001-000000000004', '25', false, 1),
('iqo00000-0001-0004-0000-000000000002', 'iq000000-0000-0000-0001-000000000004', '30', true, 2),
('iqo00000-0001-0004-0000-000000000003', 'iq000000-0000-0000-0001-000000000004', '35', false, 3),
('iqo00000-0001-0004-0000-000000000004', 'iq000000-0000-0000-0001-000000000004', '15', false, 4),
('iqo00000-0001-0005-0000-000000000001', 'iq000000-0000-0000-0001-000000000005', 'Apple', false, 1),
('iqo00000-0001-0005-0000-000000000002', 'iq000000-0000-0000-0001-000000000005', 'Banana', false, 2),
('iqo00000-0001-0005-0000-000000000003', 'iq000000-0000-0000-0001-000000000005', 'Carrot', true, 3),
('iqo00000-0001-0005-0000-000000000004', 'iq000000-0000-0000-0001-000000000005', 'Mango', false, 4)
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 2: TRUE_FALSE (Questions 6-10)
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status) VALUES
('iq000000-0000-0000-0002-000000000001', 'c1000000-0000-0000-0000-000000000099', 'true_false', 'The square root of 144 is 12', 'True or False?', '12 × 12 = 144', 'easy', 6, 'published'),
('iq000000-0000-0000-0002-000000000002', 'c1000000-0000-0000-0000-000000000099', 'true_false', 'All prime numbers are odd', 'True or False?', '2 is a prime number and it is even', 'medium', 7, 'published'),
('iq000000-0000-0000-0002-000000000003', 'c1000000-0000-0000-0000-000000000099', 'true_false', 'A triangle can have two right angles', 'True or False?', 'Sum of angles = 180. Two right angles = 180, leaving 0 for the third', 'easy', 8, 'published'),
('iq000000-0000-0000-0002-000000000004', 'c1000000-0000-0000-0000-000000000099', 'true_false', 'Water boils at 100 degrees Celsius at sea level', 'True or False?', 'Standard boiling point of water at 1 atm', 'easy', 9, 'published'),
('iq000000-0000-0000-0002-000000000005', 'c1000000-0000-0000-0000-000000000099', 'true_false', 'The sun revolves around the Earth', 'True or False?', 'The Earth revolves around the Sun', 'easy', 10, 'published')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('iqo00000-0002-0001-0000-000000000001', 'iq000000-0000-0000-0002-000000000001', 'True', true, 1),
('iqo00000-0002-0001-0000-000000000002', 'iq000000-0000-0000-0002-000000000001', 'False', false, 2),
('iqo00000-0002-0002-0000-000000000001', 'iq000000-0000-0000-0002-000000000002', 'True', false, 1),
('iqo00000-0002-0002-0000-000000000002', 'iq000000-0000-0000-0002-000000000002', 'False', true, 2),
('iqo00000-0002-0003-0000-000000000001', 'iq000000-0000-0000-0002-000000000003', 'True', false, 1),
('iqo00000-0002-0003-0000-000000000002', 'iq000000-0000-0000-0002-000000000003', 'False', true, 2),
('iqo00000-0002-0004-0000-000000000001', 'iq000000-0000-0000-0002-000000000004', 'True', true, 1),
('iqo00000-0002-0004-0000-000000000002', 'iq000000-0000-0000-0002-000000000004', 'False', false, 2),
('iqo00000-0002-0005-0000-000000000001', 'iq000000-0000-0000-0002-000000000005', 'True', false, 1),
('iqo00000-0002-0005-0000-000000000002', 'iq000000-0000-0000-0002-000000000005', 'False', true, 2)
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 3: ORDERING (Questions 11-15) — options have correct_position
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status) VALUES
('iq000000-0000-0000-0003-000000000001', 'c1000000-0000-0000-0000-000000000099', 'ordering', 'Arrange from smallest to largest: 0.5, 1/4, 0.75, 1/3', 'Drag to order', '1/4=0.25, 1/3=0.33, 0.5, 0.75', 'medium', 11, 'published'),
('iq000000-0000-0000-0003-000000000002', 'c1000000-0000-0000-0000-000000000099', 'ordering', 'Order these planets from closest to farthest from Sun', 'Drag to order', 'Mercury, Venus, Earth, Mars', 'easy', 12, 'published'),
('iq000000-0000-0000-0003-000000000003', 'c1000000-0000-0000-0000-000000000099', 'ordering', 'Arrange in chronological order of invention', 'Drag to order', 'Wheel → Printing Press → Telephone → Internet', 'medium', 13, 'published'),
('iq000000-0000-0000-0003-000000000004', 'c1000000-0000-0000-0000-000000000099', 'ordering', 'Order from lightest to heaviest: Feather, Book, Car, Elephant', 'Drag to order', 'Feather < Book < Car < Elephant', 'easy', 14, 'published'),
('iq000000-0000-0000-0003-000000000005', 'c1000000-0000-0000-0000-000000000099', 'ordering', 'Arrange these numbers in descending order: 17, 42, 8, 31', 'Drag to order', '42, 31, 17, 8', 'easy', 15, 'published')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order, correct_position) VALUES
('iqo00000-0003-0001-0000-000000000001', 'iq000000-0000-0000-0003-000000000001', '0.5', false, 1, 3),
('iqo00000-0003-0001-0000-000000000002', 'iq000000-0000-0000-0003-000000000001', '1/4', false, 2, 1),
('iqo00000-0003-0001-0000-000000000003', 'iq000000-0000-0000-0003-000000000001', '0.75', false, 3, 4),
('iqo00000-0003-0001-0000-000000000004', 'iq000000-0000-0000-0003-000000000001', '1/3', false, 4, 2),
('iqo00000-0003-0002-0000-000000000001', 'iq000000-0000-0000-0003-000000000002', 'Mercury', false, 1, 1),
('iqo00000-0003-0002-0000-000000000002', 'iq000000-0000-0000-0003-000000000002', 'Venus', false, 2, 2),
('iqo00000-0003-0002-0000-000000000003', 'iq000000-0000-0000-0003-000000000002', 'Earth', false, 3, 3),
('iqo00000-0003-0002-0000-000000000004', 'iq000000-0000-0000-0003-000000000002', 'Mars', false, 4, 4),
('iqo00000-0003-0003-0000-000000000001', 'iq000000-0000-0000-0003-000000000003', 'Wheel', false, 1, 1),
('iqo00000-0003-0003-0000-000000000002', 'iq000000-0000-0000-0003-000000000003', 'Printing Press', false, 2, 2),
('iqo00000-0003-0003-0000-000000000003', 'iq000000-0000-0000-0003-000000000003', 'Telephone', false, 3, 3),
('iqo00000-0003-0003-0000-000000000004', 'iq000000-0000-0000-0003-000000000003', 'Internet', false, 4, 4),
('iqo00000-0003-0004-0000-000000000001', 'iq000000-0000-0000-0003-000000000004', 'Feather', false, 1, 1),
('iqo00000-0003-0004-0000-000000000002', 'iq000000-0000-0000-0003-000000000004', 'Book', false, 2, 2),
('iqo00000-0003-0004-0000-000000000003', 'iq000000-0000-0000-0003-000000000004', 'Car', false, 3, 3),
('iqo00000-0003-0004-0000-000000000004', 'iq000000-0000-0000-0003-000000000004', 'Elephant', false, 4, 4),
('iqo00000-0003-0005-0000-000000000001', 'iq000000-0000-0000-0003-000000000005', '42', false, 1, 1),
('iqo00000-0003-0005-0000-000000000002', 'iq000000-0000-0000-0003-000000000005', '31', false, 2, 2),
('iqo00000-0003-0005-0000-000000000003', 'iq000000-0000-0000-0003-000000000005', '17', false, 3, 3),
('iqo00000-0003-0005-0000-000000000004', 'iq000000-0000-0000-0003-000000000005', '8', false, 4, 4)
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 4: MATCH (Questions 16-20) — uses match_pairs table
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status) VALUES
('iq000000-0000-0000-0004-000000000001', 'c1000000-0000-0000-0000-000000000099', 'match', 'Match the country with its capital', 'Draw lines to match', 'Standard geography knowledge', 'medium', 16, 'published'),
('iq000000-0000-0000-0004-000000000002', 'c1000000-0000-0000-0000-000000000099', 'match', 'Match the animal with its sound', 'Draw lines to match', 'Common animal sounds', 'easy', 17, 'published'),
('iq000000-0000-0000-0004-000000000003', 'c1000000-0000-0000-0000-000000000099', 'match', 'Match the shape with its number of sides', 'Draw lines to match', 'Basic geometry', 'easy', 18, 'published'),
('iq000000-0000-0000-0004-000000000004', 'c1000000-0000-0000-0000-000000000099', 'match', 'Match the operator with its meaning', 'Draw lines to match', 'Math operators', 'easy', 19, 'published'),
('iq000000-0000-0000-0004-000000000005', 'c1000000-0000-0000-0000-000000000099', 'match', 'Match the word with its antonym', 'Draw lines to match', 'Antonyms are opposite meanings', 'medium', 20, 'published')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.match_pairs (id, question_id, left_text, right_text, sort_order) VALUES
('mp000000-0004-0001-0000-000000000001', 'iq000000-0000-0000-0004-000000000001', 'India', 'New Delhi', 1),
('mp000000-0004-0001-0000-000000000002', 'iq000000-0000-0000-0004-000000000001', 'France', 'Paris', 2),
('mp000000-0004-0001-0000-000000000003', 'iq000000-0000-0000-0004-000000000001', 'Japan', 'Tokyo', 3),
('mp000000-0004-0001-0000-000000000004', 'iq000000-0000-0000-0004-000000000001', 'Australia', 'Canberra', 4),
('mp000000-0004-0002-0000-000000000001', 'iq000000-0000-0000-0004-000000000002', 'Dog', 'Bark', 1),
('mp000000-0004-0002-0000-000000000002', 'iq000000-0000-0000-0004-000000000002', 'Cat', 'Meow', 2),
('mp000000-0004-0002-0000-000000000003', 'iq000000-0000-0000-0004-000000000002', 'Cow', 'Moo', 3),
('mp000000-0004-0002-0000-000000000004', 'iq000000-0000-0000-0004-000000000002', 'Duck', 'Quack', 4),
('mp000000-0004-0003-0000-000000000001', 'iq000000-0000-0000-0004-000000000003', 'Triangle', '3', 1),
('mp000000-0004-0003-0000-000000000002', 'iq000000-0000-0000-0004-000000000003', 'Square', '4', 2),
('mp000000-0004-0003-0000-000000000003', 'iq000000-0000-0000-0004-000000000003', 'Pentagon', '5', 3),
('mp000000-0004-0003-0000-000000000004', 'iq000000-0000-0000-0004-000000000003', 'Hexagon', '6', 4),
('mp000000-0004-0004-0000-000000000001', 'iq000000-0000-0000-0004-000000000004', '+', 'Addition', 1),
('mp000000-0004-0004-0000-000000000002', 'iq000000-0000-0000-0004-000000000004', '-', 'Subtraction', 2),
('mp000000-0004-0004-0000-000000000003', 'iq000000-0000-0000-0004-000000000004', '×', 'Multiplication', 3),
('mp000000-0004-0004-0000-000000000004', 'iq000000-0000-0000-0004-000000000004', '÷', 'Division', 4),
('mp000000-0004-0005-0000-000000000001', 'iq000000-0000-0000-0004-000000000005', 'Hot', 'Cold', 1),
('mp000000-0004-0005-0000-000000000002', 'iq000000-0000-0000-0004-000000000005', 'Big', 'Small', 2),
('mp000000-0004-0005-0000-000000000003', 'iq000000-0000-0000-0004-000000000005', 'Fast', 'Slow', 3),
('mp000000-0004-0005-0000-000000000004', 'iq000000-0000-0000-0004-000000000005', 'Happy', 'Sad', 4)
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 5: FILL_BLANK (Questions 21-25)
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status) VALUES
('iq000000-0000-0000-0005-000000000001', 'c1000000-0000-0000-0000-000000000099', 'fill_blank', 'The capital of India is ___', 'Fill in the blank', 'New Delhi is the capital of India', 'easy', 21, 'published'),
('iq000000-0000-0000-0005-000000000002', 'c1000000-0000-0000-0000-000000000099', 'fill_blank', '8 × 7 = ___', 'Fill in the blank', '8 × 7 = 56', 'easy', 22, 'published'),
('iq000000-0000-0000-0005-000000000003', 'c1000000-0000-0000-0000-000000000099', 'fill_blank', 'The chemical symbol for water is ___', 'Fill in the blank', 'H2O is the chemical formula for water', 'easy', 23, 'published'),
('iq000000-0000-0000-0005-000000000004', 'c1000000-0000-0000-0000-000000000099', 'fill_blank', 'A rectangle has ___ sides', 'Fill in the blank', 'A rectangle has 4 sides', 'easy', 24, 'published'),
('iq000000-0000-0000-0005-000000000005', 'c1000000-0000-0000-0000-000000000099', 'fill_blank', 'The past tense of "go" is ___', 'Fill in the blank', 'The past tense of go is went', 'easy', 25, 'published')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('iqo00000-0005-0001-0000-000000000001', 'iq000000-0000-0000-0005-000000000001', 'New Delhi', true, 1),
('iqo00000-0005-0001-0000-000000000002', 'iq000000-0000-0000-0005-000000000001', 'Mumbai', false, 2),
('iqo00000-0005-0001-0000-000000000003', 'iq000000-0000-0000-0005-000000000001', 'Kolkata', false, 3),
('iqo00000-0005-0001-0000-000000000004', 'iq000000-0000-0000-0005-000000000001', 'Chennai', false, 4),
('iqo00000-0005-0002-0000-000000000001', 'iq000000-0000-0000-0005-000000000002', '56', true, 1),
('iqo00000-0005-0002-0000-000000000002', 'iq000000-0000-0000-0005-000000000002', '48', false, 2),
('iqo00000-0005-0002-0000-000000000003', 'iq000000-0000-0000-0005-000000000002', '54', false, 3),
('iqo00000-0005-0002-0000-000000000004', 'iq000000-0000-0000-0005-000000000002', '64', false, 4),
('iqo00000-0005-0003-0000-000000000001', 'iq000000-0000-0000-0005-000000000003', 'H2O', true, 1),
('iqo00000-0005-0003-0000-000000000002', 'iq000000-0000-0000-0005-000000000003', 'CO2', false, 2),
('iqo00000-0005-0003-0000-000000000003', 'iq000000-0000-0000-0005-000000000003', 'O2', false, 3),
('iqo00000-0005-0003-0000-000000000004', 'iq000000-0000-0000-0005-000000000003', 'NaCl', false, 4),
('iqo00000-0005-0004-0000-000000000001', 'iq000000-0000-0000-0005-000000000004', '4', true, 1),
('iqo00000-0005-0004-0000-000000000002', 'iq000000-0000-0000-0005-000000000004', '3', false, 2),
('iqo00000-0005-0004-0000-000000000003', 'iq000000-0000-0000-0005-000000000004', '5', false, 3),
('iqo00000-0005-0004-0000-000000000004', 'iq000000-0000-0000-0005-000000000004', '6', false, 4),
('iqo00000-0005-0005-0000-000000000001', 'iq000000-0000-0000-0005-000000000005', 'went', true, 1),
('iqo00000-0005-0005-0000-000000000002', 'iq000000-0000-0000-0005-000000000005', 'gone', false, 2),
('iqo00000-0005-0005-0000-000000000003', 'iq000000-0000-0000-0005-000000000005', 'goed', false, 3),
('iqo00000-0005-0005-0000-000000000004', 'iq000000-0000-0000-0005-000000000005', 'going', false, 4)
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 6: SELECT_WORD (Questions 26-30)
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status) VALUES
('iq000000-0000-0000-0006-000000000001', 'c1000000-0000-0000-0000-000000000099', 'select_word', 'Select the noun in: "The quick brown fox jumps"', 'Tap the noun', 'Fox is the noun (person/place/thing)', 'easy', 26, 'published'),
('iq000000-0000-0000-0006-000000000002', 'c1000000-0000-0000-0000-000000000099', 'select_word', 'Select the verb in: "She reads books daily"', 'Tap the verb', 'Reads is the action word (verb)', 'easy', 27, 'published'),
('iq000000-0000-0000-0006-000000000003', 'c1000000-0000-0000-0000-000000000099', 'select_word', 'Select the adjective in: "A tall building stood there"', 'Tap the adjective', 'Tall describes the building', 'easy', 28, 'published'),
('iq000000-0000-0000-0006-000000000004', 'c1000000-0000-0000-0000-000000000099', 'select_word', 'Select the adverb in: "He ran quickly to school"', 'Tap the adverb', 'Quickly describes how he ran', 'medium', 29, 'published'),
('iq000000-0000-0000-0006-000000000005', 'c1000000-0000-0000-0000-000000000099', 'select_word', 'Select the preposition in: "The cat is under the table"', 'Tap the preposition', 'Under shows position/relationship', 'medium', 30, 'published')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('iqo00000-0006-0001-0000-000000000001', 'iq000000-0000-0000-0006-000000000001', 'quick', false, 1),
('iqo00000-0006-0001-0000-000000000002', 'iq000000-0000-0000-0006-000000000001', 'brown', false, 2),
('iqo00000-0006-0001-0000-000000000003', 'iq000000-0000-0000-0006-000000000001', 'fox', true, 3),
('iqo00000-0006-0001-0000-000000000004', 'iq000000-0000-0000-0006-000000000001', 'jumps', false, 4),
('iqo00000-0006-0002-0000-000000000001', 'iq000000-0000-0000-0006-000000000002', 'She', false, 1),
('iqo00000-0006-0002-0000-000000000002', 'iq000000-0000-0000-0006-000000000002', 'reads', true, 2),
('iqo00000-0006-0002-0000-000000000003', 'iq000000-0000-0000-0006-000000000002', 'books', false, 3),
('iqo00000-0006-0002-0000-000000000004', 'iq000000-0000-0000-0006-000000000002', 'daily', false, 4),
('iqo00000-0006-0003-0000-000000000001', 'iq000000-0000-0000-0006-000000000003', 'A', false, 1),
('iqo00000-0006-0003-0000-000000000002', 'iq000000-0000-0000-0006-000000000003', 'tall', true, 2),
('iqo00000-0006-0003-0000-000000000003', 'iq000000-0000-0000-0006-000000000003', 'building', false, 3),
('iqo00000-0006-0003-0000-000000000004', 'iq000000-0000-0000-0006-000000000003', 'stood', false, 4),
('iqo00000-0006-0004-0000-000000000001', 'iq000000-0000-0000-0006-000000000004', 'He', false, 1),
('iqo00000-0006-0004-0000-000000000002', 'iq000000-0000-0000-0006-000000000004', 'ran', false, 2),
('iqo00000-0006-0004-0000-000000000003', 'iq000000-0000-0000-0006-000000000004', 'quickly', true, 3),
('iqo00000-0006-0004-0000-000000000004', 'iq000000-0000-0000-0006-000000000004', 'school', false, 4),
('iqo00000-0006-0005-0000-000000000001', 'iq000000-0000-0000-0006-000000000005', 'cat', false, 1),
('iqo00000-0006-0005-0000-000000000002', 'iq000000-0000-0000-0006-000000000005', 'is', false, 2),
('iqo00000-0006-0005-0000-000000000003', 'iq000000-0000-0000-0006-000000000005', 'under', true, 3),
('iqo00000-0006-0005-0000-000000000004', 'iq000000-0000-0000-0006-000000000005', 'table', false, 4)
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 7: MATRIX (Questions 31-35) — uses prompt_config for matrix data
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status, prompt_config) VALUES
('iq000000-0000-0000-0007-000000000001', 'c1000000-0000-0000-0000-000000000099', 'matrix', 'Complete the 2x2 number matrix', 'Find the missing number',
    'Each row adds up to 10. Missing = 10 - 7 = 3', 'medium', 31, 'published',
    '{"rows":[["5","5"],["7","?"]],"row_labels":[],"col_labels":[]}'::jsonb),
('iq000000-0000-0000-0007-000000000002', 'c1000000-0000-0000-0000-000000000099', 'matrix', 'Find the pattern in this 3x3 matrix', 'What replaces the ?',
    'Each row doubles: 1→2→4, 3→6→12, 5→10→?=20', 'hard', 32, 'published',
    '{"rows":[["1","2","4"],["3","6","12"],["5","10","?"]]}'::jsonb),
('iq000000-0000-0000-0007-000000000003', 'c1000000-0000-0000-0000-000000000099', 'matrix', 'Complete the letter matrix', 'Find the pattern',
    'Rows go A→B→C, D→E→F, G→H→I', 'easy', 33, 'published',
    '{"rows":[["A","B","C"],["D","E","F"],["G","H","?"]]}'::jsonb),
('iq000000-0000-0000-0007-000000000004', 'c1000000-0000-0000-0000-000000000099', 'matrix', 'Multiplication table matrix', 'Find the missing product',
    '4 × 6 = 24', 'easy', 34, 'published',
    '{"rows":[["×","3","6"],["2","6","12"],["4","12","?"]],"col_labels":["×","3","6"]}'::jsonb),
('iq000000-0000-0000-0007-000000000005', 'c1000000-0000-0000-0000-000000000099', 'matrix', 'Complete the subtraction matrix', 'Each row: first - second = third',
    '15 - 6 = 9', 'medium', 35, 'published',
    '{"rows":[["10","4","6"],["12","5","7"],["15","6","?"]]}'::jsonb)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('iqo00000-0007-0001-0000-000000000001', 'iq000000-0000-0000-0007-000000000001', '3', true, 1),
('iqo00000-0007-0001-0000-000000000002', 'iq000000-0000-0000-0007-000000000001', '2', false, 2),
('iqo00000-0007-0001-0000-000000000003', 'iq000000-0000-0000-0007-000000000001', '5', false, 3),
('iqo00000-0007-0001-0000-000000000004', 'iq000000-0000-0000-0007-000000000001', '4', false, 4),
('iqo00000-0007-0002-0000-000000000001', 'iq000000-0000-0000-0007-000000000002', '20', true, 1),
('iqo00000-0007-0002-0000-000000000002', 'iq000000-0000-0000-0007-000000000002', '15', false, 2),
('iqo00000-0007-0002-0000-000000000003', 'iq000000-0000-0000-0007-000000000002', '25', false, 3),
('iqo00000-0007-0002-0000-000000000004', 'iq000000-0000-0000-0007-000000000002', '18', false, 4),
('iqo00000-0007-0003-0000-000000000001', 'iq000000-0000-0000-0007-000000000003', 'I', true, 1),
('iqo00000-0007-0003-0000-000000000002', 'iq000000-0000-0000-0007-000000000003', 'J', false, 2),
('iqo00000-0007-0003-0000-000000000003', 'iq000000-0000-0000-0007-000000000003', 'K', false, 3),
('iqo00000-0007-0003-0000-000000000004', 'iq000000-0000-0000-0007-000000000003', 'L', false, 4),
('iqo00000-0007-0004-0000-000000000001', 'iq000000-0000-0000-0007-000000000004', '24', true, 1),
('iqo00000-0007-0004-0000-000000000002', 'iq000000-0000-0000-0007-000000000004', '18', false, 2),
('iqo00000-0007-0004-0000-000000000003', 'iq000000-0000-0000-0007-000000000004', '20', false, 3),
('iqo00000-0007-0004-0000-000000000004', 'iq000000-0000-0000-0007-000000000004', '16', false, 4),
('iqo00000-0007-0005-0000-000000000001', 'iq000000-0000-0000-0007-000000000005', '9', true, 1),
('iqo00000-0007-0005-0000-000000000002', 'iq000000-0000-0000-0007-000000000005', '8', false, 2),
('iqo00000-0007-0005-0000-000000000003', 'iq000000-0000-0000-0007-000000000005', '10', false, 3),
('iqo00000-0007-0005-0000-000000000004', 'iq000000-0000-0000-0007-000000000005', '7', false, 4)
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 8: GRID_PATTERN (Questions 36-40) — uses prompt_config for grid
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status, prompt_config) VALUES
('iq000000-0000-0000-0008-000000000001', 'c1000000-0000-0000-0000-000000000099', 'grid_pattern', 'Which tile completes the 3x3 grid?', 'Find the missing tile',
    'Pattern rotates 90 degrees each cell', 'hard', 36, 'published',
    '{"grid":[["⬆","➡","⬇"],["➡","⬇","⬅"],["⬇","⬅","?"]],"size":3}'::jsonb),
('iq000000-0000-0000-0008-000000000002', 'c1000000-0000-0000-0000-000000000099', 'grid_pattern', 'Complete the color pattern grid', 'Which color goes in the empty cell?',
    'Colors alternate in a checkerboard', 'medium', 37, 'published',
    '{"grid":[["R","B","R"],["B","R","B"],["R","B","?"]],"size":3}'::jsonb),
('iq000000-0000-0000-0008-000000000003', 'c1000000-0000-0000-0000-000000000099', 'grid_pattern', 'Number grid: find the missing value', 'Each row and column has a pattern',
    'Diagonals sum to 15 in a magic square', 'hard', 38, 'published',
    '{"grid":[["2","7","6"],["9","5","1"],["4","3","?"]],"size":3}'::jsonb),
('iq000000-0000-0000-0008-000000000004', 'c1000000-0000-0000-0000-000000000099', 'grid_pattern', 'Shape rotation grid', 'What shape goes in the blank?',
    'Each row cycles through 3 shapes', 'medium', 39, 'published',
    '{"grid":[["◯","△","□"],["△","□","◯"],["□","◯","?"]],"size":3}'::jsonb),
('iq000000-0000-0000-0008-000000000005', 'c1000000-0000-0000-0000-000000000099', 'grid_pattern', 'Dot count pattern', 'How many dots in the missing cell?',
    'Each row increases by 1: 1,2,3 / 2,3,4 / 3,4,?=5', 'medium', 40, 'published',
    '{"grid":[["1","2","3"],["2","3","4"],["3","4","?"]],"size":3}'::jsonb)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('iqo00000-0008-0001-0000-000000000001', 'iq000000-0000-0000-0008-000000000001', '⬆', true, 1),
('iqo00000-0008-0001-0000-000000000002', 'iq000000-0000-0000-0008-000000000001', '➡', false, 2),
('iqo00000-0008-0001-0000-000000000003', 'iq000000-0000-0000-0008-000000000001', '⬇', false, 3),
('iqo00000-0008-0001-0000-000000000004', 'iq000000-0000-0000-0008-000000000001', '⬅', false, 4),
('iqo00000-0008-0002-0000-000000000001', 'iq000000-0000-0000-0008-000000000002', 'R', true, 1),
('iqo00000-0008-0002-0000-000000000002', 'iq000000-0000-0000-0008-000000000002', 'B', false, 2),
('iqo00000-0008-0002-0000-000000000003', 'iq000000-0000-0000-0008-000000000002', 'G', false, 3),
('iqo00000-0008-0002-0000-000000000004', 'iq000000-0000-0000-0008-000000000002', 'Y', false, 4),
('iqo00000-0008-0003-0000-000000000001', 'iq000000-0000-0000-0008-000000000003', '8', true, 1),
('iqo00000-0008-0003-0000-000000000002', 'iq000000-0000-0000-0008-000000000003', '6', false, 2),
('iqo00000-0008-0003-0000-000000000003', 'iq000000-0000-0000-0008-000000000003', '7', false, 3),
('iqo00000-0008-0003-0000-000000000004', 'iq000000-0000-0000-0008-000000000003', '9', false, 4),
('iqo00000-0008-0004-0000-000000000001', 'iq000000-0000-0000-0008-000000000004', '△', true, 1),
('iqo00000-0008-0004-0000-000000000002', 'iq000000-0000-0000-0008-000000000004', '□', false, 2),
('iqo00000-0008-0004-0000-000000000003', 'iq000000-0000-0000-0008-000000000004', '◯', false, 3),
('iqo00000-0008-0004-0000-000000000004', 'iq000000-0000-0000-0008-000000000004', '⬡', false, 4),
('iqo00000-0008-0005-0000-000000000001', 'iq000000-0000-0000-0008-000000000005', '5', true, 1),
('iqo00000-0008-0005-0000-000000000002', 'iq000000-0000-0000-0008-000000000005', '4', false, 2),
('iqo00000-0008-0005-0000-000000000003', 'iq000000-0000-0000-0008-000000000005', '6', false, 3),
('iqo00000-0008-0005-0000-000000000004', 'iq000000-0000-0000-0008-000000000005', '3', false, 4)
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 9: STATEMENT_REASON (Questions 41-45)
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status, metadata) VALUES
('iq000000-0000-0000-0009-000000000001', 'c1000000-0000-0000-0000-000000000099', 'statement_reason',
    'Statement: All metals conduct electricity. Reason: Metals have free electrons.',
    'Are both correct? Does the reason explain the statement?',
    'Both are true and the reason correctly explains the statement', 'hard', 41, 'published',
    '{"statement":"All metals conduct electricity","reason":"Metals have free electrons"}'::jsonb),
('iq000000-0000-0000-0009-000000000002', 'c1000000-0000-0000-0000-000000000099', 'statement_reason',
    'Statement: Plants are green. Reason: Plants have chlorophyll.',
    'Evaluate the statement and reason',
    'Both true, chlorophyll gives plants their green color', 'medium', 42, 'published',
    '{"statement":"Plants are green","reason":"Plants contain chlorophyll"}'::jsonb),
('iq000000-0000-0000-0009-000000000003', 'c1000000-0000-0000-0000-000000000099', 'statement_reason',
    'Statement: Ice floats on water. Reason: Ice is denser than water.',
    'Evaluate the statement and reason',
    'Statement is true but reason is false. Ice is LESS dense than water', 'hard', 43, 'published',
    '{"statement":"Ice floats on water","reason":"Ice is denser than water"}'::jsonb),
('iq000000-0000-0000-0009-000000000004', 'c1000000-0000-0000-0000-000000000099', 'statement_reason',
    'Statement: The moon produces its own light. Reason: The moon is a star.',
    'Evaluate the statement and reason',
    'Both are false. The moon reflects sunlight and is a satellite', 'medium', 44, 'published',
    '{"statement":"The moon produces its own light","reason":"The moon is a star"}'::jsonb),
('iq000000-0000-0000-0009-000000000005', 'c1000000-0000-0000-0000-000000000099', 'statement_reason',
    'Statement: Squares have 4 equal sides. Reason: All quadrilaterals have 4 sides.',
    'Evaluate the statement and reason',
    'Both true, but reason does not explain the statement', 'hard', 45, 'published',
    '{"statement":"Squares have 4 equal sides","reason":"All quadrilaterals have 4 sides"}'::jsonb)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('iqo00000-0009-0001-0000-000000000001', 'iq000000-0000-0000-0009-000000000001', 'Both true; reason explains statement', true, 1),
('iqo00000-0009-0001-0000-000000000002', 'iq000000-0000-0000-0009-000000000001', 'Both true; reason does NOT explain', false, 2),
('iqo00000-0009-0001-0000-000000000003', 'iq000000-0000-0000-0009-000000000001', 'Statement true; reason false', false, 3),
('iqo00000-0009-0001-0000-000000000004', 'iq000000-0000-0000-0009-000000000001', 'Both false', false, 4),
('iqo00000-0009-0002-0000-000000000001', 'iq000000-0000-0000-0009-000000000002', 'Both true; reason explains statement', true, 1),
('iqo00000-0009-0002-0000-000000000002', 'iq000000-0000-0000-0009-000000000002', 'Both true; reason does NOT explain', false, 2),
('iqo00000-0009-0002-0000-000000000003', 'iq000000-0000-0000-0009-000000000002', 'Statement true; reason false', false, 3),
('iqo00000-0009-0002-0000-000000000004', 'iq000000-0000-0000-0009-000000000002', 'Both false', false, 4),
('iqo00000-0009-0003-0000-000000000001', 'iq000000-0000-0000-0009-000000000003', 'Both true; reason explains statement', false, 1),
('iqo00000-0009-0003-0000-000000000002', 'iq000000-0000-0000-0009-000000000003', 'Both true; reason does NOT explain', false, 2),
('iqo00000-0009-0003-0000-000000000003', 'iq000000-0000-0000-0009-000000000003', 'Statement true; reason false', true, 3),
('iqo00000-0009-0003-0000-000000000004', 'iq000000-0000-0000-0009-000000000003', 'Both false', false, 4),
('iqo00000-0009-0004-0000-000000000001', 'iq000000-0000-0000-0009-000000000004', 'Both true; reason explains statement', false, 1),
('iqo00000-0009-0004-0000-000000000002', 'iq000000-0000-0000-0009-000000000004', 'Both true; reason does NOT explain', false, 2),
('iqo00000-0009-0004-0000-000000000003', 'iq000000-0000-0000-0009-000000000004', 'Statement true; reason false', false, 3),
('iqo00000-0009-0004-0000-000000000004', 'iq000000-0000-0000-0009-000000000004', 'Both false', true, 4),
('iqo00000-0009-0005-0000-000000000001', 'iq000000-0000-0000-0009-000000000005', 'Both true; reason explains statement', false, 1),
('iqo00000-0009-0005-0000-000000000002', 'iq000000-0000-0000-0009-000000000005', 'Both true; reason does NOT explain', true, 2),
('iqo00000-0009-0005-0000-000000000003', 'iq000000-0000-0000-0009-000000000005', 'Statement true; reason false', false, 3),
('iqo00000-0009-0005-0000-000000000004', 'iq000000-0000-0000-0009-000000000005', 'Both false', false, 4)
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 10: TABLE_DATA (Questions 46-50) — uses prompt_config for table
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status, prompt_config) VALUES
('iq000000-0000-0000-0010-000000000001', 'c1000000-0000-0000-0000-000000000099', 'table_data',
    'Study the marks table and answer', 'Who scored the highest in Math?',
    'Priya scored 95 in Math', 'easy', 46, 'published',
    '{"headers":["Student","Math","Science","English"],"rows":[["Amit","85","90","78"],["Priya","95","88","92"],["Raj","72","95","85"]]}'::jsonb),
('iq000000-0000-0000-0010-000000000002', 'c1000000-0000-0000-0000-000000000099', 'table_data',
    'Read the fruit price table', 'Which fruit costs the most per kg?',
    'Mango at 120/kg is the most expensive', 'easy', 47, 'published',
    '{"headers":["Fruit","Price/kg","Season"],"rows":[["Apple","80","Winter"],["Mango","120","Summer"],["Banana","40","All year"]]}'::jsonb),
('iq000000-0000-0000-0010-000000000003', 'c1000000-0000-0000-0000-000000000099', 'table_data',
    'Temperature log: find the coldest day', 'Which day was coldest?',
    'Wednesday at 18C was the coldest', 'easy', 48, 'published',
    '{"headers":["Day","Temp (C)","Weather"],"rows":[["Monday","25","Sunny"],["Tuesday","22","Cloudy"],["Wednesday","18","Rainy"],["Thursday","28","Sunny"]]}'::jsonb),
('iq000000-0000-0000-0010-000000000004', 'c1000000-0000-0000-0000-000000000099', 'table_data',
    'Animal lifespan table', 'Which animal lives the longest?',
    'Elephant at 70 years lives the longest', 'easy', 49, 'published',
    '{"headers":["Animal","Lifespan (years)","Type"],"rows":[["Dog","13","Mammal"],["Cat","15","Mammal"],["Elephant","70","Mammal"],["Parrot","50","Bird"]]}'::jsonb),
('iq000000-0000-0000-0010-000000000005', 'c1000000-0000-0000-0000-000000000099', 'table_data',
    'Class test results', 'How many students scored above 80?',
    '3 students (Anita 85, Vikram 92, Zara 88) scored above 80', 'medium', 50, 'published',
    '{"headers":["Name","Score","Grade"],"rows":[["Anita","85","A"],["Bikash","72","B"],["Vikram","92","A+"],["Zara","88","A"]]}'::jsonb)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('iqo00000-0010-0001-0000-000000000001', 'iq000000-0000-0000-0010-000000000001', 'Amit', false, 1),
('iqo00000-0010-0001-0000-000000000002', 'iq000000-0000-0000-0010-000000000001', 'Priya', true, 2),
('iqo00000-0010-0001-0000-000000000003', 'iq000000-0000-0000-0010-000000000001', 'Raj', false, 3),
('iqo00000-0010-0001-0000-000000000004', 'iq000000-0000-0000-0010-000000000001', 'All scored equal', false, 4),
('iqo00000-0010-0002-0000-000000000001', 'iq000000-0000-0000-0010-000000000002', 'Apple', false, 1),
('iqo00000-0010-0002-0000-000000000002', 'iq000000-0000-0000-0010-000000000002', 'Mango', true, 2),
('iqo00000-0010-0002-0000-000000000003', 'iq000000-0000-0000-0010-000000000002', 'Banana', false, 3),
('iqo00000-0010-0002-0000-000000000004', 'iq000000-0000-0000-0010-000000000002', 'All same price', false, 4),
('iqo00000-0010-0003-0000-000000000001', 'iq000000-0000-0000-0010-000000000003', 'Monday', false, 1),
('iqo00000-0010-0003-0000-000000000002', 'iq000000-0000-0000-0010-000000000003', 'Tuesday', false, 2),
('iqo00000-0010-0003-0000-000000000003', 'iq000000-0000-0000-0010-000000000003', 'Wednesday', true, 3),
('iqo00000-0010-0003-0000-000000000004', 'iq000000-0000-0000-0010-000000000003', 'Thursday', false, 4),
('iqo00000-0010-0004-0000-000000000001', 'iq000000-0000-0000-0010-000000000004', 'Dog', false, 1),
('iqo00000-0010-0004-0000-000000000002', 'iq000000-0000-0000-0010-000000000004', 'Cat', false, 2),
('iqo00000-0010-0004-0000-000000000003', 'iq000000-0000-0000-0010-000000000004', 'Elephant', true, 3),
('iqo00000-0010-0004-0000-000000000004', 'iq000000-0000-0000-0010-000000000004', 'Parrot', false, 4),
('iqo00000-0010-0005-0000-000000000001', 'iq000000-0000-0000-0010-000000000005', '1', false, 1),
('iqo00000-0010-0005-0000-000000000002', 'iq000000-0000-0000-0010-000000000005', '2', false, 2),
('iqo00000-0010-0005-0000-000000000003', 'iq000000-0000-0000-0010-000000000005', '3', true, 3),
('iqo00000-0010-0005-0000-000000000004', 'iq000000-0000-0000-0010-000000000005', '4', false, 4)
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 11: MEMORY (Questions 51-55) — options with visual_label for pairs
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status, prompt_config) VALUES
('iq000000-0000-0000-0011-000000000001', 'c1000000-0000-0000-0000-000000000099', 'memory',
    'Match the pairs: Fruits', 'Find matching fruit pairs', 'Match identical fruits from memory', 'easy', 51, 'published',
    '{"pairs":4,"category":"fruits"}'::jsonb),
('iq000000-0000-0000-0011-000000000002', 'c1000000-0000-0000-0000-000000000099', 'memory',
    'Match the pairs: Numbers and Words', 'Match number with its word form', 'Each number matches its spelled form', 'medium', 52, 'published',
    '{"pairs":4,"category":"numbers"}'::jsonb),
('iq000000-0000-0000-0011-000000000003', 'c1000000-0000-0000-0000-000000000099', 'memory',
    'Match the pairs: Colors', 'Find matching color pairs', 'Match the color emoji with its name', 'easy', 53, 'published',
    '{"pairs":4,"category":"colors"}'::jsonb),
('iq000000-0000-0000-0011-000000000004', 'c1000000-0000-0000-0000-000000000099', 'memory',
    'Match the pairs: Shapes', 'Find matching shape pairs', 'Match the shape with its name', 'easy', 54, 'published',
    '{"pairs":4,"category":"shapes"}'::jsonb),
('iq000000-0000-0000-0011-000000000005', 'c1000000-0000-0000-0000-000000000099', 'memory',
    'Match the pairs: Animals and Sounds', 'Match animal with its sound', 'Each animal matches its characteristic sound', 'medium', 55, 'published',
    '{"pairs":4,"category":"animals"}'::jsonb)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order, visual_label) VALUES
('iqo00000-0011-0001-0000-000000000001', 'iq000000-0000-0000-0011-000000000001', 'Apple', true, 1, '🍎'),
('iqo00000-0011-0001-0000-000000000002', 'iq000000-0000-0000-0011-000000000001', 'Apple', true, 2, '🍎'),
('iqo00000-0011-0001-0000-000000000003', 'iq000000-0000-0000-0011-000000000001', 'Banana', true, 3, '🍌'),
('iqo00000-0011-0001-0000-000000000004', 'iq000000-0000-0000-0011-000000000001', 'Banana', true, 4, '🍌'),
('iqo00000-0011-0001-0000-000000000005', 'iq000000-0000-0000-0011-000000000001', 'Grape', true, 5, '🍇'),
('iqo00000-0011-0001-0000-000000000006', 'iq000000-0000-0000-0011-000000000001', 'Grape', true, 6, '🍇'),
('iqo00000-0011-0001-0000-000000000007', 'iq000000-0000-0000-0011-000000000001', 'Orange', true, 7, '🍊'),
('iqo00000-0011-0001-0000-000000000008', 'iq000000-0000-0000-0011-000000000001', 'Orange', true, 8, '🍊'),
('iqo00000-0011-0002-0000-000000000001', 'iq000000-0000-0000-0011-000000000002', '1', true, 1, 'One'),
('iqo00000-0011-0002-0000-000000000002', 'iq000000-0000-0000-0011-000000000002', '1', true, 2, 'One'),
('iqo00000-0011-0002-0000-000000000003', 'iq000000-0000-0000-0011-000000000002', '2', true, 3, 'Two'),
('iqo00000-0011-0002-0000-000000000004', 'iq000000-0000-0000-0011-000000000002', '2', true, 4, 'Two'),
('iqo00000-0011-0002-0000-000000000005', 'iq000000-0000-0000-0011-000000000002', '3', true, 5, 'Three'),
('iqo00000-0011-0002-0000-000000000006', 'iq000000-0000-0000-0011-000000000002', '3', true, 6, 'Three'),
('iqo00000-0011-0002-0000-000000000007', 'iq000000-0000-0000-0011-000000000002', '4', true, 7, 'Four'),
('iqo00000-0011-0002-0000-000000000008', 'iq000000-0000-0000-0011-000000000002', '4', true, 8, 'Four'),
('iqo00000-0011-0003-0000-000000000001', 'iq000000-0000-0000-0011-000000000003', 'Red', true, 1, '🔴'),
('iqo00000-0011-0003-0000-000000000002', 'iq000000-0000-0000-0011-000000000003', 'Red', true, 2, '🔴'),
('iqo00000-0011-0003-0000-000000000003', 'iq000000-0000-0000-0011-000000000003', 'Blue', true, 3, '🔵'),
('iqo00000-0011-0003-0000-000000000004', 'iq000000-0000-0000-0011-000000000003', 'Blue', true, 4, '🔵'),
('iqo00000-0011-0003-0000-000000000005', 'iq000000-0000-0000-0011-000000000003', 'Green', true, 5, '🟢'),
('iqo00000-0011-0003-0000-000000000006', 'iq000000-0000-0000-0011-000000000003', 'Green', true, 6, '🟢'),
('iqo00000-0011-0003-0000-000000000007', 'iq000000-0000-0000-0011-000000000003', 'Yellow', true, 7, '🟡'),
('iqo00000-0011-0003-0000-000000000008', 'iq000000-0000-0000-0011-000000000003', 'Yellow', true, 8, '🟡'),
('iqo00000-0011-0004-0000-000000000001', 'iq000000-0000-0000-0011-000000000004', 'Circle', true, 1, '◯'),
('iqo00000-0011-0004-0000-000000000002', 'iq000000-0000-0000-0011-000000000004', 'Circle', true, 2, '◯'),
('iqo00000-0011-0004-0000-000000000003', 'iq000000-0000-0000-0011-000000000004', 'Triangle', true, 3, '△'),
('iqo00000-0011-0004-0000-000000000004', 'iq000000-0000-0000-0011-000000000004', 'Triangle', true, 4, '△'),
('iqo00000-0011-0004-0000-000000000005', 'iq000000-0000-0000-0011-000000000004', 'Square', true, 5, '□'),
('iqo00000-0011-0004-0000-000000000006', 'iq000000-0000-0000-0011-000000000004', 'Square', true, 6, '□'),
('iqo00000-0011-0004-0000-000000000007', 'iq000000-0000-0000-0011-000000000004', 'Star', true, 7, '⭐'),
('iqo00000-0011-0004-0000-000000000008', 'iq000000-0000-0000-0011-000000000004', 'Star', true, 8, '⭐'),
('iqo00000-0011-0005-0000-000000000001', 'iq000000-0000-0000-0011-000000000005', 'Dog', true, 1, 'Bark'),
('iqo00000-0011-0005-0000-000000000002', 'iq000000-0000-0000-0011-000000000005', 'Dog', true, 2, 'Bark'),
('iqo00000-0011-0005-0000-000000000003', 'iq000000-0000-0000-0011-000000000005', 'Cat', true, 3, 'Meow'),
('iqo00000-0011-0005-0000-000000000004', 'iq000000-0000-0000-0011-000000000005', 'Cat', true, 4, 'Meow'),
('iqo00000-0011-0005-0000-000000000005', 'iq000000-0000-0000-0011-000000000005', 'Lion', true, 5, 'Roar'),
('iqo00000-0011-0005-0000-000000000006', 'iq000000-0000-0000-0011-000000000005', 'Lion', true, 6, 'Roar'),
('iqo00000-0011-0005-0000-000000000007', 'iq000000-0000-0000-0011-000000000005', 'Bird', true, 7, 'Tweet'),
('iqo00000-0011-0005-0000-000000000008', 'iq000000-0000-0000-0011-000000000005', 'Bird', true, 8, 'Tweet')
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 12: VISUAL_SINGLE_CHOICE (Questions 56-60)
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status) VALUES
('iq000000-0000-0000-0012-000000000001', 'c1000000-0000-0000-0000-000000000099', 'visual_single_choice', 'Which image shows a triangle?', 'Select the correct shape', 'A triangle has 3 sides and 3 angles', 'easy', 56, 'published'),
('iq000000-0000-0000-0012-000000000002', 'c1000000-0000-0000-0000-000000000099', 'visual_single_choice', 'Which shows an even number of objects?', 'Count and select', 'Even numbers can be divided by 2 with no remainder', 'easy', 57, 'published'),
('iq000000-0000-0000-0012-000000000003', 'c1000000-0000-0000-0000-000000000099', 'visual_single_choice', 'Which is the largest animal?', 'Select the biggest one', 'Elephant is the largest land animal', 'easy', 58, 'published'),
('iq000000-0000-0000-0012-000000000004', 'c1000000-0000-0000-0000-000000000099', 'visual_single_choice', 'Which shows a symmetrical pattern?', 'Find the mirror image', 'Symmetry means both halves are identical mirrors', 'medium', 59, 'published'),
('iq000000-0000-0000-0012-000000000005', 'c1000000-0000-0000-0000-000000000099', 'visual_single_choice', 'Which clock shows 3 o''clock?', 'Select the correct clock', 'At 3:00 the hour hand points at 3, minute hand at 12', 'easy', 60, 'published')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order, visual_label) VALUES
('iqo00000-0012-0001-0000-000000000001', 'iq000000-0000-0000-0012-000000000001', 'Shape A', false, 1, '◯ Circle'),
('iqo00000-0012-0001-0000-000000000002', 'iq000000-0000-0000-0012-000000000001', 'Shape B', true, 2, '△ Triangle'),
('iqo00000-0012-0001-0000-000000000003', 'iq000000-0000-0000-0012-000000000001', 'Shape C', false, 3, '□ Square'),
('iqo00000-0012-0001-0000-000000000004', 'iq000000-0000-0000-0012-000000000001', 'Shape D', false, 4, '⬡ Hexagon'),
('iqo00000-0012-0002-0000-000000000001', 'iq000000-0000-0000-0012-000000000002', 'Group A', false, 1, '⭐⭐⭐ (3 stars)'),
('iqo00000-0012-0002-0000-000000000002', 'iq000000-0000-0000-0012-000000000002', 'Group B', true, 2, '⭐⭐⭐⭐ (4 stars)'),
('iqo00000-0012-0002-0000-000000000003', 'iq000000-0000-0000-0012-000000000002', 'Group C', false, 3, '⭐⭐⭐⭐⭐ (5 stars)'),
('iqo00000-0012-0002-0000-000000000004', 'iq000000-0000-0000-0012-000000000002', 'Group D', false, 4, '⭐ (1 star)'),
('iqo00000-0012-0003-0000-000000000001', 'iq000000-0000-0000-0012-000000000003', 'Animal A', false, 1, '🐱 Cat'),
('iqo00000-0012-0003-0000-000000000002', 'iq000000-0000-0000-0012-000000000003', 'Animal B', false, 2, '🐕 Dog'),
('iqo00000-0012-0003-0000-000000000003', 'iq000000-0000-0000-0012-000000000003', 'Animal C', true, 3, '🐘 Elephant'),
('iqo00000-0012-0003-0000-000000000004', 'iq000000-0000-0000-0012-000000000003', 'Animal D', false, 4, '🐇 Rabbit'),
('iqo00000-0012-0004-0000-000000000001', 'iq000000-0000-0000-0012-000000000004', 'Pattern A', false, 1, '⬆⬇⬆⬅ (asymmetric)'),
('iqo00000-0012-0004-0000-000000000002', 'iq000000-0000-0000-0012-000000000004', 'Pattern B', true, 2, '⬆⬇⬇⬆ (symmetric)'),
('iqo00000-0012-0004-0000-000000000003', 'iq000000-0000-0000-0012-000000000004', 'Pattern C', false, 3, '➡⬅⬆⬇ (asymmetric)'),
('iqo00000-0012-0004-0000-000000000004', 'iq000000-0000-0000-0012-000000000004', 'Pattern D', false, 4, '⬆➡⬇⬅ (asymmetric)'),
('iqo00000-0012-0005-0000-000000000001', 'iq000000-0000-0000-0012-000000000005', 'Clock A', false, 1, '🕐 1:00'),
('iqo00000-0012-0005-0000-000000000002', 'iq000000-0000-0000-0012-000000000005', 'Clock B', false, 2, '🕑 2:00'),
('iqo00000-0012-0005-0000-000000000003', 'iq000000-0000-0000-0012-000000000005', 'Clock C', true, 3, '🕒 3:00'),
('iqo00000-0012-0005-0000-000000000004', 'iq000000-0000-0000-0012-000000000005', 'Clock D', false, 4, '🕓 4:00')
ON CONFLICT (id) DO NOTHING;


-- ═══════════════════════════════════════════════════════════════════════
-- DONE!
--
-- This file adds:
-- • 15 NEW CMS RPCs (content lists, ref data CRUD, media, audit, analytics, tournaments)
-- • 60 IQ questions covering ALL 12 question types:
--     1. multiple_choice   (5) — standard 4-option MCQ
--     2. true_false         (5) — 2-option true/false
--     3. ordering           (5) — options with correct_position
--     4. match              (5) — match_pairs table entries
--     5. fill_blank         (5) — fill-in with 4 choices
--     6. select_word        (5) — pick the right word
--     7. matrix             (5) — prompt_config with matrix grid
--     8. grid_pattern       (5) — prompt_config with grid
--     9. statement_reason   (5) — metadata with statement+reason
--    10. table_data         (5) — prompt_config with table headers+rows
--    11. memory             (5) — paired options with visual_label
--    12. visual_single_choice (5) — options with visual_label
--
-- Total CMS RPCs: 15 (original) + 15 (this file) = 30 admin RPCs
-- Total App RPCs: 15 (unchanged)
-- Total: 45 RPC functions
-- ═══════════════════════════════════════════════════════════════════════
