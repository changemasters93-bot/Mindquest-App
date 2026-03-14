-- ╔══════════════════════════════════════════════════════════════════╗
-- ║  MINDQUEST V2 — COMPLETE SUPABASE SETUP                        ║
-- ║  Run this in the Supabase SQL Editor (Dashboard → SQL Editor)  ║
-- ║  This script is IDEMPOTENT — safe to re-run.                   ║
-- ╚══════════════════════════════════════════════════════════════════╝

-- ═══════════════════════════════════════════════════════════════════
-- 0. EXTENSIONS
-- ═══════════════════════════════════════════════════════════════════
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ═══════════════════════════════════════════════════════════════════
-- 1. REFERENCE TABLES (grades, countries, cities)
-- ═══════════════════════════════════════════════════════════════════

-- ── grades ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.grades (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code       TEXT    NOT NULL UNIQUE,       -- e.g. "G1", "G2"
    label      TEXT    NOT NULL,              -- e.g. "Grade 1", "Grade 2"
    sort_order INT     NOT NULL DEFAULT 0
);

-- Seed grades (skip if they exist)
INSERT INTO public.grades (code, label, sort_order) VALUES
    ('G1', 'Grade 1', 1),
    ('G2', 'Grade 2', 2),
    ('G3', 'Grade 3', 3),
    ('G4', 'Grade 4', 4),
    ('G5', 'Grade 5', 5),
    ('G6', 'Grade 6', 6),
    ('G7', 'Grade 7', 7),
    ('G8', 'Grade 8', 8)
ON CONFLICT (code) DO NOTHING;

-- ── countries ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.countries (
    id        UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name      TEXT    NOT NULL,
    code      TEXT    NOT NULL UNIQUE,        -- ISO 3166-1 alpha-2
    is_active BOOLEAN NOT NULL DEFAULT true
);

INSERT INTO public.countries (name, code) VALUES
    ('India',          'IN'),
    ('United States',  'US'),
    ('United Kingdom', 'GB'),
    ('Canada',         'CA'),
    ('Australia',      'AU'),
    ('Singapore',      'SG'),
    ('UAE',            'AE')
ON CONFLICT (code) DO NOTHING;

-- ── cities ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.cities (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    country_id UUID NOT NULL REFERENCES public.countries(id),
    name       TEXT NOT NULL,
    UNIQUE (country_id, name)
);

-- Seed cities (India)
INSERT INTO public.cities (country_id, name)
SELECT c.id, city.name
FROM public.countries c,
     (VALUES ('Delhi'),('Mumbai'),('Bangalore'),('Chennai'),('Hyderabad'),('Pune'),('Kolkata')) AS city(name)
WHERE c.code = 'IN'
ON CONFLICT (country_id, name) DO NOTHING;

-- Seed cities (US)
INSERT INTO public.cities (country_id, name)
SELECT c.id, city.name
FROM public.countries c,
     (VALUES ('New York'),('Los Angeles'),('Chicago'),('Houston'),('Phoenix')) AS city(name)
WHERE c.code = 'US'
ON CONFLICT (country_id, name) DO NOTHING;

-- Seed cities (UK)
INSERT INTO public.cities (country_id, name)
SELECT c.id, city.name
FROM public.countries c,
     (VALUES ('London'),('Manchester'),('Birmingham'),('Edinburgh')) AS city(name)
WHERE c.code = 'GB'
ON CONFLICT (country_id, name) DO NOTHING;

-- Seed cities (Canada)
INSERT INTO public.cities (country_id, name)
SELECT c.id, city.name
FROM public.countries c,
     (VALUES ('Toronto'),('Vancouver'),('Montreal'),('Ottawa')) AS city(name)
WHERE c.code = 'CA'
ON CONFLICT (country_id, name) DO NOTHING;

-- Seed cities (Australia)
INSERT INTO public.cities (country_id, name)
SELECT c.id, city.name
FROM public.countries c,
     (VALUES ('Sydney'),('Melbourne'),('Brisbane'),('Perth')) AS city(name)
WHERE c.code = 'AU'
ON CONFLICT (country_id, name) DO NOTHING;

-- Seed cities (Singapore)
INSERT INTO public.cities (country_id, name)
SELECT c.id, city.name
FROM public.countries c,
     (VALUES ('Singapore')) AS city(name)
WHERE c.code = 'SG'
ON CONFLICT (country_id, name) DO NOTHING;

-- Seed cities (UAE)
INSERT INTO public.cities (country_id, name)
SELECT c.id, city.name
FROM public.countries c,
     (VALUES ('Dubai'),('Abu Dhabi'),('Sharjah')) AS city(name)
WHERE c.code = 'AE'
ON CONFLICT (country_id, name) DO NOTHING;


