-- ============================================================================
-- MindQuest Phase 3 Fixes: Performance Optimizations
-- ============================================================================
--
-- This migration introduces two major performance improvements:
--
-- 1. MATERIALIZED VIEWS FOR LEADERBOARD RANKINGS
--    Replaces expensive COUNT(*) full-table scans in get_leaderboard() with
--    pre-computed rank lookups via materialized views. Rank retrieval goes
--    from O(n) to O(1) per lookup.
--
-- 2. PRE-COMPUTED MODULE PROGRESS TABLE
--    Caches aggregate progress data (chapters done, quizzes done, XP earned,
--    best accuracy) per user per module. Automatically refreshed via a trigger
--    on quiz_attempts INSERT.
--
-- IMPORTANT: You must set up a cron job (pg_cron or Supabase Edge Function)
-- to call refresh_leaderboard_views() every 5 minutes. Example with pg_cron:
--
--   SELECT cron.schedule(
--       'refresh-leaderboard',
--       '*/5 * * * *',
--       $$SELECT public.refresh_leaderboard_views()$$
--   );
--
-- All statements are idempotent and safe to run multiple times
-- (IF NOT EXISTS, CREATE OR REPLACE, DROP IF EXISTS before CREATE).
-- ============================================================================


-- ############################################################################
-- SECTION 1: MATERIALIZED VIEWS FOR LEADERBOARD RANKINGS
-- ############################################################################

-- ----------------------------------------------------------------------------
-- 1a. Global leaderboard ranking
-- ----------------------------------------------------------------------------
CREATE MATERIALIZED VIEW IF NOT EXISTS public.mv_leaderboard_global AS
SELECT
    id AS user_id,
    display_name,
    avatar_id,
    total_xp,
    country_id,
    city_id,
    RANK() OVER (ORDER BY total_xp DESC, created_at ASC)::int AS rank_global
FROM public.users
WHERE NOT is_banned
WITH DATA;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_lb_global_user ON public.mv_leaderboard_global(user_id);
CREATE INDEX IF NOT EXISTS idx_mv_lb_global_rank ON public.mv_leaderboard_global(rank_global);

-- ----------------------------------------------------------------------------
-- 1b. Country leaderboard ranking
-- ----------------------------------------------------------------------------
CREATE MATERIALIZED VIEW IF NOT EXISTS public.mv_leaderboard_country AS
SELECT
    id AS user_id,
    country_id,
    total_xp,
    RANK() OVER (PARTITION BY country_id ORDER BY total_xp DESC, created_at ASC)::int AS rank_country
FROM public.users
WHERE NOT is_banned AND country_id IS NOT NULL
WITH DATA;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_lb_country_user ON public.mv_leaderboard_country(user_id);
CREATE INDEX IF NOT EXISTS idx_mv_lb_country_rank ON public.mv_leaderboard_country(country_id, rank_country);

-- ----------------------------------------------------------------------------
-- 1c. City leaderboard ranking
-- ----------------------------------------------------------------------------
CREATE MATERIALIZED VIEW IF NOT EXISTS public.mv_leaderboard_city AS
SELECT
    id AS user_id,
    city_id,
    total_xp,
    RANK() OVER (PARTITION BY city_id ORDER BY total_xp DESC, created_at ASC)::int AS rank_city
FROM public.users
WHERE NOT is_banned AND city_id IS NOT NULL
WITH DATA;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_lb_city_user ON public.mv_leaderboard_city(user_id);
CREATE INDEX IF NOT EXISTS idx_mv_lb_city_rank ON public.mv_leaderboard_city(city_id, rank_city);

-- ----------------------------------------------------------------------------
-- 1d. Refresh function (call via cron every 5 minutes)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.refresh_leaderboard_views()
RETURNS void LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY public.mv_leaderboard_global;
    REFRESH MATERIALIZED VIEW CONCURRENTLY public.mv_leaderboard_country;
    REFRESH MATERIALIZED VIEW CONCURRENTLY public.mv_leaderboard_city;
END; $$;

