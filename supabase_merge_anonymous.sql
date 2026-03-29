-- ╔══════════════════════════════════════════════════════════════════╗
-- ║  MERGE ANONYMOUS USER → GOOGLE USER                            ║
-- ║                                                                 ║
-- ║  Child tables (from actual schema dump):                        ║
-- ║    1. quiz_attempts       (user_id FK, ON DELETE CASCADE)       ║
-- ║    2. tournament_entries   (user_id FK, ON DELETE CASCADE)       ║
-- ║    3. user_module_progress (user_id FK, ON DELETE CASCADE)       ║
-- ║                                                                 ║
-- ║  Run this in the Supabase SQL Editor (Dashboard → SQL Editor)  ║
-- ╚══════════════════════════════════════════════════════════════════╝

DROP FUNCTION IF EXISTS public.merge_anonymous_to_google(UUID, UUID);
DROP FUNCTION IF EXISTS public.merge_anonymous_to_google(UUID, UUID, TEXT);
DROP FUNCTION IF EXISTS public.link_google_to_anonymous(UUID, UUID, TEXT);

CREATE OR REPLACE FUNCTION public.merge_anonymous_to_google(
    p_anon_id UUID,
    p_google_id UUID,
    p_email TEXT DEFAULT NULL
) RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER
AS $$
BEGIN
    -- Validate anonymous user exists
    IF NOT EXISTS (SELECT 1 FROM public.users WHERE id = p_anon_id) THEN
        RAISE EXCEPTION 'Anonymous user not found: %', p_anon_id;
    END IF;

    -- 1. Transfer child rows BEFORE deleting anonymous row (prevents CASCADE loss)
    UPDATE public.quiz_attempts SET user_id = p_google_id WHERE user_id = p_anon_id;

    UPDATE public.tournament_entries SET user_id = p_google_id
        WHERE user_id = p_anon_id
        AND tournament_id NOT IN (SELECT tournament_id FROM public.tournament_entries WHERE user_id = p_google_id);

    UPDATE public.user_module_progress SET user_id = p_google_id
        WHERE user_id = p_anon_id
        AND module_id NOT IN (SELECT module_id FROM public.user_module_progress WHERE user_id = p_google_id);

    -- 2. Clone anonymous profile → Google user row (all columns from actual schema)
    INSERT INTO public.users (
        id, display_name, avatar_id, grade_id, auth_provider,
        country_id, city_id, school_name, google_sub,
        total_xp, level, streak_current, streak_best,
        iq_best_score, is_banned, banned_reason, banned_at,
        last_active_at, created_at, updated_at,
        email, phone, is_email_verified, is_phone_verified
    )
    SELECT
        p_google_id, display_name, avatar_id, grade_id, 'google',
        country_id, city_id, school_name, google_sub,
        total_xp, level, streak_current, streak_best,
        iq_best_score, is_banned, banned_reason, banned_at,
        now(), created_at, now(),
        COALESCE(p_email, email), phone, true, is_phone_verified
    FROM public.users WHERE id = p_anon_id
    ON CONFLICT (id) DO UPDATE SET
        auth_provider  = 'google',
        email          = COALESCE(p_email, EXCLUDED.email),
        is_email_verified = true,
        display_name   = CASE WHEN public.users.display_name = '' THEN EXCLUDED.display_name
                         ELSE public.users.display_name END,
        total_xp       = public.users.total_xp + EXCLUDED.total_xp,
        level          = GREATEST(public.users.level, EXCLUDED.level),
        streak_current = GREATEST(public.users.streak_current, EXCLUDED.streak_current),
        streak_best    = GREATEST(public.users.streak_best, EXCLUDED.streak_best),
        iq_best_score  = GREATEST(public.users.iq_best_score, EXCLUDED.iq_best_score),
        updated_at     = now();

    -- 3. Delete orphaned anonymous row (child data already moved)
    DELETE FROM public.users WHERE id = p_anon_id;

    RETURN json_build_object('success', true, 'anon_id', p_anon_id, 'google_id', p_google_id);
END;
$$;