-- ═══════════════════════════════════════════════════════════════════
-- 2. USERS TABLE
-- ═══════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS public.users (
    id            UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name  TEXT    NOT NULL,
    avatar_id     INT     NOT NULL DEFAULT 4,
    grade_id      UUID    NOT NULL REFERENCES public.grades(id),
    auth_provider TEXT    NOT NULL DEFAULT 'anonymous'
                  CHECK (auth_provider IN ('google','phone','anonymous','google_and_phone')),
    country_id    UUID    REFERENCES public.countries(id),
    city_id       UUID    REFERENCES public.cities(id),
    school_name   TEXT,
    google_sub    TEXT,
    total_xp      BIGINT  NOT NULL DEFAULT 0,
    level         INT     NOT NULL DEFAULT 1,
    streak_current INT    NOT NULL DEFAULT 0,
    streak_best   INT     NOT NULL DEFAULT 0,
    last_active_at TIMESTAMPTZ DEFAULT now(),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- If auth_provider column exists but CHECK constraint is wrong, fix it:
DO $$
BEGIN
    -- Drop old constraint if it exists, then re-add
    ALTER TABLE public.users DROP CONSTRAINT IF EXISTS users_auth_provider_check;
    ALTER TABLE public.users ADD CONSTRAINT users_auth_provider_check
        CHECK (auth_provider IN ('google','phone','anonymous','google_and_phone'));
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- Add google_sub column if missing
DO $$
BEGIN
    ALTER TABLE public.users ADD COLUMN IF NOT EXISTS google_sub TEXT;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;


-- ═══════════════════════════════════════════════════════════════════
-- 3. MODULES TABLE
-- ═══════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS public.modules (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title        TEXT NOT NULL,
    subtitle     TEXT,
    emoji        TEXT NOT NULL DEFAULT '📚',
    accent_color TEXT NOT NULL DEFAULT '#4F46E5',
    display_order INT NOT NULL DEFAULT 0,
    grade_id     UUID REFERENCES public.grades(id),
    is_active    BOOLEAN NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ═══════════════════════════════════════════════════════════════════
-- 4. CHAPTERS TABLE
-- ═══════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS public.chapters (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    module_id      UUID NOT NULL REFERENCES public.modules(id) ON DELETE CASCADE,
    title          TEXT NOT NULL,
    chapter_number INT  NOT NULL DEFAULT 1,
    sort_order     INT  NOT NULL DEFAULT 0,
    is_active      BOOLEAN NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ═══════════════════════════════════════════════════════════════════
-- 5. QUIZZES TABLE
-- ═══════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS public.quizzes (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    chapter_id      UUID NOT NULL REFERENCES public.chapters(id) ON DELETE CASCADE,
    title           TEXT NOT NULL,
    quiz_type       TEXT NOT NULL DEFAULT 'practice'
                    CHECK (quiz_type IN ('practice','iq','tournament','daily')),
    question_count  INT  NOT NULL DEFAULT 0,
    time_limit_secs INT  NOT NULL DEFAULT 300,
    max_xp          INT  NOT NULL DEFAULT 100,
    difficulty      TEXT CHECK (difficulty IN ('easy','medium','hard')),
    sort_order      INT  NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ═══════════════════════════════════════════════════════════════════
-- 6. QUESTIONS TABLE
-- ═══════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS public.questions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quiz_id         UUID NOT NULL REFERENCES public.quizzes(id) ON DELETE CASCADE,
    question_type   TEXT NOT NULL DEFAULT 'multiple_choice'
                    CHECK (question_type IN (
                        'multiple_choice','true_false','ordering','match',
                        'fill_blank','select_word','matrix','grid_pattern',
                        'statement_reason','table_data','memory','visual_single_choice'
                    )),
    title           TEXT NOT NULL,
    prompt          TEXT,
    explanation     TEXT NOT NULL DEFAULT '',
    difficulty      TEXT CHECK (difficulty IN ('easy','medium','hard')),
    time_limit_secs INT,
    allow_multiple  BOOLEAN NOT NULL DEFAULT false,
    prompt_config   JSONB,
    metadata        JSONB,
    media_url       TEXT,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ═══════════════════════════════════════════════════════════════════
-- 7. QUESTION OPTIONS TABLE
-- ═══════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS public.question_options (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    question_id      UUID NOT NULL REFERENCES public.questions(id) ON DELETE CASCADE,
    label            TEXT NOT NULL,
    is_correct       BOOLEAN NOT NULL DEFAULT false,
    sort_order       INT NOT NULL DEFAULT 0,
    correct_position INT,
    media_url        TEXT,
    visual_label     TEXT
);

-- ═══════════════════════════════════════════════════════════════════
-- 8. MATCH PAIRS TABLE (for match-type questions)
-- ═══════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS public.match_pairs (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    question_id UUID NOT NULL REFERENCES public.questions(id) ON DELETE CASCADE,
    left_text   TEXT NOT NULL,
    right_text  TEXT NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0
);

-- ═══════════════════════════════════════════════════════════════════
-- 9. QUIZ ATTEMPTS TABLE
-- ═══════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS public.quiz_attempts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    quiz_id         UUID NOT NULL REFERENCES public.quizzes(id) ON DELETE CASCADE,
    score           INT  NOT NULL DEFAULT 0,
    total_questions INT  NOT NULL DEFAULT 0,
    xp_earned       INT  NOT NULL DEFAULT 0,
    time_taken_secs INT  NOT NULL DEFAULT 0,
    answers         JSONB NOT NULL DEFAULT '[]'::jsonb,
    idempotency_key TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, idempotency_key)
);

-- ═══════════════════════════════════════════════════════════════════
-- 10. DAILY CHALLENGES TABLE
-- ═══════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS public.daily_challenges (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quiz_id     UUID NOT NULL REFERENCES public.quizzes(id) ON DELETE CASCADE,
    grade_id    UUID NOT NULL REFERENCES public.grades(id),
    challenge_date DATE NOT NULL DEFAULT CURRENT_DATE,
    is_active   BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (quiz_id, challenge_date)
);

-- ═══════════════════════════════════════════════════════════════════
-- 11. TOURNAMENTS TABLE
-- ═══════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS public.tournaments (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title             TEXT NOT NULL,
    grade_id          UUID NOT NULL REFERENCES public.grades(id),
    question_count    INT NOT NULL DEFAULT 10,
    time_limit_seconds INT NOT NULL DEFAULT 600,
    starts_at         TIMESTAMPTZ NOT NULL,
    ends_at           TIMESTAMPTZ NOT NULL,
    status            TEXT NOT NULL DEFAULT 'scheduled'
                      CHECK (status IN ('draft','scheduled','live','closed','finalized')),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ═══════════════════════════════════════════════════════════════════
-- 12. TOURNAMENT QUESTIONS TABLE
-- ═══════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS public.tournament_questions (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tournament_id UUID NOT NULL REFERENCES public.tournaments(id) ON DELETE CASCADE,
    question_id   UUID NOT NULL REFERENCES public.questions(id) ON DELETE CASCADE,
    sort_order    INT NOT NULL DEFAULT 0,
    UNIQUE (tournament_id, question_id)
);

-- ═══════════════════════════════════════════════════════════════════
-- 13. TOURNAMENT ENTRIES TABLE
-- ═══════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS public.tournament_entries (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tournament_id   UUID NOT NULL REFERENCES public.tournaments(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    status          TEXT NOT NULL DEFAULT 'not_started'
                    CHECK (status IN ('not_started','in_progress','paused','completed','auto_submitted')),
    score           INT NOT NULL DEFAULT 0,
    time_taken_seconds INT,
    rank            INT,
    time_remaining_secs INT,
    answers_so_far  JSONB DEFAULT '[]'::jsonb,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tournament_id, user_id)
);

-- ═══════════════════════════════════════════════════════════════════
-- 14. USER STREAKS TABLE (optional, for streak tracking)
-- ═══════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS public.user_streaks (
    user_id    UUID PRIMARY KEY REFERENCES public.users(id) ON DELETE CASCADE,
    current    INT NOT NULL DEFAULT 0,
    best       INT NOT NULL DEFAULT 0,
    last_date  DATE
);


-- ═══════════════════════════════════════════════════════════════════
-- ═══════════════════════════════════════════════════════════════════
--  R O W   L E V E L   S E C U R I T Y   ( R L S )
-- ═══════════════════════════════════════════════════════════════════
-- ═══════════════════════════════════════════════════════════════════

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.grades ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.countries ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cities ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.modules ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chapters ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.quizzes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.question_options ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.match_pairs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.quiz_attempts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.daily_challenges ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tournaments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tournament_questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tournament_entries ENABLE ROW LEVEL SECURITY;

-- ── Reference tables: readable by all authenticated users ──────────
CREATE POLICY IF NOT EXISTS "grades_select" ON public.grades
    FOR SELECT TO authenticated USING (true);

CREATE POLICY IF NOT EXISTS "countries_select" ON public.countries
    FOR SELECT TO authenticated USING (true);

CREATE POLICY IF NOT EXISTS "cities_select" ON public.cities
    FOR SELECT TO authenticated USING (true);

-- ── Users: own row only for read/write ─────────────────────────────
CREATE POLICY IF NOT EXISTS "users_select_own" ON public.users
    FOR SELECT TO authenticated USING (auth.uid() = id);

CREATE POLICY IF NOT EXISTS "users_insert_own" ON public.users
    FOR INSERT TO authenticated WITH CHECK (auth.uid() = id);

CREATE POLICY IF NOT EXISTS "users_update_own" ON public.users
    FOR UPDATE TO authenticated USING (auth.uid() = id);

-- ── Content tables: readable by all authenticated ──────────────────
CREATE POLICY IF NOT EXISTS "modules_select" ON public.modules
    FOR SELECT TO authenticated USING (true);

CREATE POLICY IF NOT EXISTS "chapters_select" ON public.chapters
    FOR SELECT TO authenticated USING (true);

CREATE POLICY IF NOT EXISTS "quizzes_select" ON public.quizzes
    FOR SELECT TO authenticated USING (true);

CREATE POLICY IF NOT EXISTS "questions_select" ON public.questions
    FOR SELECT TO authenticated USING (true);

CREATE POLICY IF NOT EXISTS "options_select" ON public.question_options
    FOR SELECT TO authenticated USING (true);

CREATE POLICY IF NOT EXISTS "match_pairs_select" ON public.match_pairs
    FOR SELECT TO authenticated USING (true);

CREATE POLICY IF NOT EXISTS "daily_challenges_select" ON public.daily_challenges
    FOR SELECT TO authenticated USING (true);

CREATE POLICY IF NOT EXISTS "tournaments_select" ON public.tournaments
    FOR SELECT TO authenticated USING (true);

CREATE POLICY IF NOT EXISTS "tournament_questions_select" ON public.tournament_questions
    FOR SELECT TO authenticated USING (true);

-- ── Quiz attempts: own rows only ───────────────────────────────────
CREATE POLICY IF NOT EXISTS "attempts_select_own" ON public.quiz_attempts
    FOR SELECT TO authenticated USING (auth.uid() = user_id);

CREATE POLICY IF NOT EXISTS "attempts_insert_own" ON public.quiz_attempts
    FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

-- ── Tournament entries: own row for write, all for leaderboard read ─
CREATE POLICY IF NOT EXISTS "entries_select" ON public.tournament_entries
    FOR SELECT TO authenticated USING (true);

CREATE POLICY IF NOT EXISTS "entries_insert_own" ON public.tournament_entries
    FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

CREATE POLICY IF NOT EXISTS "entries_update_own" ON public.tournament_entries
    FOR UPDATE TO authenticated USING (auth.uid() = user_id);


-- ═══════════════════════════════════════════════════════════════════
-- ═══════════════════════════════════════════════════════════════════
--  R P C   F U N C T I O N S
-- ═══════════════════════════════════════════════════════════════════
-- ═══════════════════════════════════════════════════════════════════

-- ─────────────────────────────────────────────────────────────────
-- 1. get_user_dashboard(p_user_id UUID)
--    Returns: { user, stats, modules[], active_tournament? }
-- ─────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.get_user_dashboard(p_user_id UUID)
RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
    v_user   JSON;
    v_stats  JSON;
    v_mods   JSON;
    v_tourn  JSON;
    v_grade  UUID;
BEGIN
    -- User basic info
    SELECT json_build_object(
        'id',            u.id,
        'display_name',  u.display_name,
        'avatar_id',     u.avatar_id,
        'grade_id',      u.grade_id,
        'auth_provider', u.auth_provider
    ) INTO v_user
    FROM public.users u WHERE u.id = p_user_id;

    IF v_user IS NULL THEN
        RAISE EXCEPTION 'User not found: %', p_user_id;
    END IF;

    SELECT u.grade_id INTO v_grade FROM public.users u WHERE u.id = p_user_id;

    -- User stats
    SELECT json_build_object(
        'total_xp',           u.total_xp,
        'level',              u.level,
        'streak_current',     u.streak_current,
        'streak_best',        u.streak_best,
        'quizzes_completed',  (SELECT COUNT(*) FROM public.quiz_attempts WHERE user_id = p_user_id),
        'accuracy_pct',       COALESCE(
            (SELECT ROUND(AVG(score::numeric / NULLIF(total_questions,0) * 100), 1)
             FROM public.quiz_attempts WHERE user_id = p_user_id), 0
        ),
        'tournaments_played', (SELECT COUNT(*) FROM public.tournament_entries
                               WHERE user_id = p_user_id AND status IN ('completed','auto_submitted')),
        'best_tournament_rank', (SELECT MIN(rank) FROM public.tournament_entries
                                  WHERE user_id = p_user_id AND rank IS NOT NULL)
    ) INTO v_stats
    FROM public.users u WHERE u.id = p_user_id;

    -- Modules with progress
    SELECT COALESCE(json_agg(row_to_json(m_row) ORDER BY m_row.sort_order), '[]'::json)
    INTO v_mods
    FROM (
        SELECT
            m.id,
            m.title,
            m.subtitle,
            m.emoji,
            m.accent_color,
            m.display_order AS sort_order,
            (
                SELECT json_build_object(
                    'current_chapter_id', NULL,
                    'current_quiz_id',    NULL,
                    'best_score_pct',     (
                        SELECT MAX(ROUND(a.score::numeric / NULLIF(a.total_questions,0) * 100))::int
                        FROM public.quiz_attempts a
                        JOIN public.quizzes q2 ON q2.id = a.quiz_id
                        JOIN public.chapters ch2 ON ch2.id = q2.chapter_id
                        WHERE ch2.module_id = m.id AND a.user_id = p_user_id
                    ),
                    'is_completed', false
                )
            ) AS progress
        FROM public.modules m
        WHERE m.is_active = true
          AND (m.grade_id IS NULL OR m.grade_id = v_grade)
        ORDER BY m.display_order
    ) m_row;

    -- Active tournament
    SELECT json_build_object(
        'id',               t.id,
        'title',            t.title,
        'starts_at',        t.starts_at,
        'ends_at',          t.ends_at,
        'status',           t.status,
        'user_entry_status', (SELECT te.status FROM public.tournament_entries te
                              WHERE te.tournament_id = t.id AND te.user_id = p_user_id),
        'question_count',   t.question_count,
        'time_limit_seconds', t.time_limit_seconds,
        'participant_count', (SELECT COUNT(*) FROM public.tournament_entries te2
                              WHERE te2.tournament_id = t.id)
    ) INTO v_tourn
    FROM public.tournaments t
    WHERE t.grade_id = v_grade
      AND t.status IN ('live','scheduled')
      AND t.ends_at > now()
    ORDER BY t.starts_at
    LIMIT 1;

    -- Update last_active_at
    UPDATE public.users SET last_active_at = now() WHERE id = p_user_id;

    RETURN json_build_object(
        'user',              v_user,
        'stats',             v_stats,
        'modules',           v_mods,
        'active_tournament', v_tourn
    );
END;
$$;


-- ─────────────────────────────────────────────────────────────────
-- 2. get_daily_challenges(p_user_id UUID)
--    Returns: [{ quiz_id, title, description, question_count, ... }]
-- ─────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.get_daily_challenges(p_user_id UUID)
RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
    v_grade UUID;
    v_result JSON;
BEGIN
    SELECT grade_id INTO v_grade FROM public.users WHERE id = p_user_id;

    SELECT COALESCE(json_agg(row_to_json(r)), '[]'::json) INTO v_result
    FROM (
        SELECT
            q.id       AS quiz_id,
            q.title,
            NULL::text AS description,
            q.question_count,
            CEIL(q.time_limit_secs / 60.0)::int AS time_in_minutes,
            ch.module_id,
            q.chapter_id,
            EXISTS(
                SELECT 1 FROM public.quiz_attempts a
                WHERE a.quiz_id = q.id AND a.user_id = p_user_id
                  AND a.created_at::date = CURRENT_DATE
            ) AS is_done
        FROM public.daily_challenges dc
        JOIN public.quizzes q ON q.id = dc.quiz_id
        JOIN public.chapters ch ON ch.id = q.chapter_id
        WHERE dc.challenge_date = CURRENT_DATE
          AND dc.is_active = true
          AND dc.grade_id = v_grade
        ORDER BY q.sort_order
    ) r;

    RETURN v_result;
END;
$$;


-- ─────────────────────────────────────────────────────────────────
-- 3. get_module_full(p_module_id UUID, p_user_id UUID)
--    Returns: { module, chapters[] }
-- ─────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.get_module_full(p_module_id UUID, p_user_id UUID)
RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
    v_module  JSON;
    v_chapters JSON;
BEGIN
    -- Module info
    SELECT json_build_object(
        'id',            m.id,
        'title',         m.title,
        'subtitle',      m.subtitle,
        'emoji',         m.emoji,
        'accent_color',  m.accent_color,
        'display_order', m.display_order
    ) INTO v_module
    FROM public.modules m WHERE m.id = p_module_id;

    IF v_module IS NULL THEN
        RAISE EXCEPTION 'Module not found: %', p_module_id;
    END IF;

    -- Chapters with progress
    SELECT COALESCE(json_agg(row_to_json(ch_row) ORDER BY ch_row.chapter_number), '[]'::json)
    INTO v_chapters
    FROM (
        SELECT
            ch.id,
            ch.title,
            ch.chapter_number,
            (SELECT COUNT(*) FROM public.quizzes q WHERE q.chapter_id = ch.id AND q.is_active)::int AS quiz_count,
            json_build_object(
                'quizzes_done', (
                    SELECT COUNT(DISTINCT a.quiz_id)
                    FROM public.quiz_attempts a
                    JOIN public.quizzes q ON q.id = a.quiz_id
                    WHERE q.chapter_id = ch.id AND a.user_id = p_user_id
                )::int,
                'total_quizzes', (
                    SELECT COUNT(*) FROM public.quizzes q WHERE q.chapter_id = ch.id AND q.is_active
                )::int,
                'best_score_pct', (
                    SELECT MAX(ROUND(a.score::numeric / NULLIF(a.total_questions,0) * 100))::int
                    FROM public.quiz_attempts a
                    JOIN public.quizzes q ON q.id = a.quiz_id
                    WHERE q.chapter_id = ch.id AND a.user_id = p_user_id
                ),
                'is_completed', (
                    SELECT COUNT(DISTINCT a.quiz_id) >= COUNT(DISTINCT q2.id)
                    FROM public.quizzes q2
                    LEFT JOIN public.quiz_attempts a ON a.quiz_id = q2.id AND a.user_id = p_user_id
                    WHERE q2.chapter_id = ch.id AND q2.is_active
                )
            ) AS progress,
            CASE
                WHEN ch.chapter_number = 1 THEN 'unlocked'
                WHEN EXISTS(
                    SELECT 1 FROM public.chapters prev
                    WHERE prev.module_id = ch.module_id
                      AND prev.chapter_number = ch.chapter_number - 1
                      AND (
                          SELECT COUNT(DISTINCT a.quiz_id) >= COUNT(DISTINCT q3.id)
                          FROM public.quizzes q3
                          LEFT JOIN public.quiz_attempts a ON a.quiz_id = q3.id AND a.user_id = p_user_id
                          WHERE q3.chapter_id = prev.id AND q3.is_active
                      )
                ) THEN 'unlocked'
                ELSE 'locked'
            END AS state
        FROM public.chapters ch
        WHERE ch.module_id = p_module_id AND ch.is_active
        ORDER BY ch.chapter_number
    ) ch_row;

    RETURN json_build_object(
        'module',   v_module,
        'chapters', v_chapters
    );
END;
$$;


-- ─────────────────────────────────────────────────────────────────
-- 4. get_chapter_quizzes(p_chapter_id UUID, p_user_id UUID)
--    Returns: [{ id, title, quiz_type, questions[], ... }]
-- ─────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.get_chapter_quizzes(p_chapter_id UUID, p_user_id UUID)
RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
    v_result JSON;
BEGIN
    SELECT COALESCE(json_agg(quiz_json ORDER BY q.sort_order), '[]'::json) INTO v_result
    FROM public.quizzes q,
    LATERAL (
        SELECT json_build_object(
            'id',             q.id,
            'title',          q.title,
            'quiz_type',      q.quiz_type,
            'question_count', q.question_count,
            'time_limit_secs', q.time_limit_secs,
            'max_xp',         q.max_xp,
            'difficulty',     q.difficulty,
            'sort_order',     q.sort_order,
            'best_score',     (
                SELECT MAX(a.score) FROM public.quiz_attempts a
                WHERE a.quiz_id = q.id AND a.user_id = p_user_id
            ),
            'attempt_count',  (
                SELECT COUNT(*) FROM public.quiz_attempts a
                WHERE a.quiz_id = q.id AND a.user_id = p_user_id
            )::int,
            'questions',      (
                SELECT COALESCE(json_agg(
                    json_build_object(
                        'id',              qu.id,
                        'question_type',   qu.question_type,
                        'title',           qu.title,
                        'prompt',          qu.prompt,
                        'explanation',     qu.explanation,
                        'difficulty',      qu.difficulty,
                        'time_limit_secs', qu.time_limit_secs,
                        'allow_multiple',  qu.allow_multiple,
                        'prompt_config',   qu.prompt_config,
                        'metadata',        qu.metadata,
                        'media_url',       qu.media_url,
                        'options',         (
                            SELECT COALESCE(json_agg(
                                json_build_object(
                                    'id',               o.id,
                                    'label',            o.label,
                                    'is_correct',       o.is_correct,
                                    'sort_order',       o.sort_order,
                                    'correct_position', o.correct_position,
                                    'media_url',        o.media_url,
                                    'visual_label',     o.visual_label
                                ) ORDER BY o.sort_order
                            ), '[]'::json)
                            FROM public.question_options o WHERE o.question_id = qu.id
                        ),
                        'match_pairs',     (
                            SELECT json_agg(
                                json_build_object(
                                    'id',         mp.id,
                                    'left_text',  mp.left_text,
                                    'right_text', mp.right_text,
                                    'sort_order', mp.sort_order
                                ) ORDER BY mp.sort_order
                            )
                            FROM public.match_pairs mp WHERE mp.question_id = qu.id
                        )
                    ) ORDER BY qu.sort_order
                ), '[]'::json)
                FROM public.questions qu WHERE qu.quiz_id = q.id
            )
        ) AS quiz_json
    ) sub
    WHERE q.chapter_id = p_chapter_id AND q.is_active;

    RETURN v_result;
END;
$$;


-- ─────────────────────────────────────────────────────────────────
-- 5. submit_quiz_attempt(payload JSON)
--    payload: { user_id, quiz_id, time_taken_secs, idempotency_key,
--               answers: [{question_id, selected, is_correct, time_ms}] }
--    Returns: { status, attempt_id, score, total_questions, xp_earned,
--               total_xp, level, level_changed, rank_global, is_replay, next_quiz_id }
-- ─────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.submit_quiz_attempt(payload JSON)
RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
    v_user_id         UUID    := (payload->>'user_id')::uuid;
    v_quiz_id         UUID    := (payload->>'quiz_id')::uuid;
    v_time_taken      INT     := (payload->>'time_taken_secs')::int;
    v_idemp           TEXT    := payload->>'idempotency_key';
    v_answers         JSON    := payload->'answers';
    v_score           INT     := 0;
    v_total           INT     := 0;
    v_xp_earned       INT;
    v_max_xp          INT;
    v_is_replay       BOOLEAN := false;
    v_attempt_id      UUID;
    v_old_level       INT;
    v_new_level       INT;
    v_total_xp        BIGINT;
    v_rank            INT;
    v_next_quiz       UUID;
    v_chapter_id      UUID;
BEGIN
    -- Check for duplicate submission
    SELECT id INTO v_attempt_id
    FROM public.quiz_attempts
    WHERE user_id = v_user_id AND idempotency_key = v_idemp;

    IF v_attempt_id IS NOT NULL THEN
        -- Return existing result
        SELECT a.score, a.total_questions, a.xp_earned
        INTO v_score, v_total, v_xp_earned
        FROM public.quiz_attempts a WHERE a.id = v_attempt_id;

        SELECT total_xp, level INTO v_total_xp, v_new_level FROM public.users WHERE id = v_user_id;

        RETURN json_build_object(
            'status', 'duplicate',
            'attempt_id', v_attempt_id,
            'score', v_score, 'total_questions', v_total,
            'xp_earned', v_xp_earned, 'total_xp', v_total_xp,
            'level', v_new_level, 'level_changed', false,
            'rank_global', 0, 'is_replay', true, 'next_quiz_id', NULL
        );
    END IF;

    -- Count correct answers
    SELECT COUNT(*) INTO v_total FROM json_array_elements(v_answers);
    SELECT COUNT(*) INTO v_score
    FROM json_array_elements(v_answers) elem
    WHERE (elem->>'is_correct')::boolean = true;

    -- Check if replay
    SELECT EXISTS(
        SELECT 1 FROM public.quiz_attempts WHERE user_id = v_user_id AND quiz_id = v_quiz_id
    ) INTO v_is_replay;

    -- Calculate XP (proportional to score, only for first attempt)
    SELECT max_xp, chapter_id INTO v_max_xp, v_chapter_id FROM public.quizzes WHERE id = v_quiz_id;
    v_max_xp := COALESCE(v_max_xp, 100);
    IF v_is_replay THEN
        v_xp_earned := 0;
    ELSE
        v_xp_earned := ROUND(v_max_xp * (v_score::numeric / NULLIF(v_total, 0)));
    END IF;

    -- Insert attempt
    INSERT INTO public.quiz_attempts (user_id, quiz_id, score, total_questions, xp_earned, time_taken_secs, answers, idempotency_key)
    VALUES (v_user_id, v_quiz_id, v_score, v_total, v_xp_earned, v_time_taken, v_answers::jsonb, v_idemp)
    RETURNING id INTO v_attempt_id;

    -- Update user XP and level
    SELECT level INTO v_old_level FROM public.users WHERE id = v_user_id;
    UPDATE public.users
    SET total_xp = total_xp + v_xp_earned,
        level = CASE
            WHEN total_xp + v_xp_earned >= 5000 THEN 10
            WHEN total_xp + v_xp_earned >= 4000 THEN 9
            WHEN total_xp + v_xp_earned >= 3200 THEN 8
            WHEN total_xp + v_xp_earned >= 2500 THEN 7
            WHEN total_xp + v_xp_earned >= 1900 THEN 6
            WHEN total_xp + v_xp_earned >= 1400 THEN 5
            WHEN total_xp + v_xp_earned >= 1000 THEN 4
            WHEN total_xp + v_xp_earned >= 600  THEN 3
            WHEN total_xp + v_xp_earned >= 300  THEN 2
            ELSE 1
        END,
        last_active_at = now()
    WHERE id = v_user_id
    RETURNING total_xp, level INTO v_total_xp, v_new_level;

    -- Global rank by XP
    SELECT COUNT(*) + 1 INTO v_rank
    FROM public.users WHERE total_xp > v_total_xp;

    -- Next quiz in chapter
    SELECT q.id INTO v_next_quiz
    FROM public.quizzes q
    WHERE q.chapter_id = v_chapter_id
      AND q.sort_order > (SELECT sort_order FROM public.quizzes WHERE id = v_quiz_id)
      AND q.is_active
    ORDER BY q.sort_order
    LIMIT 1;

    RETURN json_build_object(
        'status',          'success',
        'attempt_id',      v_attempt_id,
        'score',           v_score,
        'total_questions', v_total,
        'xp_earned',       v_xp_earned,
        'total_xp',        v_total_xp,
        'level',           v_new_level,
        'level_changed',   (v_new_level > v_old_level),
        'rank_global',     v_rank,
        'is_replay',       v_is_replay,
        'next_quiz_id',    v_next_quiz
    );
END;
$$;


-- ─────────────────────────────────────────────────────────────────
-- 6. get_active_tournament(p_user_id UUID, p_grade_id UUID)
-- ─────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.get_active_tournament(p_user_id UUID, p_grade_id UUID)
RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
    v_result JSON;
BEGIN
    SELECT json_build_object(
        'id',                t.id,
        'title',             t.title,
        'starts_at',         t.starts_at,
        'ends_at',           t.ends_at,
        'status',            t.status,
        'user_entry_status', (SELECT te.status FROM public.tournament_entries te
                              WHERE te.tournament_id = t.id AND te.user_id = p_user_id),
        'question_count',    t.question_count,
        'time_limit_seconds', t.time_limit_seconds,
        'participant_count', (SELECT COUNT(*) FROM public.tournament_entries te2
                              WHERE te2.tournament_id = t.id)::int
    ) INTO v_result
    FROM public.tournaments t
    WHERE t.grade_id = p_grade_id
      AND t.status IN ('live','scheduled')
      AND t.ends_at > now()
    ORDER BY t.starts_at
    LIMIT 1;

    RETURN v_result;  -- NULL if no tournament found
END;
$$;


-- ─────────────────────────────────────────────────────────────────
-- 7. start_tournament(p_user_id UUID, p_tournament_id UUID)
--    Returns: { entry_id, questions[], time_limit_secs }
-- ─────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.start_tournament(p_user_id UUID, p_tournament_id UUID)
RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
    v_entry_id    UUID;
    v_questions   JSON;
    v_time_limit  INT;
    v_existing    TEXT;
BEGIN
    -- Check for existing entry
    SELECT te.status INTO v_existing
    FROM public.tournament_entries te
    WHERE te.tournament_id = p_tournament_id AND te.user_id = p_user_id;

    IF v_existing IS NOT NULL AND v_existing NOT IN ('not_started') THEN
        RAISE EXCEPTION 'Already started or completed this tournament';
    END IF;

    -- Get time limit
    SELECT time_limit_seconds INTO v_time_limit FROM public.tournaments WHERE id = p_tournament_id;

    -- Create or update entry
    INSERT INTO public.tournament_entries (tournament_id, user_id, status, time_remaining_secs, started_at)
    VALUES (p_tournament_id, p_user_id, 'in_progress', v_time_limit, now())
    ON CONFLICT (tournament_id, user_id)
    DO UPDATE SET status = 'in_progress', time_remaining_secs = v_time_limit, started_at = now()
    RETURNING id INTO v_entry_id;

    -- Fetch questions
    SELECT COALESCE(json_agg(
        json_build_object(
            'id',              qu.id,
            'question_type',   qu.question_type,
            'title',           qu.title,
            'prompt',          qu.prompt,
            'explanation',     '',  -- hidden during tournament
            'difficulty',      qu.difficulty,
            'time_limit_secs', qu.time_limit_secs,
            'allow_multiple',  qu.allow_multiple,
            'prompt_config',   qu.prompt_config,
            'metadata',        qu.metadata,
            'media_url',       qu.media_url,
            'options',         (
                SELECT COALESCE(json_agg(
                    json_build_object(
                        'id', o.id, 'label', o.label,
                        'is_correct', o.is_correct,
                        'sort_order', o.sort_order,
                        'correct_position', o.correct_position,
                        'media_url', o.media_url,
                        'visual_label', o.visual_label
                    ) ORDER BY o.sort_order
                ), '[]'::json) FROM public.question_options o WHERE o.question_id = qu.id
            ),
            'match_pairs', (
                SELECT json_agg(
                    json_build_object(
                        'id', mp.id, 'left_text', mp.left_text,
                        'right_text', mp.right_text, 'sort_order', mp.sort_order
                    ) ORDER BY mp.sort_order
                ) FROM public.match_pairs mp WHERE mp.question_id = qu.id
            )
        ) ORDER BY tq.sort_order
    ), '[]'::json) INTO v_questions
    FROM public.tournament_questions tq
    JOIN public.questions qu ON qu.id = tq.question_id
    WHERE tq.tournament_id = p_tournament_id;

    RETURN json_build_object(
        'entry_id',       v_entry_id,
        'questions',      v_questions,
        'time_limit_secs', v_time_limit
    );
END;
$$;


-- ─────────────────────────────────────────────────────────────────
-- 8. pause_tournament(p_entry_id UUID)
-- ─────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.pause_tournament(p_entry_id UUID)
RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
    v_result JSON;
BEGIN
    UPDATE public.tournament_entries
    SET status = 'paused'
    WHERE id = p_entry_id AND status = 'in_progress';

    SELECT row_to_json(te) INTO v_result
    FROM (
        SELECT id, tournament_id, user_id, status, score,
               time_taken_seconds, rank, time_remaining_secs, answers_so_far
        FROM public.tournament_entries WHERE id = p_entry_id
    ) te;

    RETURN v_result;
END;
$$;


-- ─────────────────────────────────────────────────────────────────
-- 9. resume_tournament(p_entry_id UUID)
-- ─────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.resume_tournament(p_entry_id UUID)
RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
    v_result JSON;
BEGIN
    UPDATE public.tournament_entries
    SET status = 'in_progress'
    WHERE id = p_entry_id AND status = 'paused';

    SELECT row_to_json(te) INTO v_result
    FROM (
        SELECT id, tournament_id, user_id, status, score,
               time_taken_seconds, rank, time_remaining_secs, answers_so_far
        FROM public.tournament_entries WHERE id = p_entry_id
    ) te;

    RETURN v_result;
END;
$$;


-- ─────────────────────────────────────────────────────────────────
-- 10. submit_tournament(p_entry_id UUID, p_answers JSON, p_time_taken INT)
-- ─────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.submit_tournament(p_entry_id UUID, p_answers JSON, p_time_taken INT)
RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
    v_user_id    UUID;
    v_tourn_id   UUID;
    v_score      INT := 0;
    v_total      INT := 0;
    v_xp_earned  INT;
    v_old_level  INT;
    v_new_level  INT;
    v_total_xp   BIGINT;
    v_rank       INT;
    v_answers_arr JSON;
BEGIN
    SELECT user_id, tournament_id INTO v_user_id, v_tourn_id
    FROM public.tournament_entries WHERE id = p_entry_id;

    -- Count correct from items array
    v_answers_arr := p_answers->'items';
    IF v_answers_arr IS NULL THEN
        v_answers_arr := p_answers;
    END IF;

    SELECT COUNT(*) INTO v_total FROM json_array_elements(v_answers_arr);
    SELECT COUNT(*) INTO v_score
    FROM json_array_elements(v_answers_arr) elem
    WHERE (elem->>'is_correct')::boolean = true;

    -- XP: 10 per correct answer for tournament
    v_xp_earned := v_score * 10;

    -- Update entry
    UPDATE public.tournament_entries
    SET status = 'completed',
        score = v_score,
        time_taken_seconds = p_time_taken,
        answers_so_far = v_answers_arr::jsonb,
        completed_at = now()
    WHERE id = p_entry_id;

    -- Update user XP
    SELECT level INTO v_old_level FROM public.users WHERE id = v_user_id;
    UPDATE public.users
    SET total_xp = total_xp + v_xp_earned,
        level = CASE
            WHEN total_xp + v_xp_earned >= 5000 THEN 10
            WHEN total_xp + v_xp_earned >= 4000 THEN 9
            WHEN total_xp + v_xp_earned >= 3200 THEN 8
            WHEN total_xp + v_xp_earned >= 2500 THEN 7
            WHEN total_xp + v_xp_earned >= 1900 THEN 6
            WHEN total_xp + v_xp_earned >= 1400 THEN 5
            WHEN total_xp + v_xp_earned >= 1000 THEN 4
            WHEN total_xp + v_xp_earned >= 600  THEN 3
            WHEN total_xp + v_xp_earned >= 300  THEN 2
            ELSE 1
        END
    WHERE id = v_user_id
    RETURNING total_xp, level INTO v_total_xp, v_new_level;

    -- Rank within this tournament
    SELECT COUNT(*) + 1 INTO v_rank
    FROM public.tournament_entries
    WHERE tournament_id = v_tourn_id AND score > v_score AND status IN ('completed','auto_submitted');

    -- Update rank on entry
    UPDATE public.tournament_entries SET rank = v_rank WHERE id = p_entry_id;

    RETURN json_build_object(
        'status',          'success',
        'attempt_id',      p_entry_id,
        'score',           v_score,
        'total_questions', v_total,
        'xp_earned',       v_xp_earned,
        'total_xp',        v_total_xp,
        'level',           v_new_level,
        'level_changed',   (v_new_level > v_old_level),
        'rank_global',     v_rank,
        'is_replay',       false,
        'next_quiz_id',    NULL
    );
END;
$$;


-- ─────────────────────────────────────────────────────────────────
-- 11. submit_tournament_answer(p_entry_id UUID, p_answer JSON)
--     Fire-and-forget per-question save during tournament
-- ─────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.submit_tournament_answer(p_entry_id UUID, p_answer JSON)
RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER
AS $$
BEGIN
    UPDATE public.tournament_entries
    SET answers_so_far = COALESCE(answers_so_far, '[]'::jsonb) || p_answer::jsonb
    WHERE id = p_entry_id;
END;
$$;


-- ─────────────────────────────────────────────────────────────────
-- 12. get_leaderboard(p_user_id UUID, p_filter TEXT,
--                     p_filter_id UUID, p_limit INT, p_offset INT)
--     p_filter: 'global' | 'country' | 'city' | 'school'
--     Returns: { ranked_users[], user_rank }
-- ─────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.get_leaderboard(
    p_user_id  UUID,
    p_filter   TEXT DEFAULT 'global',
    p_filter_id UUID DEFAULT NULL,
    p_limit    INT DEFAULT 50,
    p_offset   INT DEFAULT 0
)
RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
    v_ranked  JSON;
    v_user_rank JSON;
    v_country UUID;
    v_city    UUID;
    v_my_xp   BIGINT;
    v_rank_g  INT;
    v_rank_c  INT;
    v_rank_ci INT;
    v_xp_gap  BIGINT;
BEGIN
    SELECT country_id, city_id, total_xp INTO v_country, v_city, v_my_xp
    FROM public.users WHERE id = p_user_id;

    -- Build ranked list based on filter
    SELECT COALESCE(json_agg(row_to_json(r)), '[]'::json) INTO v_ranked
    FROM (
        SELECT
            u.id       AS user_id,
            u.display_name,
            u.avatar_id,
            u.total_xp,
            ROW_NUMBER() OVER (ORDER BY u.total_xp DESC, u.created_at ASC)::int AS rank
        FROM public.users u
        WHERE CASE
            WHEN p_filter = 'country' THEN u.country_id = COALESCE(p_filter_id, v_country)
            WHEN p_filter = 'city'    THEN u.city_id = COALESCE(p_filter_id, v_city)
            WHEN p_filter = 'school'  THEN u.school_name IS NOT NULL
                                           AND u.school_name = (SELECT school_name FROM public.users WHERE id = p_user_id)
            ELSE true
        END
        ORDER BY u.total_xp DESC, u.created_at ASC
        LIMIT p_limit OFFSET p_offset
    ) r;

    -- Calculate user's ranks
    SELECT COUNT(*) + 1 INTO v_rank_g FROM public.users WHERE total_xp > v_my_xp;

    IF v_country IS NOT NULL THEN
        SELECT COUNT(*) + 1 INTO v_rank_c
        FROM public.users WHERE country_id = v_country AND total_xp > v_my_xp;
    END IF;

    IF v_city IS NOT NULL THEN
        SELECT COUNT(*) + 1 INTO v_rank_ci
        FROM public.users WHERE city_id = v_city AND total_xp > v_my_xp;
    END IF;

    -- XP gap to next rank
    SELECT total_xp - v_my_xp INTO v_xp_gap
    FROM public.users
    WHERE total_xp > v_my_xp
    ORDER BY total_xp ASC
    LIMIT 1;

    v_user_rank := json_build_object(
        'rank_global',  v_rank_g,
        'rank_country', v_rank_c,
        'rank_city',    v_rank_ci,
        'xp_gap',       COALESCE(v_xp_gap, 0)
    );

    RETURN json_build_object(
        'ranked_users', v_ranked,
        'user_rank',    v_user_rank
    );
END;
$$;


-- ─────────────────────────────────────────────────────────────────
-- 13. get_user_stats(p_user_id UUID, p_period TEXT)
--     p_period: 'week' | 'month' | 'all'
--     Returns: { stats, accuracy_pct, daily_activity[], subject_performance[] }
-- ─────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.get_user_stats(p_user_id UUID, p_period TEXT DEFAULT 'week')
RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
    v_stats   JSON;
    v_acc     DOUBLE PRECISION;
    v_daily   JSON;
    v_subj    JSON;
    v_since   TIMESTAMPTZ;
BEGIN
    -- Determine period start
    v_since := CASE
        WHEN p_period = 'week'  THEN now() - INTERVAL '7 days'
        WHEN p_period = 'month' THEN now() - INTERVAL '30 days'
        ELSE '1970-01-01'::timestamptz
    END;

    -- User stats
    SELECT json_build_object(
        'total_xp',           u.total_xp,
        'level',              u.level,
        'streak_current',     u.streak_current,
        'streak_best',        u.streak_best,
        'quizzes_completed',  (SELECT COUNT(*) FROM public.quiz_attempts WHERE user_id = p_user_id AND created_at >= v_since),
        'accuracy_pct',       COALESCE(
            (SELECT ROUND(AVG(score::numeric / NULLIF(total_questions,0) * 100), 1)
             FROM public.quiz_attempts WHERE user_id = p_user_id AND created_at >= v_since), 0
        ),
        'tournaments_played', (SELECT COUNT(*) FROM public.tournament_entries
                               WHERE user_id = p_user_id AND status IN ('completed','auto_submitted')
                               AND created_at >= v_since),
        'best_tournament_rank', (SELECT MIN(rank) FROM public.tournament_entries
                                  WHERE user_id = p_user_id AND rank IS NOT NULL AND created_at >= v_since)
    ) INTO v_stats
    FROM public.users u WHERE u.id = p_user_id;

    -- Accuracy
    SELECT COALESCE(
        ROUND(AVG(score::numeric / NULLIF(total_questions,0) * 100), 1), 0
    ) INTO v_acc
    FROM public.quiz_attempts WHERE user_id = p_user_id AND created_at >= v_since;

    -- Daily activity
    SELECT COALESCE(json_agg(row_to_json(d) ORDER BY d.date), '[]'::json) INTO v_daily
    FROM (
        SELECT
            a.created_at::date::text AS date,
            COUNT(*)::int            AS quizzes,
            SUM(a.xp_earned)::int    AS xp,
            SUM(a.time_taken_secs / 60)::int AS time_spent_minutes
        FROM public.quiz_attempts a
        WHERE a.user_id = p_user_id AND a.created_at >= v_since
        GROUP BY a.created_at::date
        ORDER BY a.created_at::date
    ) d;

    -- Subject performance
    SELECT COALESCE(json_agg(row_to_json(s)), '[]'::json) INTO v_subj
    FROM (
        SELECT
            m.id                 AS module_id,
            m.title,
            m.emoji,
            COALESCE(MAX(ROUND(a.score::numeric / NULLIF(a.total_questions,0) * 100))::int, 0) AS best_score_pct,
            COALESCE(ROUND(AVG(a.score::numeric / NULLIF(a.total_questions,0) * 100))::int, 0) AS accuracy_pct,
            COUNT(DISTINCT CASE WHEN ch_done.is_done THEN ch.id END)::int AS chapters_completed,
            COUNT(DISTINCT ch.id)::int AS total_chapters
        FROM public.modules m
        JOIN public.chapters ch ON ch.module_id = m.id AND ch.is_active
        LEFT JOIN public.quizzes q ON q.chapter_id = ch.id AND q.is_active
        LEFT JOIN public.quiz_attempts a ON a.quiz_id = q.id AND a.user_id = p_user_id AND a.created_at >= v_since
        LEFT JOIN LATERAL (
            SELECT (COUNT(DISTINCT a2.quiz_id) >= COUNT(DISTINCT q2.id)) AS is_done
            FROM public.quizzes q2
            LEFT JOIN public.quiz_attempts a2 ON a2.quiz_id = q2.id AND a2.user_id = p_user_id
            WHERE q2.chapter_id = ch.id AND q2.is_active
        ) ch_done ON true
        WHERE m.is_active
        GROUP BY m.id, m.title, m.emoji
    ) s;

    RETURN json_build_object(
        'stats',               v_stats,
        'accuracy_pct',        v_acc,
        'daily_activity',      v_daily,
        'subject_performance', v_subj
    );
END;
$$;


-- ─────────────────────────────────────────────────────────────────
-- 14. get_profile(p_user_id UUID)
--     Returns: { user, stats, completed_chapters[], tournament_results[] }
-- ─────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.get_profile(p_user_id UUID)
RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
    v_user    JSON;
    v_stats   JSON;
    v_chapters JSON;
    v_tourns  JSON;
BEGIN
    -- User profile
    SELECT json_build_object(
        'id',            u.id,
        'display_name',  u.display_name,
        'avatar_id',     u.avatar_id,
        'grade_id',      u.grade_id,
        'grade_label',   COALESCE(g.label, ''),
        'auth_provider', u.auth_provider,
        'country_name',  co.name,
        'city_name',     ci.name,
        'school_name',   u.school_name
    ) INTO v_user
    FROM public.users u
    LEFT JOIN public.grades g ON g.id = u.grade_id
    LEFT JOIN public.countries co ON co.id = u.country_id
    LEFT JOIN public.cities ci ON ci.id = u.city_id
    WHERE u.id = p_user_id;

    IF v_user IS NULL THEN
        RAISE EXCEPTION 'User not found: %', p_user_id;
    END IF;

    -- Stats
    SELECT json_build_object(
        'total_xp',           u.total_xp,
        'level',              u.level,
        'streak_current',     u.streak_current,
        'streak_best',        u.streak_best,
        'quizzes_completed',  (SELECT COUNT(*) FROM public.quiz_attempts WHERE user_id = p_user_id),
        'accuracy_pct',       COALESCE(
            (SELECT ROUND(AVG(score::numeric / NULLIF(total_questions,0) * 100), 1)
             FROM public.quiz_attempts WHERE user_id = p_user_id), 0
        ),
        'tournaments_played', (SELECT COUNT(*) FROM public.tournament_entries
                               WHERE user_id = p_user_id AND status IN ('completed','auto_submitted')),
        'best_tournament_rank', (SELECT MIN(rank) FROM public.tournament_entries
                                  WHERE user_id = p_user_id AND rank IS NOT NULL)
    ) INTO v_stats
    FROM public.users u WHERE u.id = p_user_id;

    -- Completed chapters
    SELECT COALESCE(json_agg(row_to_json(cc) ORDER BY cc.completed_at DESC), '[]'::json) INTO v_chapters
    FROM (
        SELECT DISTINCT ON (ch.id)
            ch.id            AS chapter_id,
            ch.title         AS chapter_title,
            m.title          AS module_title,
            m.emoji          AS module_emoji,
            a.created_at::text AS completed_at
        FROM public.chapters ch
        JOIN public.modules m ON m.id = ch.module_id
        JOIN public.quizzes q ON q.chapter_id = ch.id AND q.is_active
        JOIN public.quiz_attempts a ON a.quiz_id = q.id AND a.user_id = p_user_id
        WHERE (
            SELECT COUNT(DISTINCT a2.quiz_id)
            FROM public.quiz_attempts a2
            JOIN public.quizzes q2 ON q2.id = a2.quiz_id
            WHERE q2.chapter_id = ch.id AND a2.user_id = p_user_id
        ) >= (
            SELECT COUNT(*) FROM public.quizzes q3 WHERE q3.chapter_id = ch.id AND q3.is_active
        )
        ORDER BY ch.id, a.created_at DESC
    ) cc;

    -- Tournament results
    SELECT COALESCE(json_agg(row_to_json(tr) ORDER BY tr.date DESC), '[]'::json) INTO v_tourns
    FROM (
        SELECT
            t.id                 AS tournament_id,
            t.title,
            te.score,
            t.question_count     AS total_questions,
            COALESCE(te.rank, 0) AS rank,
            (SELECT COUNT(*) FROM public.tournament_entries te2
             WHERE te2.tournament_id = t.id)::int AS participant_count,
            NULL::text           AS certificate_url,
            te.completed_at::text AS date
        FROM public.tournament_entries te
        JOIN public.tournaments t ON t.id = te.tournament_id
        WHERE te.user_id = p_user_id
          AND te.status IN ('completed','auto_submitted')
    ) tr;

    RETURN json_build_object(
        'user',               v_user,
        'stats',              v_stats,
        'completed_chapters', v_chapters,
        'tournament_results', v_tourns
    );
END;
$$;


-- ─────────────────────────────────────────────────────────────────
-- 15. update_profile(p_user_id UUID, p_fields JSON)
--     Dynamically updates user fields
-- ─────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.update_profile(p_user_id UUID, p_fields JSON)
RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
    v_key   TEXT;
    v_value TEXT;
    v_sql   TEXT := 'UPDATE public.users SET updated_at = now()';
    v_allowed TEXT[] := ARRAY['display_name','avatar_id','grade_id','school_name','country_id','city_id'];
BEGIN
    FOR v_key, v_value IN SELECT * FROM json_each_text(p_fields) LOOP
        IF v_key = ANY(v_allowed) THEN
            v_sql := v_sql || format(', %I = %L', v_key, v_value);
        END IF;
    END LOOP;

    v_sql := v_sql || format(' WHERE id = %L', p_user_id);
    EXECUTE v_sql;
END;
$$;


-- ═══════════════════════════════════════════════════════════════════
-- ═══════════════════════════════════════════════════════════════════
--  G R A N T S  (allow anon + authenticated to call RPCs)
-- ═══════════════════════════════════════════════════════════════════
-- ═══════════════════════════════════════════════════════════════════

GRANT USAGE ON SCHEMA public TO anon, authenticated;

-- Reference tables
GRANT SELECT ON public.grades    TO anon, authenticated;
GRANT SELECT ON public.countries TO anon, authenticated;
GRANT SELECT ON public.cities    TO anon, authenticated;

-- Users table (direct upsert from app)
GRANT SELECT, INSERT, UPDATE ON public.users TO authenticated;

-- Content tables (read-only for app)
GRANT SELECT ON public.modules            TO authenticated;
GRANT SELECT ON public.chapters           TO authenticated;
GRANT SELECT ON public.quizzes            TO authenticated;
GRANT SELECT ON public.questions          TO authenticated;
GRANT SELECT ON public.question_options   TO authenticated;
GRANT SELECT ON public.match_pairs        TO authenticated;
GRANT SELECT ON public.daily_challenges   TO authenticated;
GRANT SELECT ON public.tournaments        TO authenticated;
GRANT SELECT ON public.tournament_questions TO authenticated;

-- User data tables (read/write own)
GRANT SELECT, INSERT ON public.quiz_attempts      TO authenticated;
GRANT SELECT, INSERT, UPDATE ON public.tournament_entries TO authenticated;

-- RPC functions
GRANT EXECUTE ON FUNCTION public.get_user_dashboard(UUID)          TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_daily_challenges(UUID)        TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_module_full(UUID, UUID)       TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_chapter_quizzes(UUID, UUID)   TO authenticated;
GRANT EXECUTE ON FUNCTION public.submit_quiz_attempt(JSON)         TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_active_tournament(UUID, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.start_tournament(UUID, UUID)      TO authenticated;
GRANT EXECUTE ON FUNCTION public.pause_tournament(UUID)            TO authenticated;
GRANT EXECUTE ON FUNCTION public.resume_tournament(UUID)           TO authenticated;
GRANT EXECUTE ON FUNCTION public.submit_tournament(UUID, JSON, INT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.submit_tournament_answer(UUID, JSON) TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_leaderboard(UUID, TEXT, UUID, INT, INT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_user_stats(UUID, TEXT)        TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_profile(UUID)                 TO authenticated;
GRANT EXECUTE ON FUNCTION public.update_profile(UUID, JSON)        TO authenticated;


-- ═══════════════════════════════════════════════════════════════════
-- ═══════════════════════════════════════════════════════════════════
--  A U T H   C O N F I G U R A T I O N   N O T E S
-- ═══════════════════════════════════════════════════════════════════
-- ═══════════════════════════════════════════════════════════════════
--
-- The following must be configured in the Supabase Dashboard (not via SQL):
--
-- 1. GOOGLE OAUTH:
--    Dashboard → Authentication → Providers → Google
--    - Enable Google provider
--    - Set Client ID and Client Secret from Google Cloud Console
--    - Authorized redirect URI: https://<project-ref>.supabase.co/auth/v1/callback
--
-- 2. PHONE OTP:
--    Dashboard → Authentication → Providers → Phone
--    - Enable Phone provider
--    - Choose SMS provider (Twilio, MessageBird, etc.)
--    - Set API key, Auth Token, and Sender phone number
--
-- 3. ANONYMOUS AUTH:
--    Dashboard → Authentication → Settings
--    - Enable "Allow anonymous sign-ins"
--
-- 4. URL CONFIGURATION (for Google OAuth deep links):
--    Dashboard → Authentication → URL Configuration
--    - Site URL: your app's deep link scheme (e.g., com.android.mindquest://callback)
--    - Redirect URLs: add your app's OAuth callback URLs
--
-- ═══════════════════════════════════════════════════════════════════

-- Done! Your Supabase backend is now fully configured for MindQuest V2.