-- ----------------------------------------------------------------------------
-- 1e. Rewritten get_leaderboard using materialized views for rank lookups
-- ----------------------------------------------------------------------------
DROP FUNCTION IF EXISTS public.get_leaderboard(UUID, TEXT, UUID, INT, INT);

CREATE OR REPLACE FUNCTION public.get_leaderboard(
    p_user_id UUID, p_filter TEXT DEFAULT 'global', p_filter_id UUID DEFAULT NULL,
    p_limit INT DEFAULT 50, p_offset INT DEFAULT 0
) RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_ranked JSON; v_country UUID; v_city UUID;
    v_rank_g INT; v_rank_c INT; v_rank_ci INT; v_xp_gap BIGINT;
BEGIN
    SELECT country_id, city_id INTO v_country, v_city
    FROM public.users WHERE id = p_user_id;

    -- Ranked list from the main users table (paginated)
    SELECT COALESCE(json_agg(row_to_json(r)), '[]'::json) INTO v_ranked
    FROM (
        SELECT u.id AS user_id, u.display_name, u.avatar_id, u.total_xp,
            ROW_NUMBER() OVER (ORDER BY u.total_xp DESC, u.created_at ASC)::int AS rank
        FROM public.users u
        WHERE NOT u.is_banned AND CASE
            WHEN p_filter='country' THEN u.country_id = COALESCE(p_filter_id, v_country)
            WHEN p_filter='city'    THEN u.city_id    = COALESCE(p_filter_id, v_city)
            WHEN p_filter='school'  THEN u.school_name IS NOT NULL
                AND u.school_name = (SELECT school_name FROM public.users WHERE id = p_user_id)
            ELSE true END
        ORDER BY u.total_xp DESC, u.created_at ASC LIMIT p_limit OFFSET p_offset
    ) r;

    -- Use materialized views for fast rank lookups (O(1) instead of O(n))
    SELECT rank_global INTO v_rank_g FROM public.mv_leaderboard_global WHERE user_id = p_user_id;
    SELECT rank_country INTO v_rank_c FROM public.mv_leaderboard_country WHERE user_id = p_user_id;
    SELECT rank_city INTO v_rank_ci FROM public.mv_leaderboard_city WHERE user_id = p_user_id;

    -- XP gap to next rank (still from main table but uses index)
    SELECT total_xp - (SELECT total_xp FROM public.users WHERE id = p_user_id)
    INTO v_xp_gap
    FROM public.users
    WHERE total_xp > (SELECT total_xp FROM public.users WHERE id = p_user_id)
      AND NOT is_banned
    ORDER BY total_xp ASC LIMIT 1;

    RETURN json_build_object('ranked_users', v_ranked,
        'user_rank', json_build_object('rank_global', COALESCE(v_rank_g, 0), 'rank_country', v_rank_c, 'rank_city', v_rank_ci, 'xp_gap', COALESCE(v_xp_gap, 0)));
END; $$;

GRANT EXECUTE ON FUNCTION public.get_leaderboard(UUID, TEXT, UUID, INT, INT) TO anon, authenticated;


-- ############################################################################
-- SECTION 2: PRE-COMPUTED MODULE PROGRESS TABLE
-- ############################################################################

-- ----------------------------------------------------------------------------
-- 2a. Module progress cache table
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.user_module_progress (
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    module_id UUID NOT NULL REFERENCES public.modules(id) ON DELETE CASCADE,
    chapters_total INT NOT NULL DEFAULT 0,
    chapters_done INT NOT NULL DEFAULT 0,
    quizzes_total INT NOT NULL DEFAULT 0,
    quizzes_done INT NOT NULL DEFAULT 0,
    total_xp_earned INT NOT NULL DEFAULT 0,
    best_accuracy NUMERIC(5,2) DEFAULT NULL,
    last_updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, module_id)
);

CREATE INDEX IF NOT EXISTS idx_user_module_progress_user ON public.user_module_progress(user_id);

-- ----------------------------------------------------------------------------
-- 2b. Function to refresh a single user's module progress
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.refresh_user_module_progress(p_user_id UUID, p_module_id UUID)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_chapters_total INT;
    v_chapters_done INT;
    v_quizzes_total INT;
    v_quizzes_done INT;
    v_total_xp INT;
    v_best_accuracy NUMERIC(5,2);
BEGIN
    -- Count total chapters in this module
    SELECT COUNT(*) INTO v_chapters_total
    FROM public.chapters WHERE module_id = p_module_id AND is_active;

    -- Count total quizzes across all chapters in this module
    SELECT COUNT(*) INTO v_quizzes_total
    FROM public.quizzes q
    JOIN public.chapters c ON c.id = q.chapter_id
    WHERE c.module_id = p_module_id AND q.is_active AND c.is_active;

    -- Count quizzes the user has attempted
    SELECT COUNT(DISTINCT qa.quiz_id) INTO v_quizzes_done
    FROM public.quiz_attempts qa
    JOIN public.quizzes q ON q.id = qa.quiz_id
    JOIN public.chapters c ON c.id = q.chapter_id
    WHERE qa.user_id = p_user_id AND c.module_id = p_module_id AND q.is_active AND c.is_active;

    -- Count chapters where all quizzes are done
    SELECT COUNT(*) INTO v_chapters_done
    FROM public.chapters c
    WHERE c.module_id = p_module_id AND c.is_active
      AND NOT EXISTS (
          SELECT 1 FROM public.quizzes q
          WHERE q.chapter_id = c.id AND q.is_active
            AND NOT EXISTS (
                SELECT 1 FROM public.quiz_attempts qa
                WHERE qa.quiz_id = q.id AND qa.user_id = p_user_id
            )
      );

    -- Total XP earned in this module
    SELECT COALESCE(SUM(qa.xp_earned), 0) INTO v_total_xp
    FROM public.quiz_attempts qa
    JOIN public.quizzes q ON q.id = qa.quiz_id
    JOIN public.chapters c ON c.id = q.chapter_id
    WHERE qa.user_id = p_user_id AND c.module_id = p_module_id;

    -- Best accuracy in this module
    SELECT ROUND(MAX(qa.score::numeric / NULLIF(qa.total_questions, 0) * 100), 1) INTO v_best_accuracy
    FROM public.quiz_attempts qa
    JOIN public.quizzes q ON q.id = qa.quiz_id
    JOIN public.chapters c ON c.id = q.chapter_id
    WHERE qa.user_id = p_user_id AND c.module_id = p_module_id;

    -- Upsert into the cache table
    INSERT INTO public.user_module_progress (
        user_id, module_id, chapters_total, chapters_done,
        quizzes_total, quizzes_done, total_xp_earned, best_accuracy, last_updated_at
    ) VALUES (
        p_user_id, p_module_id, v_chapters_total, v_chapters_done,
        v_quizzes_total, v_quizzes_done, v_total_xp, v_best_accuracy, now()
    )
    ON CONFLICT (user_id, module_id) DO UPDATE SET
        chapters_total = v_chapters_total,
        chapters_done = v_chapters_done,
        quizzes_total = v_quizzes_total,
        quizzes_done = v_quizzes_done,
        total_xp_earned = v_total_xp,
        best_accuracy = v_best_accuracy,
        last_updated_at = now();
END; $$;

GRANT EXECUTE ON FUNCTION public.refresh_user_module_progress(UUID, UUID) TO authenticated;

-- ----------------------------------------------------------------------------
-- 2c. Trigger to auto-refresh module progress after quiz submission
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.trigger_refresh_module_progress()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_module_id UUID;
BEGIN
    -- Find the module_id for this quiz
    SELECT c.module_id INTO v_module_id
    FROM public.quizzes q
    JOIN public.chapters c ON c.id = q.chapter_id
    WHERE q.id = NEW.quiz_id;

    -- Refresh the module progress cache
    IF v_module_id IS NOT NULL THEN
        PERFORM public.refresh_user_module_progress(NEW.user_id, v_module_id);
    END IF;

    RETURN NEW;
END; $$;

-- Drop existing trigger if any, then create
DROP TRIGGER IF EXISTS trg_refresh_module_progress ON public.quiz_attempts;
CREATE TRIGGER trg_refresh_module_progress
    AFTER INSERT ON public.quiz_attempts
    FOR EACH ROW
    EXECUTE FUNCTION public.trigger_refresh_module_progress();
