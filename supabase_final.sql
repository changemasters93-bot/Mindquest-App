-- ╔══════════════════════════════════════════════════════════════════════╗
-- ║  MINDQUEST V2 — COMPLETE SUPABASE SQL (APP + CMS + ADMIN)         ║
-- ║                                                                    ║
-- ║  ✅ RUN IN ONE GO — paste entire file and hit Run                  ║
-- ║  ✅ Works on FRESH or EXISTING Supabase project                    ║
-- ║  ✅ Every statement is idempotent — safe to run multiple times     ║
-- ║  ✅ Supports: Mobile App + CMS/Admin Panel + Analytics             ║
-- ║  ✅ 19 tables, 47 RPCs, 60 RLS policies, full grants              ║
-- ║  ✅ 75 questions (15 sample + 60 IQ covering all 12 types)        ║
-- ║  ✅ IQ quiz 7-day cooldown, tournament max-participants enforced  ║
-- ║  ✅ Tournament-specific leaderboard RPC included                  ║
-- ║                                                                    ║
-- ║  HOW: Supabase Dashboard → SQL Editor → New query → Paste → Run   ║
-- ║  NOTE: ~200KB — takes 5-15 seconds to execute                     ║
-- ╚══════════════════════════════════════════════════════════════════════╝


-- ═══════════════════════════════════════════════════════════════════════
-- PART 1: EXTENSIONS
-- ═══════════════════════════════════════════════════════════════════════
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";


-- ═══════════════════════════════════════════════════════════════════════
-- PART 2: TABLES
-- ═══════════════════════════════════════════════════════════════════════

-- ─── 2a. grades ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.grades (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code       TEXT NOT NULL UNIQUE,
    label      TEXT NOT NULL,
    sort_order INT  NOT NULL DEFAULT 0,
    created_by UUID,
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- ─── 2b. countries ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.countries (
    id        UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name      TEXT    NOT NULL,
    code      TEXT    NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT true,
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- ─── 2c. cities ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.cities (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    country_id UUID NOT NULL REFERENCES public.countries(id),
    name       TEXT NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE (country_id, name)
);

-- ─── 2d. users ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.users (
    id             UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name   TEXT    NOT NULL,
    avatar_id      INT     NOT NULL DEFAULT 4,
    grade_id       UUID    NOT NULL REFERENCES public.grades(id),
    auth_provider  TEXT    NOT NULL DEFAULT 'anonymous'
                   CHECK (auth_provider IN ('google','phone','anonymous','google_and_phone')),
    country_id     UUID    REFERENCES public.countries(id),
    city_id        UUID    REFERENCES public.cities(id),
    school_name    TEXT,
    google_sub     TEXT,
    total_xp       BIGINT  NOT NULL DEFAULT 0,
    level          INT     NOT NULL DEFAULT 1,
    streak_current INT     NOT NULL DEFAULT 0,
    streak_best    INT     NOT NULL DEFAULT 0,
    iq_best_score  INT DEFAULT NULL,
    is_banned      BOOLEAN NOT NULL DEFAULT false,
    banned_reason  TEXT,
    banned_at      TIMESTAMPTZ,
    last_active_at TIMESTAMPTZ DEFAULT now(),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─── 2e. admin_users (CMS) ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.admin_users (
    id           UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name TEXT NOT NULL,
    email        TEXT,
    role         TEXT NOT NULL DEFAULT 'content_editor'
                 CHECK (role IN ('super_admin','admin','moderator','content_editor')),
    permissions  JSONB DEFAULT '{}'::jsonb,
    is_active    BOOLEAN NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─── 2f. modules ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.modules (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title         TEXT NOT NULL,
    subtitle      TEXT,
    description   TEXT,
    emoji         TEXT NOT NULL DEFAULT '📚',
    accent_color  TEXT NOT NULL DEFAULT '#4F46E5',
    thumbnail_url TEXT,
    display_order INT  NOT NULL DEFAULT 0,
    grade_id      UUID REFERENCES public.grades(id),
    is_active     BOOLEAN NOT NULL DEFAULT true,
    status        TEXT NOT NULL DEFAULT 'draft'
                  CHECK (status IN ('draft','review','published','archived')),
    created_by    UUID,
    updated_by    UUID,
    published_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─── 2g. chapters ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.chapters (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    module_id      UUID NOT NULL REFERENCES public.modules(id) ON DELETE CASCADE,
    title          TEXT NOT NULL,
    description    TEXT,
    chapter_number INT  NOT NULL DEFAULT 1,
    sort_order     INT  NOT NULL DEFAULT 0,
    is_active      BOOLEAN NOT NULL DEFAULT true,
    status         TEXT NOT NULL DEFAULT 'draft'
                   CHECK (status IN ('draft','review','published','archived')),
    created_by     UUID,
    updated_by     UUID,
    published_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─── 2h. quizzes ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.quizzes (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    chapter_id      UUID NOT NULL REFERENCES public.chapters(id) ON DELETE CASCADE,
    title           TEXT NOT NULL,
    description     TEXT,
    quiz_type       TEXT NOT NULL DEFAULT 'practice'
                    CHECK (quiz_type IN ('practice','iq','tournament','daily')),
    question_count  INT  NOT NULL DEFAULT 0,
    time_limit_secs INT  NOT NULL DEFAULT 300,
    max_xp          INT  NOT NULL DEFAULT 100,
    difficulty      TEXT CHECK (difficulty IN ('easy','medium','hard')),
    passing_score_pct INT NOT NULL DEFAULT 60,
    shuffle_options BOOLEAN NOT NULL DEFAULT true,
    show_explanation BOOLEAN NOT NULL DEFAULT true,
    sort_order      INT  NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    status          TEXT NOT NULL DEFAULT 'draft'
                    CHECK (status IN ('draft','review','published','archived')),
    created_by      UUID,
    updated_by      UUID,
    published_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    cooldown_hours  INT DEFAULT NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─── 2i. questions ───────────────────────────────────────────────────
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
    status          TEXT NOT NULL DEFAULT 'draft'
                    CHECK (status IN ('draft','review','published','archived')),
    created_by      UUID,
    updated_by      UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─── 2j. question_options ────────────────────────────────────────────
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

-- ─── 2k. match_pairs ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.match_pairs (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    question_id UUID NOT NULL REFERENCES public.questions(id) ON DELETE CASCADE,
    left_text   TEXT NOT NULL,
    right_text  TEXT NOT NULL,
    sort_order  INT  NOT NULL DEFAULT 0
);

-- ─── 2l. quiz_attempts ──────────────────────────────────────────────
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

-- ─── 2m. daily_challenges ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.daily_challenges (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quiz_id        UUID NOT NULL REFERENCES public.quizzes(id) ON DELETE CASCADE,
    grade_id       UUID NOT NULL REFERENCES public.grades(id),
    challenge_date DATE NOT NULL DEFAULT CURRENT_DATE,
    is_active      BOOLEAN NOT NULL DEFAULT true,
    created_by     UUID,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (quiz_id, challenge_date)
);

-- ─── 2n. tournaments ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.tournaments (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title              TEXT NOT NULL,
    description        TEXT,
    grade_id           UUID NOT NULL REFERENCES public.grades(id),
    question_count     INT  NOT NULL DEFAULT 10,
    time_limit_seconds INT  NOT NULL DEFAULT 600,
    max_participants   INT,
    starts_at          TIMESTAMPTZ NOT NULL,
    ends_at            TIMESTAMPTZ NOT NULL,
    status             TEXT NOT NULL DEFAULT 'draft'
                       CHECK (status IN ('draft','scheduled','live','closed','finalized')),
    created_by         UUID,
    updated_by         UUID,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─── 2o. tournament_questions ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.tournament_questions (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tournament_id UUID NOT NULL REFERENCES public.tournaments(id) ON DELETE CASCADE,
    question_id   UUID NOT NULL REFERENCES public.questions(id) ON DELETE CASCADE,
    sort_order    INT  NOT NULL DEFAULT 0,
    UNIQUE (tournament_id, question_id)
);

-- ─── 2p. tournament_entries ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.tournament_entries (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tournament_id      UUID NOT NULL REFERENCES public.tournaments(id) ON DELETE CASCADE,
    user_id            UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    status             TEXT NOT NULL DEFAULT 'not_started'
                       CHECK (status IN ('not_started','in_progress','paused','completed','auto_submitted')),
    score              INT  NOT NULL DEFAULT 0,
    time_taken_seconds INT,
    rank               INT,
    time_remaining_secs INT,
    answers_so_far     JSONB DEFAULT '[]'::jsonb,
    started_at         TIMESTAMPTZ,
    completed_at       TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tournament_id, user_id)
);

-- ─── 2q. audit_logs (CMS) ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.audit_logs (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    admin_id    UUID NOT NULL,
    action      TEXT NOT NULL
                CHECK (action IN ('create','update','delete','publish','archive','ban','unban')),
    table_name  TEXT NOT NULL,
    record_id   UUID,
    old_values  JSONB,
    new_values  JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_audit_logs_admin   ON public.audit_logs(admin_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_table   ON public.audit_logs(table_name, record_id);

-- ─── 2r. media_uploads (CMS) ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS public.media_uploads (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    uploaded_by  UUID NOT NULL,
    file_name    TEXT NOT NULL,
    file_size    INT  NOT NULL DEFAULT 0,
    mime_type    TEXT NOT NULL DEFAULT 'image/png',
    storage_path TEXT NOT NULL,
    cdn_url      TEXT,
    alt_text     TEXT,
    width        INT,
    height       INT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_media_uploads_by ON public.media_uploads(uploaded_by, created_at);

-- ─── 2s. content_status_history (CMS) ────────────────────────────────
CREATE TABLE IF NOT EXISTS public.content_status_history (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content_type TEXT NOT NULL
                 CHECK (content_type IN ('module','chapter','quiz','question','tournament')),
    content_id   UUID NOT NULL,
    status       TEXT NOT NULL
                 CHECK (status IN ('draft','review','published','archived')),
    changed_by   UUID NOT NULL,
    reason       TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_content_history ON public.content_status_history(content_id, created_at);


-- ═══════════════════════════════════════════════════════════════════════
-- PART 3: PATCH EXISTING TABLES (safe on fresh or old setups)
-- ═══════════════════════════════════════════════════════════════════════

-- users — patch
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS google_sub       TEXT;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS total_xp         BIGINT NOT NULL DEFAULT 0;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS level            INT    NOT NULL DEFAULT 1;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS streak_current   INT    NOT NULL DEFAULT 0;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS streak_best      INT    NOT NULL DEFAULT 0;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS last_active_at   TIMESTAMPTZ DEFAULT now();
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS updated_at       TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS iq_best_score    INT DEFAULT NULL;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS is_banned        BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS banned_reason    TEXT;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS banned_at        TIMESTAMPTZ;

DO $$
BEGIN
    ALTER TABLE public.users DROP CONSTRAINT IF EXISTS users_auth_provider_check;
    ALTER TABLE public.users ADD CONSTRAINT users_auth_provider_check
        CHECK (auth_provider IN ('google','phone','anonymous','google_and_phone'));
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'auth_provider constraint: %', SQLERRM;
END $$;

-- modules — patch
ALTER TABLE public.modules ADD COLUMN IF NOT EXISTS description    TEXT;
ALTER TABLE public.modules ADD COLUMN IF NOT EXISTS thumbnail_url  TEXT;
ALTER TABLE public.modules ADD COLUMN IF NOT EXISTS status         TEXT NOT NULL DEFAULT 'draft';
ALTER TABLE public.modules ADD COLUMN IF NOT EXISTS created_by     UUID;
ALTER TABLE public.modules ADD COLUMN IF NOT EXISTS updated_by     UUID;
ALTER TABLE public.modules ADD COLUMN IF NOT EXISTS published_at   TIMESTAMPTZ;
ALTER TABLE public.modules ADD COLUMN IF NOT EXISTS updated_at     TIMESTAMPTZ DEFAULT now();

-- chapters — patch
ALTER TABLE public.chapters ADD COLUMN IF NOT EXISTS description   TEXT;
ALTER TABLE public.chapters ADD COLUMN IF NOT EXISTS status        TEXT NOT NULL DEFAULT 'draft';
ALTER TABLE public.chapters ADD COLUMN IF NOT EXISTS created_by    UUID;
ALTER TABLE public.chapters ADD COLUMN IF NOT EXISTS updated_by    UUID;
ALTER TABLE public.chapters ADD COLUMN IF NOT EXISTS published_at  TIMESTAMPTZ;
ALTER TABLE public.chapters ADD COLUMN IF NOT EXISTS updated_at    TIMESTAMPTZ DEFAULT now();

-- quizzes — patch
ALTER TABLE public.quizzes ADD COLUMN IF NOT EXISTS description      TEXT;
ALTER TABLE public.quizzes ADD COLUMN IF NOT EXISTS passing_score_pct INT NOT NULL DEFAULT 60;
ALTER TABLE public.quizzes ADD COLUMN IF NOT EXISTS shuffle_options  BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE public.quizzes ADD COLUMN IF NOT EXISTS show_explanation BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE public.quizzes ADD COLUMN IF NOT EXISTS status           TEXT NOT NULL DEFAULT 'draft';
ALTER TABLE public.quizzes ADD COLUMN IF NOT EXISTS created_by       UUID;
ALTER TABLE public.quizzes ADD COLUMN IF NOT EXISTS updated_by       UUID;
ALTER TABLE public.quizzes ADD COLUMN IF NOT EXISTS published_at     TIMESTAMPTZ;
ALTER TABLE public.quizzes ADD COLUMN IF NOT EXISTS updated_at       TIMESTAMPTZ DEFAULT now();
ALTER TABLE public.quizzes ADD COLUMN IF NOT EXISTS cooldown_hours   INT DEFAULT NULL;

-- questions — patch
ALTER TABLE public.questions ADD COLUMN IF NOT EXISTS prompt_config  JSONB;
ALTER TABLE public.questions ADD COLUMN IF NOT EXISTS metadata       JSONB;
ALTER TABLE public.questions ADD COLUMN IF NOT EXISTS media_url      TEXT;
ALTER TABLE public.questions ADD COLUMN IF NOT EXISTS allow_multiple BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE public.questions ADD COLUMN IF NOT EXISTS status         TEXT NOT NULL DEFAULT 'draft';
ALTER TABLE public.questions ADD COLUMN IF NOT EXISTS created_by     UUID;
ALTER TABLE public.questions ADD COLUMN IF NOT EXISTS updated_by     UUID;
ALTER TABLE public.questions ADD COLUMN IF NOT EXISTS updated_at     TIMESTAMPTZ DEFAULT now();

-- question_options — patch
ALTER TABLE public.question_options ADD COLUMN IF NOT EXISTS correct_position INT;
ALTER TABLE public.question_options ADD COLUMN IF NOT EXISTS media_url        TEXT;
ALTER TABLE public.question_options ADD COLUMN IF NOT EXISTS visual_label     TEXT;

-- quiz_attempts — patch
ALTER TABLE public.quiz_attempts ADD COLUMN IF NOT EXISTS idempotency_key TEXT;
ALTER TABLE public.quiz_attempts ADD COLUMN IF NOT EXISTS xp_earned       INT NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'quiz_attempts_user_id_idempotency_key_key') THEN
        ALTER TABLE public.quiz_attempts ADD CONSTRAINT quiz_attempts_user_id_idempotency_key_key UNIQUE (user_id, idempotency_key);
    END IF;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'idempotency constraint: %', SQLERRM;
END $$;

-- tournament_entries — patch
ALTER TABLE public.tournament_entries ADD COLUMN IF NOT EXISTS time_remaining_secs INT;
ALTER TABLE public.tournament_entries ADD COLUMN IF NOT EXISTS answers_so_far     JSONB DEFAULT '[]'::jsonb;
ALTER TABLE public.tournament_entries ADD COLUMN IF NOT EXISTS started_at         TIMESTAMPTZ;
ALTER TABLE public.tournament_entries ADD COLUMN IF NOT EXISTS completed_at       TIMESTAMPTZ;

-- tournaments — patch
ALTER TABLE public.tournaments ADD COLUMN IF NOT EXISTS description      TEXT;
ALTER TABLE public.tournaments ADD COLUMN IF NOT EXISTS max_participants INT;
ALTER TABLE public.tournaments ADD COLUMN IF NOT EXISTS created_by       UUID;
ALTER TABLE public.tournaments ADD COLUMN IF NOT EXISTS updated_by       UUID;
ALTER TABLE public.tournaments ADD COLUMN IF NOT EXISTS updated_at       TIMESTAMPTZ DEFAULT now();

-- daily_challenges — patch
ALTER TABLE public.daily_challenges ADD COLUMN IF NOT EXISTS created_by UUID;

-- grades — patch
ALTER TABLE public.grades ADD COLUMN IF NOT EXISTS created_by  UUID;
ALTER TABLE public.grades ADD COLUMN IF NOT EXISTS updated_at  TIMESTAMPTZ DEFAULT now();

-- countries — patch
ALTER TABLE public.countries ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT now();

-- cities — patch
ALTER TABLE public.cities ADD COLUMN IF NOT EXISTS created_by  UUID;
ALTER TABLE public.cities ADD COLUMN IF NOT EXISTS updated_at  TIMESTAMPTZ DEFAULT now();


-- ═══════════════════════════════════════════════════════════════════════
-- PART 4: SEED REFERENCE DATA
-- ═══════════════════════════════════════════════════════════════════════

INSERT INTO public.grades (code, label, sort_order) VALUES
    ('G1','Grade 1',1),('G2','Grade 2',2),('G3','Grade 3',3),
    ('G4','Grade 4',4),('G5','Grade 5',5),('G6','Grade 6',6),
    ('G7','Grade 7',7),('G8','Grade 8',8)
ON CONFLICT (code) DO NOTHING;

INSERT INTO public.countries (name, code) VALUES
    ('India','IN'),('United States','US'),('United Kingdom','GB'),
    ('Canada','CA'),('Australia','AU'),('Singapore','SG'),('UAE','AE')
ON CONFLICT (code) DO NOTHING;

INSERT INTO public.cities (country_id, name)
SELECT c.id, v.name FROM public.countries c,
    (VALUES ('Delhi'),('Mumbai'),('Bangalore'),('Chennai'),('Hyderabad'),('Pune'),('Kolkata')) AS v(name)
WHERE c.code = 'IN' ON CONFLICT (country_id, name) DO NOTHING;

INSERT INTO public.cities (country_id, name)
SELECT c.id, v.name FROM public.countries c,
    (VALUES ('New York'),('Los Angeles'),('Chicago'),('Houston'),('Phoenix')) AS v(name)
WHERE c.code = 'US' ON CONFLICT (country_id, name) DO NOTHING;

INSERT INTO public.cities (country_id, name)
SELECT c.id, v.name FROM public.countries c,
    (VALUES ('London'),('Manchester'),('Birmingham'),('Edinburgh')) AS v(name)
WHERE c.code = 'GB' ON CONFLICT (country_id, name) DO NOTHING;

INSERT INTO public.cities (country_id, name)
SELECT c.id, v.name FROM public.countries c,
    (VALUES ('Toronto'),('Vancouver'),('Montreal'),('Ottawa')) AS v(name)
WHERE c.code = 'CA' ON CONFLICT (country_id, name) DO NOTHING;

INSERT INTO public.cities (country_id, name)
SELECT c.id, v.name FROM public.countries c,
    (VALUES ('Sydney'),('Melbourne'),('Brisbane'),('Perth')) AS v(name)
WHERE c.code = 'AU' ON CONFLICT (country_id, name) DO NOTHING;

INSERT INTO public.cities (country_id, name)
SELECT c.id, v.name FROM public.countries c,
    (VALUES ('Singapore')) AS v(name)
WHERE c.code = 'SG' ON CONFLICT (country_id, name) DO NOTHING;

INSERT INTO public.cities (country_id, name)
SELECT c.id, v.name FROM public.countries c,
    (VALUES ('Dubai'),('Abu Dhabi'),('Sharjah')) AS v(name)
WHERE c.code = 'AE' ON CONFLICT (country_id, name) DO NOTHING;


-- ═══════════════════════════════════════════════════════════════════════
-- PART 5: ROW LEVEL SECURITY
-- ═══════════════════════════════════════════════════════════════════════

-- ─── Helper: is_admin function ───────────────────────────────────────
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN LANGUAGE sql SECURITY DEFINER STABLE AS $$
    SELECT EXISTS(SELECT 1 FROM public.admin_users WHERE id = auth.uid() AND is_active = true);
$$;

-- Enable RLS on every table
ALTER TABLE public.users              ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.grades             ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.countries          ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cities             ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.admin_users        ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.modules            ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chapters           ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.quizzes            ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.questions          ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.question_options   ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.match_pairs        ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.quiz_attempts      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.daily_challenges   ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tournaments        ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tournament_questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tournament_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs         ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.media_uploads      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.content_status_history ENABLE ROW LEVEL SECURITY;

-- ─── Reference tables: any authenticated can read ────────────────────
DROP POLICY IF EXISTS "grades_select" ON public.grades;
CREATE POLICY "grades_select" ON public.grades FOR SELECT TO authenticated USING (true);

DROP POLICY IF EXISTS "countries_select" ON public.countries;
CREATE POLICY "countries_select" ON public.countries FOR SELECT TO authenticated USING (true);

DROP POLICY IF EXISTS "cities_select" ON public.cities;
CREATE POLICY "cities_select" ON public.cities FOR SELECT TO authenticated USING (true);

-- Admin can write reference tables
DROP POLICY IF EXISTS "grades_admin_all" ON public.grades;
CREATE POLICY "grades_admin_all" ON public.grades FOR ALL TO authenticated USING (public.is_admin()) WITH CHECK (public.is_admin());

DROP POLICY IF EXISTS "countries_admin_all" ON public.countries;
CREATE POLICY "countries_admin_all" ON public.countries FOR ALL TO authenticated USING (public.is_admin()) WITH CHECK (public.is_admin());

DROP POLICY IF EXISTS "cities_admin_all" ON public.cities;
CREATE POLICY "cities_admin_all" ON public.cities FOR ALL TO authenticated USING (public.is_admin()) WITH CHECK (public.is_admin());

-- ─── Users: own row for app users, all rows for admin ────────────────
DROP POLICY IF EXISTS "users_select_own" ON public.users;
CREATE POLICY "users_select_own" ON public.users FOR SELECT TO authenticated
    USING (auth.uid() = id OR public.is_admin());

DROP POLICY IF EXISTS "users_insert_own" ON public.users;
CREATE POLICY "users_insert_own" ON public.users FOR INSERT TO authenticated WITH CHECK (auth.uid() = id);

DROP POLICY IF EXISTS "users_update_own" ON public.users;
CREATE POLICY "users_update_own" ON public.users FOR UPDATE TO authenticated
    USING (auth.uid() = id OR public.is_admin());

-- ─── Admin users table ───────────────────────────────────────────────
DROP POLICY IF EXISTS "admin_select" ON public.admin_users;
CREATE POLICY "admin_select" ON public.admin_users FOR SELECT TO authenticated
    USING (auth.uid() = id OR public.is_admin());

DROP POLICY IF EXISTS "admin_insert" ON public.admin_users;
CREATE POLICY "admin_insert" ON public.admin_users FOR INSERT TO authenticated
    WITH CHECK (EXISTS(SELECT 1 FROM public.admin_users WHERE id = auth.uid() AND role = 'super_admin'));

DROP POLICY IF EXISTS "admin_update" ON public.admin_users;
CREATE POLICY "admin_update" ON public.admin_users FOR UPDATE TO authenticated
    USING (EXISTS(SELECT 1 FROM public.admin_users WHERE id = auth.uid() AND role IN ('super_admin','admin')));

-- ─── Content tables: authenticated read + admin write ────────────────

-- modules
DROP POLICY IF EXISTS "modules_select" ON public.modules;
CREATE POLICY "modules_select" ON public.modules FOR SELECT TO authenticated USING (true);
DROP POLICY IF EXISTS "modules_admin_insert" ON public.modules;
CREATE POLICY "modules_admin_insert" ON public.modules FOR INSERT TO authenticated WITH CHECK (public.is_admin());
DROP POLICY IF EXISTS "modules_admin_update" ON public.modules;
CREATE POLICY "modules_admin_update" ON public.modules FOR UPDATE TO authenticated USING (public.is_admin());
DROP POLICY IF EXISTS "modules_admin_delete" ON public.modules;
CREATE POLICY "modules_admin_delete" ON public.modules FOR DELETE TO authenticated USING (public.is_admin());

-- chapters
DROP POLICY IF EXISTS "chapters_select" ON public.chapters;
CREATE POLICY "chapters_select" ON public.chapters FOR SELECT TO authenticated USING (true);
DROP POLICY IF EXISTS "chapters_admin_insert" ON public.chapters;
CREATE POLICY "chapters_admin_insert" ON public.chapters FOR INSERT TO authenticated WITH CHECK (public.is_admin());
DROP POLICY IF EXISTS "chapters_admin_update" ON public.chapters;
CREATE POLICY "chapters_admin_update" ON public.chapters FOR UPDATE TO authenticated USING (public.is_admin());
DROP POLICY IF EXISTS "chapters_admin_delete" ON public.chapters;
CREATE POLICY "chapters_admin_delete" ON public.chapters FOR DELETE TO authenticated USING (public.is_admin());

-- quizzes
DROP POLICY IF EXISTS "quizzes_select" ON public.quizzes;
CREATE POLICY "quizzes_select" ON public.quizzes FOR SELECT TO authenticated USING (true);
DROP POLICY IF EXISTS "quizzes_admin_insert" ON public.quizzes;
CREATE POLICY "quizzes_admin_insert" ON public.quizzes FOR INSERT TO authenticated WITH CHECK (public.is_admin());
DROP POLICY IF EXISTS "quizzes_admin_update" ON public.quizzes;
CREATE POLICY "quizzes_admin_update" ON public.quizzes FOR UPDATE TO authenticated USING (public.is_admin());
DROP POLICY IF EXISTS "quizzes_admin_delete" ON public.quizzes;
CREATE POLICY "quizzes_admin_delete" ON public.quizzes FOR DELETE TO authenticated USING (public.is_admin());

-- questions
DROP POLICY IF EXISTS "questions_select" ON public.questions;
CREATE POLICY "questions_select" ON public.questions FOR SELECT TO authenticated USING (true);
DROP POLICY IF EXISTS "questions_admin_insert" ON public.questions;
CREATE POLICY "questions_admin_insert" ON public.questions FOR INSERT TO authenticated WITH CHECK (public.is_admin());
DROP POLICY IF EXISTS "questions_admin_update" ON public.questions;
CREATE POLICY "questions_admin_update" ON public.questions FOR UPDATE TO authenticated USING (public.is_admin());
DROP POLICY IF EXISTS "questions_admin_delete" ON public.questions;
CREATE POLICY "questions_admin_delete" ON public.questions FOR DELETE TO authenticated USING (public.is_admin());

-- question_options
DROP POLICY IF EXISTS "options_select" ON public.question_options;
CREATE POLICY "options_select" ON public.question_options FOR SELECT TO authenticated USING (true);
DROP POLICY IF EXISTS "options_admin_insert" ON public.question_options;
CREATE POLICY "options_admin_insert" ON public.question_options FOR INSERT TO authenticated WITH CHECK (public.is_admin());
DROP POLICY IF EXISTS "options_admin_update" ON public.question_options;
CREATE POLICY "options_admin_update" ON public.question_options FOR UPDATE TO authenticated USING (public.is_admin());
DROP POLICY IF EXISTS "options_admin_delete" ON public.question_options;
CREATE POLICY "options_admin_delete" ON public.question_options FOR DELETE TO authenticated USING (public.is_admin());

-- match_pairs
DROP POLICY IF EXISTS "match_pairs_select" ON public.match_pairs;
CREATE POLICY "match_pairs_select" ON public.match_pairs FOR SELECT TO authenticated USING (true);
DROP POLICY IF EXISTS "match_pairs_admin_insert" ON public.match_pairs;
CREATE POLICY "match_pairs_admin_insert" ON public.match_pairs FOR INSERT TO authenticated WITH CHECK (public.is_admin());
DROP POLICY IF EXISTS "match_pairs_admin_update" ON public.match_pairs;
CREATE POLICY "match_pairs_admin_update" ON public.match_pairs FOR UPDATE TO authenticated USING (public.is_admin());
DROP POLICY IF EXISTS "match_pairs_admin_delete" ON public.match_pairs;
CREATE POLICY "match_pairs_admin_delete" ON public.match_pairs FOR DELETE TO authenticated USING (public.is_admin());

-- daily_challenges
DROP POLICY IF EXISTS "daily_challenges_select" ON public.daily_challenges;
CREATE POLICY "daily_challenges_select" ON public.daily_challenges FOR SELECT TO authenticated USING (true);
DROP POLICY IF EXISTS "daily_challenges_admin_insert" ON public.daily_challenges;
CREATE POLICY "daily_challenges_admin_insert" ON public.daily_challenges FOR INSERT TO authenticated WITH CHECK (public.is_admin());
DROP POLICY IF EXISTS "daily_challenges_admin_update" ON public.daily_challenges;
CREATE POLICY "daily_challenges_admin_update" ON public.daily_challenges FOR UPDATE TO authenticated USING (public.is_admin());
DROP POLICY IF EXISTS "daily_challenges_admin_delete" ON public.daily_challenges;
CREATE POLICY "daily_challenges_admin_delete" ON public.daily_challenges FOR DELETE TO authenticated USING (public.is_admin());

-- tournaments
DROP POLICY IF EXISTS "tournaments_select" ON public.tournaments;
CREATE POLICY "tournaments_select" ON public.tournaments FOR SELECT TO authenticated USING (true);
DROP POLICY IF EXISTS "tournaments_admin_insert" ON public.tournaments;
CREATE POLICY "tournaments_admin_insert" ON public.tournaments FOR INSERT TO authenticated WITH CHECK (public.is_admin());
DROP POLICY IF EXISTS "tournaments_admin_update" ON public.tournaments;
CREATE POLICY "tournaments_admin_update" ON public.tournaments FOR UPDATE TO authenticated USING (public.is_admin());
DROP POLICY IF EXISTS "tournaments_admin_delete" ON public.tournaments;
CREATE POLICY "tournaments_admin_delete" ON public.tournaments FOR DELETE TO authenticated USING (public.is_admin());

-- tournament_questions
DROP POLICY IF EXISTS "tournament_questions_select" ON public.tournament_questions;
CREATE POLICY "tournament_questions_select" ON public.tournament_questions FOR SELECT TO authenticated USING (true);
DROP POLICY IF EXISTS "tq_admin_insert" ON public.tournament_questions;
CREATE POLICY "tq_admin_insert" ON public.tournament_questions FOR INSERT TO authenticated WITH CHECK (public.is_admin());
DROP POLICY IF EXISTS "tq_admin_update" ON public.tournament_questions;
CREATE POLICY "tq_admin_update" ON public.tournament_questions FOR UPDATE TO authenticated USING (public.is_admin());
DROP POLICY IF EXISTS "tq_admin_delete" ON public.tournament_questions;
CREATE POLICY "tq_admin_delete" ON public.tournament_questions FOR DELETE TO authenticated USING (public.is_admin());

-- ─── Quiz attempts: own rows (app), all rows (admin) ─────────────────
DROP POLICY IF EXISTS "attempts_select_own" ON public.quiz_attempts;
CREATE POLICY "attempts_select_own" ON public.quiz_attempts FOR SELECT TO authenticated
    USING (auth.uid() = user_id OR public.is_admin());

DROP POLICY IF EXISTS "attempts_insert_own" ON public.quiz_attempts;
CREATE POLICY "attempts_insert_own" ON public.quiz_attempts FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

-- ─── Tournament entries: all read (leaderboard), own write ───────────
DROP POLICY IF EXISTS "entries_select" ON public.tournament_entries;
CREATE POLICY "entries_select" ON public.tournament_entries FOR SELECT TO authenticated USING (true);

DROP POLICY IF EXISTS "entries_insert_own" ON public.tournament_entries;
CREATE POLICY "entries_insert_own" ON public.tournament_entries FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "entries_update_own" ON public.tournament_entries;
CREATE POLICY "entries_update_own" ON public.tournament_entries FOR UPDATE TO authenticated
    USING (auth.uid() = user_id OR public.is_admin());

-- ─── CMS tables: admin only ─────────────────────────────────────────
DROP POLICY IF EXISTS "audit_logs_select" ON public.audit_logs;
CREATE POLICY "audit_logs_select" ON public.audit_logs FOR SELECT TO authenticated USING (public.is_admin());
DROP POLICY IF EXISTS "audit_logs_insert" ON public.audit_logs;
CREATE POLICY "audit_logs_insert" ON public.audit_logs FOR INSERT TO authenticated WITH CHECK (public.is_admin());

DROP POLICY IF EXISTS "media_select" ON public.media_uploads;
CREATE POLICY "media_select" ON public.media_uploads FOR SELECT TO authenticated USING (public.is_admin());
DROP POLICY IF EXISTS "media_insert" ON public.media_uploads;
CREATE POLICY "media_insert" ON public.media_uploads FOR INSERT TO authenticated WITH CHECK (public.is_admin());
DROP POLICY IF EXISTS "media_delete" ON public.media_uploads;
CREATE POLICY "media_delete" ON public.media_uploads FOR DELETE TO authenticated USING (public.is_admin());

DROP POLICY IF EXISTS "content_history_select" ON public.content_status_history;
CREATE POLICY "content_history_select" ON public.content_status_history FOR SELECT TO authenticated USING (public.is_admin());
DROP POLICY IF EXISTS "content_history_insert" ON public.content_status_history;
CREATE POLICY "content_history_insert" ON public.content_status_history FOR INSERT TO authenticated WITH CHECK (public.is_admin());


-- ═══════════════════════════════════════════════════════════════════════
-- PART 6: APP RPC FUNCTIONS (15 — mobile app uses these)
-- ═══════════════════════════════════════════════════════════════════════

-- ─── 6.1  get_user_dashboard ─────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.get_user_dashboard(p_user_id UUID)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_user JSON; v_stats JSON; v_mods JSON; v_tourn JSON; v_grade UUID;
BEGIN
    -- Check if user is banned
    IF EXISTS(SELECT 1 FROM public.users WHERE id = p_user_id AND is_banned = true) THEN
        RAISE EXCEPTION 'Account suspended';
    END IF;

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
        'accuracy_pct',       COALESCE((SELECT ROUND(AVG(score::numeric / NULLIF(total_questions,0) * 100),1)
                              FROM public.quiz_attempts WHERE user_id = p_user_id), 0),
        'tournaments_played', (SELECT COUNT(*) FROM public.tournament_entries
                              WHERE user_id = p_user_id AND status IN ('completed','auto_submitted')),
        'best_tournament_rank', (SELECT MIN(rank) FROM public.tournament_entries
                                WHERE user_id = p_user_id AND rank IS NOT NULL),
        'iq_best_score',      u.iq_best_score,
        'last_iq_attempt_at', (SELECT MAX(qa.created_at) FROM public.quiz_attempts qa
            JOIN public.quizzes iq ON iq.id = qa.quiz_id AND iq.quiz_type = 'iq'
            WHERE qa.user_id = p_user_id),
        'iq_cooldown_hours', (SELECT iq.cooldown_hours FROM public.quizzes iq
            WHERE iq.quiz_type = 'iq' AND iq.is_active LIMIT 1)
    ) INTO v_stats FROM public.users u WHERE u.id = p_user_id;

    SELECT COALESCE(json_agg(row_to_json(m_row) ORDER BY m_row.sort_order), '[]'::json) INTO v_mods
    FROM (
        SELECT m.id, m.title, m.emoji, m.accent_color, m.display_order AS sort_order,
            (SELECT json_build_object(
                'current_chapter_id', NULL,
                'current_quiz_id',    NULL,
                'best_score_pct', (SELECT MAX(ROUND(a.score::numeric / NULLIF(a.total_questions,0) * 100))::int
                    FROM public.quiz_attempts a
                    JOIN public.quizzes q2 ON q2.id = a.quiz_id
                    JOIN public.chapters ch2 ON ch2.id = q2.chapter_id
                    WHERE ch2.module_id = m.id AND a.user_id = p_user_id),
                'is_completed', false
            )) AS progress
        FROM public.modules m
        WHERE m.is_active = true AND (m.grade_id IS NULL OR m.grade_id = v_grade)
    ) m_row;

    SELECT json_build_object(
        'id', t.id, 'title', t.title, 'starts_at', t.starts_at, 'ends_at', t.ends_at,
        'status', t.status,
        'user_entry_status', (SELECT te.status FROM public.tournament_entries te
                             WHERE te.tournament_id = t.id AND te.user_id = p_user_id),
        'question_count', t.question_count, 'time_limit_seconds', t.time_limit_seconds,
        'participant_count', (SELECT COUNT(*) FROM public.tournament_entries te2
                             WHERE te2.tournament_id = t.id)::int
    ) INTO v_tourn
    FROM public.tournaments t
    WHERE t.grade_id = v_grade AND t.status IN ('live','scheduled') AND t.ends_at > now()
    ORDER BY t.starts_at LIMIT 1;

    UPDATE public.users SET last_active_at = now() WHERE id = p_user_id;

    RETURN json_build_object(
        'user', v_user, 'stats', v_stats, 'modules', v_mods, 'active_tournament', v_tourn
    );
END; $$;

-- ─── 6.2  get_daily_challenges ───────────────────────────────────────
CREATE OR REPLACE FUNCTION public.get_daily_challenges(p_user_id UUID)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_grade UUID; v_result JSON;
BEGIN
    SELECT grade_id INTO v_grade FROM public.users WHERE id = p_user_id;
    SELECT COALESCE(json_agg(row_to_json(r)), '[]'::json) INTO v_result
    FROM (
        SELECT q.id AS quiz_id, q.title, NULL::text AS description, q.question_count,
            CEIL(q.time_limit_secs / 60.0)::int AS time_in_minutes,
            ch.module_id, q.chapter_id,
            EXISTS(SELECT 1 FROM public.quiz_attempts a
                   WHERE a.quiz_id = q.id AND a.user_id = p_user_id
                   AND a.created_at::date = CURRENT_DATE) AS is_done
        FROM public.daily_challenges dc
        JOIN public.quizzes q  ON q.id  = dc.quiz_id
        JOIN public.chapters ch ON ch.id = q.chapter_id
        WHERE dc.challenge_date = CURRENT_DATE AND dc.is_active AND dc.grade_id = v_grade
        ORDER BY q.sort_order
    ) r;
    RETURN v_result;
END; $$;

-- ─── 6.3  get_module_full ────────────────────────────────────────────
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
                    WHERE q.chapter_id = ch.id AND a.user_id = p_user_id)::int,
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
                WHEN ch.chapter_number = 1 THEN 'unlocked'
                WHEN EXISTS(
                    SELECT 1 FROM public.chapters prev
                    WHERE prev.module_id = ch.module_id AND prev.chapter_number = ch.chapter_number - 1
                    AND (SELECT COUNT(DISTINCT a.quiz_id) >= COUNT(DISTINCT q3.id)
                         FROM public.quizzes q3
                         LEFT JOIN public.quiz_attempts a ON a.quiz_id = q3.id AND a.user_id = p_user_id
                         WHERE q3.chapter_id = prev.id AND q3.is_active)
                ) THEN 'unlocked'
                ELSE 'locked'
            END AS state
        FROM public.chapters ch WHERE ch.module_id = p_module_id AND ch.is_active
    ) ch_row;

    RETURN json_build_object('module', v_module, 'chapters', v_chapters);
END; $$;

-- ─── 6.4  get_chapter_quizzes ────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.get_chapter_quizzes(p_chapter_id UUID, p_user_id UUID)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_result JSON;
BEGIN
    SELECT COALESCE(json_agg(quiz_json ORDER BY q.sort_order), '[]'::json) INTO v_result
    FROM public.quizzes q,
    LATERAL (
        SELECT json_build_object(
            'id', q.id, 'title', q.title, 'quiz_type', q.quiz_type,
            'question_count', q.question_count, 'time_limit_secs', q.time_limit_secs,
            'max_xp', q.max_xp, 'difficulty', q.difficulty, 'sort_order', q.sort_order,
            'best_score', (SELECT MAX(a.score) FROM public.quiz_attempts a
                          WHERE a.quiz_id = q.id AND a.user_id = p_user_id),
            'attempt_count', (SELECT COUNT(*) FROM public.quiz_attempts a
                             WHERE a.quiz_id = q.id AND a.user_id = p_user_id)::int,
            'cooldown_hours', q.cooldown_hours,
            'last_attempt_at', (SELECT MAX(a.created_at) FROM public.quiz_attempts a
                               WHERE a.quiz_id = q.id AND a.user_id = p_user_id),
            'is_locked', CASE
                WHEN q.cooldown_hours IS NULL THEN false
                WHEN NOT EXISTS(SELECT 1 FROM public.quiz_attempts a
                    WHERE a.quiz_id = q.id AND a.user_id = p_user_id) THEN false
                WHEN (SELECT MAX(a.created_at) FROM public.quiz_attempts a
                    WHERE a.quiz_id = q.id AND a.user_id = p_user_id)
                    + (q.cooldown_hours || ' hours')::interval > now() THEN true
                ELSE false END,
            'unlocks_at', CASE
                WHEN q.cooldown_hours IS NULL THEN NULL
                WHEN NOT EXISTS(SELECT 1 FROM public.quiz_attempts a
                    WHERE a.quiz_id = q.id AND a.user_id = p_user_id) THEN NULL
                ELSE (SELECT MAX(a.created_at) + (q.cooldown_hours || ' hours')::interval
                    FROM public.quiz_attempts a
                    WHERE a.quiz_id = q.id AND a.user_id = p_user_id) END,
            'questions', (
                SELECT COALESCE(json_agg(json_build_object(
                    'id', qu.id, 'question_type', qu.question_type,
                    'title', qu.title, 'prompt', qu.prompt,
                    'explanation', qu.explanation, 'difficulty', qu.difficulty,
                    'time_limit_secs', qu.time_limit_secs,
                    'allow_multiple', qu.allow_multiple,
                    'prompt_config', qu.prompt_config,
                    'metadata', qu.metadata, 'media_url', qu.media_url,
                    'options', (SELECT COALESCE(json_agg(json_build_object(
                        'id', o.id, 'label', o.label, 'is_correct', o.is_correct,
                        'sort_order', o.sort_order, 'correct_position', o.correct_position,
                        'media_url', o.media_url, 'visual_label', o.visual_label
                    ) ORDER BY o.sort_order), '[]'::json)
                    FROM public.question_options o WHERE o.question_id = qu.id),
                    'match_pairs', (SELECT json_agg(json_build_object(
                        'id', mp.id, 'left_text', mp.left_text,
                        'right_text', mp.right_text, 'sort_order', mp.sort_order
                    ) ORDER BY mp.sort_order)
                    FROM public.match_pairs mp WHERE mp.question_id = qu.id)
                ) ORDER BY qu.sort_order), '[]'::json)
                FROM public.questions qu WHERE qu.quiz_id = q.id
            )
        ) AS quiz_json
    ) sub
    WHERE q.chapter_id = p_chapter_id AND q.is_active;
    RETURN v_result;
END; $$;

-- ─── 6.5  submit_quiz_attempt ────────────────────────────────────────
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
            'is_replay',true,'next_quiz_id',NULL);
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

    SELECT level, last_active_at::date INTO v_old_level, v_last_active_date FROM public.users WHERE id = v_user_id;
    UPDATE public.users SET total_xp = total_xp + v_xp,
        level = CASE WHEN total_xp+v_xp>=5000 THEN 10 WHEN total_xp+v_xp>=4000 THEN 9 WHEN total_xp+v_xp>=3200 THEN 8
            WHEN total_xp+v_xp>=2500 THEN 7 WHEN total_xp+v_xp>=1900 THEN 6 WHEN total_xp+v_xp>=1400 THEN 5
            WHEN total_xp+v_xp>=1000 THEN 4 WHEN total_xp+v_xp>=600 THEN 3 WHEN total_xp+v_xp>=300 THEN 2 ELSE 1 END,
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
        END)
    WHERE id = v_user_id RETURNING total_xp, level INTO v_total_xp, v_new_level;

    -- Update IQ best score if this is an IQ quiz and new score > stored best
    IF EXISTS(SELECT 1 FROM public.quizzes WHERE id = v_quiz_id AND quiz_type = 'iq') THEN
        UPDATE public.users
        SET iq_best_score = GREATEST(COALESCE(iq_best_score, 0), v_score)
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
        'rank_global',v_rank,'is_replay',v_is_replay,'next_quiz_id',v_next_quiz);
END; $$;

-- ─── 6.6  get_active_tournament ──────────────────────────────────────
CREATE OR REPLACE FUNCTION public.get_active_tournament(p_user_id UUID, p_grade_id UUID)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_result JSON;
BEGIN
    SELECT json_build_object(
        'id', t.id, 'title', t.title, 'starts_at', t.starts_at, 'ends_at', t.ends_at,
        'status', t.status,
        'user_entry_status', (SELECT te.status FROM public.tournament_entries te
                             WHERE te.tournament_id = t.id AND te.user_id = p_user_id),
        'question_count', t.question_count, 'time_limit_seconds', t.time_limit_seconds,
        'participant_count', (SELECT COUNT(*) FROM public.tournament_entries te2
                             WHERE te2.tournament_id = t.id)::int
    ) INTO v_result
    FROM public.tournaments t
    WHERE t.grade_id = p_grade_id AND t.status IN ('live','scheduled') AND t.ends_at > now()
    ORDER BY t.starts_at LIMIT 1;
    RETURN v_result;
END; $$;

-- ─── 6.7  start_tournament ───────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.start_tournament(p_user_id UUID, p_tournament_id UUID)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_entry_id UUID; v_questions JSON; v_time_limit INT; v_existing TEXT;
    v_max_part INT; v_current_part INT;
BEGIN
    SELECT te.status INTO v_existing FROM public.tournament_entries te
    WHERE te.tournament_id = p_tournament_id AND te.user_id = p_user_id;
    IF v_existing IS NOT NULL AND v_existing NOT IN ('not_started') THEN
        RAISE EXCEPTION 'Already started or completed';
    END IF;

    -- Enforce max_participants limit
    SELECT max_participants, time_limit_seconds INTO v_max_part, v_time_limit
    FROM public.tournaments WHERE id = p_tournament_id;
    IF v_max_part IS NOT NULL AND v_existing IS NULL THEN
        SELECT COUNT(*) INTO v_current_part FROM public.tournament_entries
        WHERE tournament_id = p_tournament_id;
        IF v_current_part >= v_max_part THEN
            RAISE EXCEPTION 'Tournament is full (% / % participants)', v_current_part, v_max_part;
        END IF;
    END IF;
    INSERT INTO public.tournament_entries (tournament_id, user_id, status, time_remaining_secs, started_at)
    VALUES (p_tournament_id, p_user_id, 'in_progress', v_time_limit, now())
    ON CONFLICT (tournament_id, user_id) DO UPDATE SET status='in_progress', time_remaining_secs=v_time_limit, started_at=now()
    RETURNING id INTO v_entry_id;

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

-- ─── 6.8  pause_tournament ───────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.pause_tournament(p_entry_id UUID)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_result JSON;
BEGIN
    UPDATE public.tournament_entries SET status = 'paused' WHERE id = p_entry_id AND status = 'in_progress';
    SELECT row_to_json(te) INTO v_result FROM (
        SELECT id, tournament_id, user_id, status, score, time_taken_seconds,
               rank, time_remaining_secs, answers_so_far
        FROM public.tournament_entries WHERE id = p_entry_id
    ) te;
    RETURN v_result;
END; $$;

-- ─── 6.9  resume_tournament ──────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.resume_tournament(p_entry_id UUID)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_result JSON;
BEGIN
    UPDATE public.tournament_entries SET status = 'in_progress' WHERE id = p_entry_id AND status = 'paused';
    SELECT row_to_json(te) INTO v_result FROM (
        SELECT id, tournament_id, user_id, status, score, time_taken_seconds,
               rank, time_remaining_secs, answers_so_far
        FROM public.tournament_entries WHERE id = p_entry_id
    ) te;
    RETURN v_result;
END; $$;

-- ─── 6.10 submit_tournament ──────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.submit_tournament(p_entry_id UUID, p_answers JSON, p_time_taken INT)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_uid UUID; v_tid UUID; v_score INT:=0; v_total INT:=0; v_xp INT;
    v_old_level INT; v_new_level INT; v_total_xp BIGINT; v_rank INT; v_arr JSON;
BEGIN
    SELECT user_id, tournament_id INTO v_uid, v_tid FROM public.tournament_entries WHERE id = p_entry_id;
    v_arr := COALESCE(p_answers->'items', p_answers);
    SELECT COUNT(*) INTO v_total FROM json_array_elements(v_arr);
    SELECT COUNT(*) INTO v_score FROM json_array_elements(v_arr) e WHERE (e->>'is_correct')::boolean;
    v_xp := v_score * 10;

    UPDATE public.tournament_entries SET status='completed', score=v_score, time_taken_seconds=p_time_taken,
        answers_so_far=v_arr::jsonb, completed_at=now() WHERE id = p_entry_id;

    SELECT level INTO v_old_level FROM public.users WHERE id = v_uid;
    UPDATE public.users SET total_xp = total_xp + v_xp,
        level = CASE WHEN total_xp+v_xp>=5000 THEN 10 WHEN total_xp+v_xp>=4000 THEN 9 WHEN total_xp+v_xp>=3200 THEN 8
            WHEN total_xp+v_xp>=2500 THEN 7 WHEN total_xp+v_xp>=1900 THEN 6 WHEN total_xp+v_xp>=1400 THEN 5
            WHEN total_xp+v_xp>=1000 THEN 4 WHEN total_xp+v_xp>=600 THEN 3 WHEN total_xp+v_xp>=300 THEN 2 ELSE 1 END
    WHERE id = v_uid RETURNING total_xp, level INTO v_total_xp, v_new_level;

    SELECT COUNT(*)+1 INTO v_rank FROM public.tournament_entries
    WHERE tournament_id=v_tid AND score>v_score AND status IN ('completed','auto_submitted');
    UPDATE public.tournament_entries SET rank = v_rank WHERE id = p_entry_id;

    RETURN json_build_object(
        'status','success','attempt_id',p_entry_id,'score',v_score,
        'total_questions',v_total,'xp_earned',v_xp,'total_xp',v_total_xp,
        'level',v_new_level,'level_changed',(v_new_level>v_old_level),
        'rank_global',v_rank,'is_replay',false,'next_quiz_id',NULL);
END; $$;

-- ─── 6.11 submit_tournament_answer ───────────────────────────────────
CREATE OR REPLACE FUNCTION public.submit_tournament_answer(p_entry_id UUID, p_answer JSON)
RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    UPDATE public.tournament_entries
    SET answers_so_far = COALESCE(answers_so_far, '[]'::jsonb) || p_answer::jsonb
    WHERE id = p_entry_id;
END; $$;

-- ─── 6.11b get_tournament_leaderboard ─────────────────────────────────
CREATE OR REPLACE FUNCTION public.get_tournament_leaderboard(
    p_tournament_id UUID, p_user_id UUID,
    p_limit INT DEFAULT 50, p_offset INT DEFAULT 0
) RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_ranked JSON; v_my_entry JSON; v_total_participants INT;
BEGIN
    -- Ranked list of participants
    SELECT COALESCE(json_agg(row_to_json(r)), '[]'::json) INTO v_ranked
    FROM (
        SELECT u.id AS user_id, u.display_name, u.avatar_id,
            te.score, te.time_taken_seconds,
            te.status AS entry_status,
            ROW_NUMBER() OVER (ORDER BY te.score DESC, te.time_taken_seconds ASC)::int AS rank
        FROM public.tournament_entries te
        JOIN public.users u ON u.id = te.user_id
        WHERE te.tournament_id = p_tournament_id
            AND te.status IN ('completed','auto_submitted')
        ORDER BY te.score DESC, te.time_taken_seconds ASC
        LIMIT p_limit OFFSET p_offset
    ) r;

    -- Current user's entry
    SELECT json_build_object(
        'score', te.score, 'time_taken_seconds', te.time_taken_seconds,
        'status', te.status, 'rank', te.rank,
        'my_rank', (SELECT COUNT(*)+1 FROM public.tournament_entries te2
            WHERE te2.tournament_id = p_tournament_id
            AND te2.status IN ('completed','auto_submitted')
            AND (te2.score > te.score OR (te2.score = te.score AND te2.time_taken_seconds < te.time_taken_seconds)))::int
    ) INTO v_my_entry FROM public.tournament_entries te
    WHERE te.tournament_id = p_tournament_id AND te.user_id = p_user_id;

    SELECT COUNT(*) INTO v_total_participants FROM public.tournament_entries
    WHERE tournament_id = p_tournament_id;

    RETURN json_build_object(
        'ranked_users', v_ranked,
        'my_entry', v_my_entry,
        'total_participants', v_total_participants
    );
END; $$;

-- ─── 6.12 get_leaderboard ───────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.get_leaderboard(
    p_user_id UUID, p_filter TEXT DEFAULT 'global', p_filter_id UUID DEFAULT NULL,
    p_limit INT DEFAULT 50, p_offset INT DEFAULT 0
) RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_ranked JSON; v_country UUID; v_city UUID; v_my_xp BIGINT;
    v_rank_g INT; v_rank_c INT; v_rank_ci INT; v_xp_gap BIGINT;
BEGIN
    SELECT country_id, city_id, total_xp INTO v_country, v_city, v_my_xp
    FROM public.users WHERE id = p_user_id;

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

    SELECT COUNT(*)+1 INTO v_rank_g FROM public.users WHERE total_xp > v_my_xp AND NOT is_banned;
    IF v_country IS NOT NULL THEN
        SELECT COUNT(*)+1 INTO v_rank_c FROM public.users WHERE country_id=v_country AND total_xp>v_my_xp AND NOT is_banned;
    END IF;
    IF v_city IS NOT NULL THEN
        SELECT COUNT(*)+1 INTO v_rank_ci FROM public.users WHERE city_id=v_city AND total_xp>v_my_xp AND NOT is_banned;
    END IF;
    SELECT total_xp - v_my_xp INTO v_xp_gap FROM public.users WHERE total_xp > v_my_xp AND NOT is_banned ORDER BY total_xp ASC LIMIT 1;

    RETURN json_build_object('ranked_users', v_ranked,
        'user_rank', json_build_object('rank_global',v_rank_g,'rank_country',v_rank_c,'rank_city',v_rank_ci,'xp_gap',COALESCE(v_xp_gap,0)));
END; $$;

-- ─── 6.13 get_user_stats ────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.get_user_stats(p_user_id UUID, p_period TEXT DEFAULT 'week')
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_stats JSON; v_acc DOUBLE PRECISION; v_daily JSON; v_subj JSON; v_since TIMESTAMPTZ;
BEGIN
    v_since := CASE WHEN p_period='week' THEN now()-'7 days'::interval
        WHEN p_period='month' THEN now()-'30 days'::interval ELSE '1970-01-01'::timestamptz END;

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

    SELECT COALESCE(json_agg(row_to_json(s)),'[]'::json) INTO v_subj
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
        COUNT(DISTINCT ch.id)::int AS total_chapters
        FROM public.modules m JOIN public.chapters ch ON ch.module_id=m.id AND ch.is_active
        LEFT JOIN public.quizzes q ON q.chapter_id=ch.id AND q.is_active
        LEFT JOIN public.quiz_attempts a ON a.quiz_id=q.id AND a.user_id=p_user_id AND a.created_at>=v_since
        WHERE m.is_active GROUP BY m.id, m.title, m.emoji) s;

    RETURN json_build_object('stats',v_stats,'accuracy_pct',v_acc,'daily_activity',v_daily,'subject_performance',v_subj);
END; $$;

-- ─── 6.14 get_profile ───────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.get_profile(p_user_id UUID)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_user JSON; v_stats JSON; v_chapters JSON; v_tourns JSON;
BEGIN
    SELECT json_build_object(
        'id', u.id, 'display_name', u.display_name, 'avatar_id', u.avatar_id,
        'grade_id', u.grade_id, 'grade_label', COALESCE(g.label,''),
        'auth_provider', u.auth_provider, 'country_name', co.name, 'city_name', ci.name, 'school_name', u.school_name
    ) INTO v_user FROM public.users u LEFT JOIN public.grades g ON g.id=u.grade_id
    LEFT JOIN public.countries co ON co.id=u.country_id LEFT JOIN public.cities ci ON ci.id=u.city_id
    WHERE u.id = p_user_id;
    IF v_user IS NULL THEN RAISE EXCEPTION 'User not found: %', p_user_id; END IF;

    SELECT json_build_object(
        'total_xp', u.total_xp, 'level', u.level, 'streak_current', u.streak_current, 'streak_best', u.streak_best,
        'quizzes_completed', (SELECT COUNT(*) FROM public.quiz_attempts WHERE user_id=p_user_id),
        'accuracy_pct', COALESCE((SELECT ROUND(AVG(score::numeric/NULLIF(total_questions,0)*100),1) FROM public.quiz_attempts WHERE user_id=p_user_id),0),
        'tournaments_played', (SELECT COUNT(*) FROM public.tournament_entries WHERE user_id=p_user_id AND status IN ('completed','auto_submitted')),
        'best_tournament_rank', (SELECT MIN(rank) FROM public.tournament_entries WHERE user_id=p_user_id AND rank IS NOT NULL),
        'iq_best_score', u.iq_best_score
    ) INTO v_stats FROM public.users u WHERE u.id = p_user_id;

    SELECT COALESCE(json_agg(row_to_json(cc) ORDER BY cc.completed_at DESC),'[]'::json) INTO v_chapters
    FROM (SELECT DISTINCT ON (ch.id) ch.id AS chapter_id, ch.title AS chapter_title,
        m.title AS module_title, m.emoji AS module_emoji, a.created_at::text AS completed_at
        FROM public.chapters ch JOIN public.modules m ON m.id=ch.module_id
        JOIN public.quizzes q ON q.chapter_id=ch.id AND q.is_active
        JOIN public.quiz_attempts a ON a.quiz_id=q.id AND a.user_id=p_user_id
        WHERE (SELECT COUNT(DISTINCT a2.quiz_id) FROM public.quiz_attempts a2
            JOIN public.quizzes q2 ON q2.id=a2.quiz_id WHERE q2.chapter_id=ch.id AND a2.user_id=p_user_id)
            >= (SELECT COUNT(*) FROM public.quizzes q3 WHERE q3.chapter_id=ch.id AND q3.is_active)
        ORDER BY ch.id, a.created_at DESC) cc;

    SELECT COALESCE(json_agg(row_to_json(tr) ORDER BY tr.date DESC),'[]'::json) INTO v_tourns
    FROM (SELECT t.id AS tournament_id, t.title, te.score, t.question_count AS total_questions,
        COALESCE(te.rank,0) AS rank,
        (SELECT COUNT(*) FROM public.tournament_entries te2 WHERE te2.tournament_id=t.id)::int AS participant_count,
        NULL::text AS certificate_url, te.completed_at::text AS date
        FROM public.tournament_entries te JOIN public.tournaments t ON t.id=te.tournament_id
        WHERE te.user_id=p_user_id AND te.status IN ('completed','auto_submitted')) tr;

    RETURN json_build_object('user',v_user,'stats',v_stats,'completed_chapters',v_chapters,'tournament_results',v_tourns);
END; $$;

-- ─── 6.15 update_profile ────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.update_profile(p_user_id UUID, p_fields JSON)
RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_key TEXT; v_value TEXT;
    v_sql TEXT := 'UPDATE public.users SET updated_at = now()';
    v_allowed TEXT[] := ARRAY['display_name','avatar_id','grade_id','school_name','country_id','city_id'];
BEGIN
    FOR v_key, v_value IN SELECT * FROM json_each_text(p_fields) LOOP
        IF v_key = ANY(v_allowed) THEN v_sql := v_sql || format(', %I = %L', v_key, v_value); END IF;
    END LOOP;
    v_sql := v_sql || format(' WHERE id = %L', p_user_id);
    EXECUTE v_sql;
END; $$;


-- ═══════════════════════════════════════════════════════════════════════
-- PART 7: CMS/ADMIN RPC FUNCTIONS (15 — admin panel uses these)
-- ═══════════════════════════════════════════════════════════════════════

-- ─── 7.1  admin_dashboard_stats ──────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_dashboard_stats()
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_result JSON;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    SELECT json_build_object(
        'total_users',     (SELECT COUNT(*) FROM public.users WHERE NOT is_banned),
        'total_modules',   (SELECT COUNT(*) FROM public.modules),
        'total_chapters',  (SELECT COUNT(*) FROM public.chapters),
        'total_quizzes',   (SELECT COUNT(*) FROM public.quizzes),
        'total_questions', (SELECT COUNT(*) FROM public.questions),
        'total_attempts',  (SELECT COUNT(*) FROM public.quiz_attempts),
        'active_tournaments', (SELECT COUNT(*) FROM public.tournaments WHERE status IN ('live','scheduled')),
        'users_today',     (SELECT COUNT(*) FROM public.users WHERE created_at::date = CURRENT_DATE),
        'attempts_today',  (SELECT COUNT(*) FROM public.quiz_attempts WHERE created_at::date = CURRENT_DATE),
        'banned_users',    (SELECT COUNT(*) FROM public.users WHERE is_banned)
    ) INTO v_result;
    RETURN v_result;
END; $$;

-- ─── 7.2  admin_get_users ────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_get_users(
    p_search TEXT DEFAULT NULL, p_grade_id UUID DEFAULT NULL,
    p_country_id UUID DEFAULT NULL, p_include_banned BOOLEAN DEFAULT false,
    p_limit INT DEFAULT 50, p_offset INT DEFAULT 0
) RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_result JSON; v_total INT;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;

    SELECT COUNT(*) INTO v_total FROM public.users u
    WHERE (p_include_banned OR NOT u.is_banned)
      AND (p_grade_id IS NULL OR u.grade_id = p_grade_id)
      AND (p_country_id IS NULL OR u.country_id = p_country_id)
      AND (p_search IS NULL OR u.display_name ILIKE '%' || p_search || '%');

    SELECT json_build_object('total', v_total, 'users',
        COALESCE(json_agg(row_to_json(r) ORDER BY r.created_at DESC), '[]'::json)
    ) INTO v_result FROM (
        SELECT u.id, u.display_name, u.avatar_id, u.auth_provider,
            g.label AS grade_label, co.name AS country_name, u.school_name,
            u.total_xp, u.level, u.is_banned, u.last_active_at, u.created_at,
            (SELECT COUNT(*) FROM public.quiz_attempts WHERE user_id = u.id)::int AS quiz_count
        FROM public.users u
        LEFT JOIN public.grades g ON g.id = u.grade_id
        LEFT JOIN public.countries co ON co.id = u.country_id
        WHERE (p_include_banned OR NOT u.is_banned)
          AND (p_grade_id IS NULL OR u.grade_id = p_grade_id)
          AND (p_country_id IS NULL OR u.country_id = p_country_id)
          AND (p_search IS NULL OR u.display_name ILIKE '%' || p_search || '%')
        ORDER BY u.created_at DESC LIMIT p_limit OFFSET p_offset
    ) r;
    RETURN v_result;
END; $$;

-- ─── 7.3  admin_ban_user ─────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_ban_user(p_user_id UUID, p_reason TEXT)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    UPDATE public.users SET is_banned = true, banned_reason = p_reason, banned_at = now()
    WHERE id = p_user_id;
    INSERT INTO public.audit_logs (admin_id, action, table_name, record_id, new_values)
    VALUES (auth.uid(), 'ban', 'users', p_user_id, jsonb_build_object('reason', p_reason));
    RETURN json_build_object('status', 'success');
END; $$;

-- ─── 7.4  admin_unban_user ───────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_unban_user(p_user_id UUID)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    UPDATE public.users SET is_banned = false, banned_reason = NULL, banned_at = NULL
    WHERE id = p_user_id;
    INSERT INTO public.audit_logs (admin_id, action, table_name, record_id)
    VALUES (auth.uid(), 'unban', 'users', p_user_id);
    RETURN json_build_object('status', 'success');
END; $$;

-- ─── 7.5  admin_upsert_module ────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_upsert_module(p_data JSON)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_id UUID; v_action TEXT;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    v_id := (p_data->>'id')::uuid;
    IF v_id IS NOT NULL AND EXISTS(SELECT 1 FROM public.modules WHERE id = v_id) THEN
        v_action := 'update';
        UPDATE public.modules SET
            title         = COALESCE(p_data->>'title', title),
            subtitle      = COALESCE(p_data->>'subtitle', subtitle),
            description   = COALESCE(p_data->>'description', description),
            emoji         = COALESCE(p_data->>'emoji', emoji),
            accent_color  = COALESCE(p_data->>'accent_color', accent_color),
            thumbnail_url = COALESCE(p_data->>'thumbnail_url', thumbnail_url),
            grade_id      = COALESCE((p_data->>'grade_id')::uuid, grade_id),
            display_order = COALESCE((p_data->>'display_order')::int, display_order),
            is_active     = COALESCE((p_data->>'is_active')::boolean, is_active),
            status        = COALESCE(p_data->>'status', status),
            updated_by    = auth.uid(), updated_at = now()
        WHERE id = v_id;
    ELSE
        v_action := 'create';
        INSERT INTO public.modules (title, subtitle, description, emoji, accent_color, thumbnail_url, grade_id,
            display_order, is_active, status, created_by, updated_by)
        VALUES (p_data->>'title', p_data->>'subtitle', p_data->>'description',
            COALESCE(p_data->>'emoji','📚'), COALESCE(p_data->>'accent_color','#4F46E5'),
            p_data->>'thumbnail_url', (p_data->>'grade_id')::uuid,
            COALESCE((p_data->>'display_order')::int, (SELECT COALESCE(MAX(display_order),0)+1 FROM public.modules)),
            COALESCE((p_data->>'is_active')::boolean, true), 'draft', auth.uid(), auth.uid())
        RETURNING id INTO v_id;
    END IF;

    INSERT INTO public.audit_logs (admin_id, action, table_name, record_id, new_values)
    VALUES (auth.uid(), v_action, 'modules', v_id, p_data::jsonb);
    RETURN json_build_object('id', v_id, 'action', v_action);
END; $$;

-- ─── 7.6  admin_upsert_chapter ───────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_upsert_chapter(p_data JSON)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_id UUID; v_action TEXT;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    v_id := (p_data->>'id')::uuid;
    IF v_id IS NOT NULL AND EXISTS(SELECT 1 FROM public.chapters WHERE id = v_id) THEN
        v_action := 'update';
        UPDATE public.chapters SET
            title          = COALESCE(p_data->>'title', title),
            description    = COALESCE(p_data->>'description', description),
            chapter_number = COALESCE((p_data->>'chapter_number')::int, chapter_number),
            sort_order     = COALESCE((p_data->>'sort_order')::int, sort_order),
            is_active      = COALESCE((p_data->>'is_active')::boolean, is_active),
            status         = COALESCE(p_data->>'status', status),
            updated_by     = auth.uid(), updated_at = now()
        WHERE id = v_id;
    ELSE
        v_action := 'create';
        INSERT INTO public.chapters (module_id, title, description, chapter_number, sort_order, status, created_by, updated_by)
        VALUES ((p_data->>'module_id')::uuid, p_data->>'title', p_data->>'description',
            COALESCE((p_data->>'chapter_number')::int, 1), COALESCE((p_data->>'sort_order')::int, 0),
            'draft', auth.uid(), auth.uid())
        RETURNING id INTO v_id;
    END IF;

    INSERT INTO public.audit_logs (admin_id, action, table_name, record_id, new_values)
    VALUES (auth.uid(), v_action, 'chapters', v_id, p_data::jsonb);
    RETURN json_build_object('id', v_id, 'action', v_action);
END; $$;

-- ─── 7.7  admin_upsert_quiz ─────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_upsert_quiz(p_data JSON)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_id UUID; v_action TEXT;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    v_id := (p_data->>'id')::uuid;
    IF v_id IS NOT NULL AND EXISTS(SELECT 1 FROM public.quizzes WHERE id = v_id) THEN
        v_action := 'update';
        UPDATE public.quizzes SET
            title            = COALESCE(p_data->>'title', title),
            description      = COALESCE(p_data->>'description', description),
            quiz_type        = COALESCE(p_data->>'quiz_type', quiz_type),
            question_count   = COALESCE((p_data->>'question_count')::int, question_count),
            time_limit_secs  = COALESCE((p_data->>'time_limit_secs')::int, time_limit_secs),
            max_xp           = COALESCE((p_data->>'max_xp')::int, max_xp),
            difficulty       = COALESCE(p_data->>'difficulty', difficulty),
            passing_score_pct = COALESCE((p_data->>'passing_score_pct')::int, passing_score_pct),
            shuffle_options  = COALESCE((p_data->>'shuffle_options')::boolean, shuffle_options),
            show_explanation = COALESCE((p_data->>'show_explanation')::boolean, show_explanation),
            sort_order       = COALESCE((p_data->>'sort_order')::int, sort_order),
            is_active        = COALESCE((p_data->>'is_active')::boolean, is_active),
            status           = COALESCE(p_data->>'status', status),
            updated_by       = auth.uid(), updated_at = now()
        WHERE id = v_id;
    ELSE
        v_action := 'create';
        INSERT INTO public.quizzes (chapter_id, title, description, quiz_type, question_count, time_limit_secs,
            max_xp, difficulty, sort_order, status, created_by, updated_by)
        VALUES ((p_data->>'chapter_id')::uuid, p_data->>'title', p_data->>'description',
            COALESCE(p_data->>'quiz_type','practice'), COALESCE((p_data->>'question_count')::int,0),
            COALESCE((p_data->>'time_limit_secs')::int,300), COALESCE((p_data->>'max_xp')::int,100),
            p_data->>'difficulty', COALESCE((p_data->>'sort_order')::int,0), 'draft', auth.uid(), auth.uid())
        RETURNING id INTO v_id;
    END IF;

    INSERT INTO public.audit_logs (admin_id, action, table_name, record_id, new_values)
    VALUES (auth.uid(), v_action, 'quizzes', v_id, p_data::jsonb);
    RETURN json_build_object('id', v_id, 'action', v_action);
END; $$;

-- ─── 7.8  admin_upsert_question (with options + match_pairs) ─────────
CREATE OR REPLACE FUNCTION public.admin_upsert_question(p_data JSON)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_id UUID; v_action TEXT; v_opt JSON; v_mp JSON;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    v_id := (p_data->>'id')::uuid;
    IF v_id IS NOT NULL AND EXISTS(SELECT 1 FROM public.questions WHERE id = v_id) THEN
        v_action := 'update';
        UPDATE public.questions SET
            title           = COALESCE(p_data->>'title', title),
            question_type   = COALESCE(p_data->>'question_type', question_type),
            prompt          = COALESCE(p_data->>'prompt', prompt),
            explanation     = COALESCE(p_data->>'explanation', explanation),
            difficulty      = COALESCE(p_data->>'difficulty', difficulty),
            time_limit_secs = COALESCE((p_data->>'time_limit_secs')::int, time_limit_secs),
            allow_multiple  = COALESCE((p_data->>'allow_multiple')::boolean, allow_multiple),
            prompt_config   = COALESCE((p_data->>'prompt_config')::jsonb, prompt_config),
            metadata        = COALESCE((p_data->>'metadata')::jsonb, metadata),
            media_url       = COALESCE(p_data->>'media_url', media_url),
            sort_order      = COALESCE((p_data->>'sort_order')::int, sort_order),
            status          = COALESCE(p_data->>'status', status),
            updated_by      = auth.uid(), updated_at = now()
        WHERE id = v_id;
    ELSE
        v_action := 'create';
        INSERT INTO public.questions (quiz_id, title, question_type, prompt, explanation, difficulty,
            time_limit_secs, allow_multiple, prompt_config, metadata, media_url, sort_order, status, created_by, updated_by)
        VALUES ((p_data->>'quiz_id')::uuid, p_data->>'title',
            COALESCE(p_data->>'question_type','multiple_choice'), p_data->>'prompt',
            COALESCE(p_data->>'explanation',''), p_data->>'difficulty',
            (p_data->>'time_limit_secs')::int, COALESCE((p_data->>'allow_multiple')::boolean, false),
            (p_data->>'prompt_config')::jsonb, (p_data->>'metadata')::jsonb,
            p_data->>'media_url', COALESCE((p_data->>'sort_order')::int,0), 'draft', auth.uid(), auth.uid())
        RETURNING id INTO v_id;
    END IF;

    -- Replace options if provided
    IF p_data->'options' IS NOT NULL THEN
        DELETE FROM public.question_options WHERE question_id = v_id;
        FOR v_opt IN SELECT * FROM json_array_elements(p_data->'options') LOOP
            INSERT INTO public.question_options (question_id, label, is_correct, sort_order,
                correct_position, media_url, visual_label)
            VALUES (v_id, v_opt->>'label', COALESCE((v_opt->>'is_correct')::boolean, false),
                COALESCE((v_opt->>'sort_order')::int, 0), (v_opt->>'correct_position')::int,
                v_opt->>'media_url', v_opt->>'visual_label');
        END LOOP;
    END IF;

    -- Replace match_pairs if provided
    IF p_data->'match_pairs' IS NOT NULL THEN
        DELETE FROM public.match_pairs WHERE question_id = v_id;
        FOR v_mp IN SELECT * FROM json_array_elements(p_data->'match_pairs') LOOP
            INSERT INTO public.match_pairs (question_id, left_text, right_text, sort_order)
            VALUES (v_id, v_mp->>'left_text', v_mp->>'right_text', COALESCE((v_mp->>'sort_order')::int, 0));
        END LOOP;
    END IF;

    INSERT INTO public.audit_logs (admin_id, action, table_name, record_id, new_values)
    VALUES (auth.uid(), v_action, 'questions', v_id, p_data::jsonb);
    RETURN json_build_object('id', v_id, 'action', v_action);
END; $$;

-- ─── 7.9  admin_delete_content ───────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_delete_content(p_table TEXT, p_id UUID)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_allowed TEXT[] := ARRAY['modules','chapters','quizzes','questions',
    'question_options','match_pairs','daily_challenges','tournament_questions'];
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    IF NOT (p_table = ANY(v_allowed)) THEN RAISE EXCEPTION 'Table not allowed: %', p_table; END IF;

    INSERT INTO public.audit_logs (admin_id, action, table_name, record_id)
    VALUES (auth.uid(), 'delete', p_table, p_id);

    EXECUTE format('DELETE FROM public.%I WHERE id = %L', p_table, p_id);
    RETURN json_build_object('status', 'success', 'deleted', p_table);
END; $$;

-- ─── 7.10 admin_publish_content ──────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_publish_content(p_type TEXT, p_id UUID)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;

    CASE p_type
        WHEN 'module' THEN UPDATE public.modules SET status='published', is_active=true, published_at=now(), updated_by=auth.uid() WHERE id = p_id;
        WHEN 'chapter' THEN UPDATE public.chapters SET status='published', is_active=true, published_at=now(), updated_by=auth.uid() WHERE id = p_id;
        WHEN 'quiz' THEN UPDATE public.quizzes SET status='published', is_active=true, published_at=now(), updated_by=auth.uid() WHERE id = p_id;
        WHEN 'question' THEN UPDATE public.questions SET status='published', updated_by=auth.uid() WHERE id = p_id;
        ELSE RAISE EXCEPTION 'Unknown type: %', p_type;
    END CASE;

    INSERT INTO public.content_status_history (content_type, content_id, status, changed_by)
    VALUES (p_type, p_id, 'published', auth.uid());

    INSERT INTO public.audit_logs (admin_id, action, table_name, record_id)
    VALUES (auth.uid(), 'publish', p_type || 's', p_id);

    RETURN json_build_object('status', 'published');
END; $$;

-- ─── 7.11 admin_archive_content ──────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_archive_content(p_type TEXT, p_id UUID)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;

    CASE p_type
        WHEN 'module' THEN UPDATE public.modules SET status='archived', is_active=false, updated_by=auth.uid() WHERE id = p_id;
        WHEN 'chapter' THEN UPDATE public.chapters SET status='archived', is_active=false, updated_by=auth.uid() WHERE id = p_id;
        WHEN 'quiz' THEN UPDATE public.quizzes SET status='archived', is_active=false, updated_by=auth.uid() WHERE id = p_id;
        WHEN 'question' THEN UPDATE public.questions SET status='archived', updated_by=auth.uid() WHERE id = p_id;
        ELSE RAISE EXCEPTION 'Unknown type: %', p_type;
    END CASE;

    INSERT INTO public.content_status_history (content_type, content_id, status, changed_by)
    VALUES (p_type, p_id, 'archived', auth.uid());

    INSERT INTO public.audit_logs (admin_id, action, table_name, record_id)
    VALUES (auth.uid(), 'archive', p_type || 's', p_id);

    RETURN json_build_object('status', 'archived');
END; $$;

-- ─── 7.12 admin_reorder ─────────────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_reorder(p_table TEXT, p_order JSON)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_item JSON; v_col TEXT;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    v_col := CASE WHEN p_table = 'modules' THEN 'display_order' ELSE 'sort_order' END;
    FOR v_item IN SELECT * FROM json_array_elements(p_order) LOOP
        EXECUTE format('UPDATE public.%I SET %I = %s WHERE id = %L',
            p_table, v_col, (v_item->>'order')::int, (v_item->>'id')::uuid);
    END LOOP;
    RETURN json_build_object('status', 'success', 'reordered', p_table);
END; $$;

-- ─── 7.13 admin_upsert_tournament ────────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_upsert_tournament(p_data JSON)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_id UUID; v_action TEXT;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    v_id := (p_data->>'id')::uuid;
    IF v_id IS NOT NULL AND EXISTS(SELECT 1 FROM public.tournaments WHERE id = v_id) THEN
        v_action := 'update';
        UPDATE public.tournaments SET
            title              = COALESCE(p_data->>'title', title),
            description        = COALESCE(p_data->>'description', description),
            grade_id           = COALESCE((p_data->>'grade_id')::uuid, grade_id),
            question_count     = COALESCE((p_data->>'question_count')::int, question_count),
            time_limit_seconds = COALESCE((p_data->>'time_limit_seconds')::int, time_limit_seconds),
            max_participants   = COALESCE((p_data->>'max_participants')::int, max_participants),
            starts_at          = COALESCE((p_data->>'starts_at')::timestamptz, starts_at),
            ends_at            = COALESCE((p_data->>'ends_at')::timestamptz, ends_at),
            status             = COALESCE(p_data->>'status', status),
            updated_by         = auth.uid(), updated_at = now()
        WHERE id = v_id;
    ELSE
        v_action := 'create';
        INSERT INTO public.tournaments (title, description, grade_id, question_count, time_limit_seconds,
            max_participants, starts_at, ends_at, status, created_by, updated_by)
        VALUES (p_data->>'title', p_data->>'description', (p_data->>'grade_id')::uuid,
            COALESCE((p_data->>'question_count')::int,10), COALESCE((p_data->>'time_limit_seconds')::int,600),
            (p_data->>'max_participants')::int, (p_data->>'starts_at')::timestamptz, (p_data->>'ends_at')::timestamptz,
            'draft', auth.uid(), auth.uid())
        RETURNING id INTO v_id;
    END IF;

    INSERT INTO public.audit_logs (admin_id, action, table_name, record_id, new_values)
    VALUES (auth.uid(), v_action, 'tournaments', v_id, p_data::jsonb);
    RETURN json_build_object('id', v_id, 'action', v_action);
END; $$;

-- ─── 7.14 admin_set_tournament_questions ─────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_set_tournament_questions(p_tournament_id UUID, p_question_ids JSON)
RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_qid UUID; v_idx INT := 0;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    DELETE FROM public.tournament_questions WHERE tournament_id = p_tournament_id;
    FOR v_qid IN SELECT (value::text)::uuid FROM json_array_elements_text(p_question_ids) LOOP
        INSERT INTO public.tournament_questions (tournament_id, question_id, sort_order)
        VALUES (p_tournament_id, v_qid, v_idx);
        v_idx := v_idx + 1;
    END LOOP;
    UPDATE public.tournaments SET question_count = v_idx, updated_by = auth.uid(), updated_at = now()
    WHERE id = p_tournament_id;

    INSERT INTO public.audit_logs (admin_id, action, table_name, record_id, new_values)
    VALUES (auth.uid(), 'update', 'tournament_questions', p_tournament_id,
        jsonb_build_object('question_count', v_idx));
    RETURN json_build_object('status', 'success', 'question_count', v_idx);
END; $$;

-- ─── 7.15 admin_set_daily_challenge ──────────────────────────────────
CREATE OR REPLACE FUNCTION public.admin_set_daily_challenge(
    p_quiz_id UUID, p_grade_id UUID, p_date DATE DEFAULT CURRENT_DATE
) RETURNS JSON LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE v_id UUID;
BEGIN
    IF NOT public.is_admin() THEN RAISE EXCEPTION 'Unauthorized'; END IF;
    INSERT INTO public.daily_challenges (quiz_id, grade_id, challenge_date, created_by)
    VALUES (p_quiz_id, p_grade_id, p_date, auth.uid())
    ON CONFLICT (quiz_id, challenge_date) DO UPDATE SET grade_id = p_grade_id, is_active = true
    RETURNING id INTO v_id;

    INSERT INTO public.audit_logs (admin_id, action, table_name, record_id, new_values)
    VALUES (auth.uid(), 'create', 'daily_challenges', v_id,
        jsonb_build_object('quiz_id', p_quiz_id, 'grade_id', p_grade_id, 'date', p_date));
    RETURN json_build_object('id', v_id, 'status', 'success');
END; $$;


-- ═══════════════════════════════════════════════════════════════════════
-- PART 7B: ADDITIONAL CMS RPCs (15)
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
-- PART 8: GRANTS
-- ═══════════════════════════════════════════════════════════════════════

GRANT USAGE ON SCHEMA public TO anon, authenticated;

-- Reference tables: readable by all (including anon for pre-login grade picker)
GRANT SELECT ON public.grades, public.countries, public.cities TO anon, authenticated;
GRANT INSERT, UPDATE, DELETE ON public.grades, public.countries, public.cities TO authenticated;

-- Users
GRANT SELECT, INSERT, UPDATE ON public.users TO authenticated;

-- Admin users
GRANT SELECT, INSERT, UPDATE ON public.admin_users TO authenticated;

-- Content tables: read for all, write for admin (enforced by RLS)
GRANT SELECT, INSERT, UPDATE, DELETE ON public.modules, public.chapters, public.quizzes,
    public.questions, public.question_options, public.match_pairs,
    public.daily_challenges, public.tournaments, public.tournament_questions
TO authenticated;

-- User-generated data
GRANT SELECT, INSERT ON public.quiz_attempts TO authenticated;
GRANT SELECT, INSERT, UPDATE ON public.tournament_entries TO authenticated;

-- CMS tables
GRANT SELECT, INSERT ON public.audit_logs TO authenticated;
GRANT SELECT, INSERT, DELETE ON public.media_uploads TO authenticated;
GRANT SELECT, INSERT ON public.content_status_history TO authenticated;

-- App RPCs (15)
GRANT EXECUTE ON FUNCTION public.get_user_dashboard(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_daily_challenges(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_module_full(UUID, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_chapter_quizzes(UUID, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.submit_quiz_attempt(JSON) TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_active_tournament(UUID, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.start_tournament(UUID, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.pause_tournament(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.resume_tournament(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.submit_tournament(UUID, JSON, INT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.submit_tournament_answer(UUID, JSON) TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_tournament_leaderboard(UUID, UUID, INT, INT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_leaderboard(UUID, TEXT, UUID, INT, INT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_user_stats(UUID, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_profile(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.update_profile(UUID, JSON) TO authenticated;

-- CMS RPCs (15)
GRANT EXECUTE ON FUNCTION public.is_admin() TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_dashboard_stats() TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_get_users(TEXT, UUID, UUID, BOOLEAN, INT, INT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_ban_user(UUID, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_unban_user(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_upsert_module(JSON) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_upsert_chapter(JSON) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_upsert_quiz(JSON) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_upsert_question(JSON) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_delete_content(TEXT, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_publish_content(TEXT, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_archive_content(TEXT, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_reorder(TEXT, JSON) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_upsert_tournament(JSON) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_set_tournament_questions(UUID, JSON) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_set_daily_challenge(UUID, UUID, DATE) TO authenticated;

-- Additional CMS RPCs (15)
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
-- PART 9: SEED FIRST SUPER ADMIN (run after your first Google sign-in)
-- ═══════════════════════════════════════════════════════════════════════
-- Replace YOUR_AUTH_USER_ID with your actual auth.users id from Supabase Dashboard → Auth → Users
--
-- INSERT INTO public.admin_users (id, display_name, email, role)
-- VALUES ('YOUR_AUTH_USER_ID', 'Your Name', 'your@email.com', 'super_admin');


-- ═══════════════════════════════════════════════════════════════════════
-- PART 10: DASHBOARD-ONLY CONFIG (NOT SQL — do these manually)
-- ═══════════════════════════════════════════════════════════════════════
-- 1. Auth → Providers → Google   → Enable + Client ID + Secret
-- 2. Auth → Providers → Phone    → Enable + SMS provider (Twilio etc.)
-- 3. Auth → Settings             → Enable "Allow anonymous sign-ins"
-- 4. Auth → URL Config           → Add app deep link / callback URLs
-- 5. Storage → Create bucket "media" → Enable for admin uploads
-- ═══════════════════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════════════════
-- PART 11: SAMPLE CONTENT DATA (for testing all features end-to-end)
-- ═══════════════════════════════════════════════════════════════════════

-- ─── Module 1: Mathematics ───────────────────────────────────────────
INSERT INTO public.modules (id, title, subtitle, description, emoji, accent_color, display_order, is_active, status)
VALUES ('a1000000-0000-0000-0000-000000000001', 'Mathematics', 'Numbers, shapes and logic',
    'Build strong foundations in mathematics through interactive quizzes',
    '🧮', '#4F46E5', 1, true, 'published')
ON CONFLICT (id) DO NOTHING;

-- ─── Module 2: Science ───────────────────────────────────────────────
INSERT INTO public.modules (id, title, subtitle, description, emoji, accent_color, display_order, is_active, status)
VALUES ('a1000000-0000-0000-0000-000000000002', 'Science', 'Explore the natural world',
    'Discover physics, chemistry and biology through fun quizzes',
    '🔬', '#059669', 2, true, 'published')
ON CONFLICT (id) DO NOTHING;

-- ─── Module 3: English ───────────────────────────────────────────────
INSERT INTO public.modules (id, title, subtitle, description, emoji, accent_color, display_order, is_active, status)
VALUES ('a1000000-0000-0000-0000-000000000003', 'English', 'Language and literature',
    'Improve your grammar, vocabulary and comprehension skills',
    '📖', '#DC2626', 3, true, 'published')
ON CONFLICT (id) DO NOTHING;

-- ─── Chapters for Mathematics ────────────────────────────────────────
INSERT INTO public.chapters (id, module_id, title, description, chapter_number, sort_order, is_active, status) VALUES
('b1000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001',
    'Addition & Subtraction', 'Master basic arithmetic operations', 1, 1, true, 'published'),
('b1000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000001',
    'Multiplication & Division', 'Learn to multiply and divide', 2, 2, true, 'published'),
('b1000000-0000-0000-0000-000000000003', 'a1000000-0000-0000-0000-000000000001',
    'Fractions', 'Understand parts of a whole', 3, 3, true, 'published')
ON CONFLICT (id) DO NOTHING;

-- ─── Chapters for Science ────────────────────────────────────────────
INSERT INTO public.chapters (id, module_id, title, description, chapter_number, sort_order, is_active, status) VALUES
('b1000000-0000-0000-0000-000000000004', 'a1000000-0000-0000-0000-000000000002',
    'Living Things', 'Plants, animals and ecosystems', 1, 1, true, 'published'),
('b1000000-0000-0000-0000-000000000005', 'a1000000-0000-0000-0000-000000000002',
    'Matter & Materials', 'Solids, liquids and gases', 2, 2, true, 'published')
ON CONFLICT (id) DO NOTHING;

-- ─── Chapters for English ────────────────────────────────────────────
INSERT INTO public.chapters (id, module_id, title, description, chapter_number, sort_order, is_active, status) VALUES
('b1000000-0000-0000-0000-000000000006', 'a1000000-0000-0000-0000-000000000003',
    'Grammar Basics', 'Nouns, verbs and adjectives', 1, 1, true, 'published'),
('b1000000-0000-0000-0000-000000000007', 'a1000000-0000-0000-0000-000000000003',
    'Vocabulary Builder', 'Expand your word power', 2, 2, true, 'published')
ON CONFLICT (id) DO NOTHING;

-- ─── Quiz 1: Addition Quiz (Chapter: Addition & Subtraction) ─────────
INSERT INTO public.quizzes (id, chapter_id, title, quiz_type, question_count, time_limit_secs, max_xp, difficulty, sort_order, is_active, status) VALUES
('c1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001',
    'Quick Addition', 'practice', 5, 120, 50, 'easy', 1, true, 'published'),
('c1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000001',
    'Subtraction Challenge', 'practice', 5, 120, 50, 'easy', 2, true, 'published'),
('c1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000002',
    'Times Tables', 'practice', 5, 180, 75, 'medium', 1, true, 'published'),
('c1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000004',
    'Plant Life', 'practice', 5, 150, 60, 'easy', 1, true, 'published'),
('c1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000005',
    'States of Matter', 'practice', 5, 150, 60, 'easy', 1, true, 'published'),
('c1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000006',
    'Parts of Speech', 'practice', 5, 150, 60, 'easy', 1, true, 'published'),
('c1000000-0000-0000-0000-000000000007', 'b1000000-0000-0000-0000-000000000007',
    'Word Meanings', 'practice', 5, 150, 60, 'medium', 1, true, 'published')
ON CONFLICT (id) DO NOTHING;

-- ─── Questions for Quiz 1: Quick Addition ────────────────────────────
INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status) VALUES
('d1000000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000001',
    'multiple_choice', 'What is 7 + 5?', 'Calculate the sum', '7 + 5 = 12', 'easy', 1, 'published'),
('d1000000-0000-0000-0000-000000000002', 'c1000000-0000-0000-0000-000000000001',
    'multiple_choice', 'What is 15 + 8?', 'Calculate the sum', '15 + 8 = 23', 'easy', 2, 'published'),
('d1000000-0000-0000-0000-000000000003', 'c1000000-0000-0000-0000-000000000001',
    'multiple_choice', 'What is 23 + 19?', 'Calculate the sum', '23 + 19 = 42', 'easy', 3, 'published'),
('d1000000-0000-0000-0000-000000000004', 'c1000000-0000-0000-0000-000000000001',
    'true_false', '10 + 10 = 20', 'Is this correct?', '10 + 10 is indeed 20', 'easy', 4, 'published'),
('d1000000-0000-0000-0000-000000000005', 'c1000000-0000-0000-0000-000000000001',
    'multiple_choice', 'What is 45 + 37?', 'Calculate the sum', '45 + 37 = 82', 'medium', 5, 'published')
ON CONFLICT (id) DO NOTHING;

-- Options for Q1: 7 + 5
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('e1000000-0000-0000-0000-000000000001', 'd1000000-0000-0000-0000-000000000001', '10', false, 1),
('e1000000-0000-0000-0000-000000000002', 'd1000000-0000-0000-0000-000000000001', '12', true,  2),
('e1000000-0000-0000-0000-000000000003', 'd1000000-0000-0000-0000-000000000001', '11', false, 3),
('e1000000-0000-0000-0000-000000000004', 'd1000000-0000-0000-0000-000000000001', '13', false, 4)
ON CONFLICT (id) DO NOTHING;

-- Options for Q2: 15 + 8
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('e1000000-0000-0000-0000-000000000005', 'd1000000-0000-0000-0000-000000000002', '21', false, 1),
('e1000000-0000-0000-0000-000000000006', 'd1000000-0000-0000-0000-000000000002', '22', false, 2),
('e1000000-0000-0000-0000-000000000007', 'd1000000-0000-0000-0000-000000000002', '23', true,  3),
('e1000000-0000-0000-0000-000000000008', 'd1000000-0000-0000-0000-000000000002', '24', false, 4)
ON CONFLICT (id) DO NOTHING;

-- Options for Q3: 23 + 19
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('e1000000-0000-0000-0000-000000000009', 'd1000000-0000-0000-0000-000000000003', '40', false, 1),
('e1000000-0000-0000-0000-000000000010', 'd1000000-0000-0000-0000-000000000003', '41', false, 2),
('e1000000-0000-0000-0000-000000000011', 'd1000000-0000-0000-0000-000000000003', '42', true,  3),
('e1000000-0000-0000-0000-000000000012', 'd1000000-0000-0000-0000-000000000003', '43', false, 4)
ON CONFLICT (id) DO NOTHING;

-- Options for Q4: True/False (10 + 10 = 20)
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('e1000000-0000-0000-0000-000000000013', 'd1000000-0000-0000-0000-000000000004', 'True',  true,  1),
('e1000000-0000-0000-0000-000000000014', 'd1000000-0000-0000-0000-000000000004', 'False', false, 2)
ON CONFLICT (id) DO NOTHING;

-- Options for Q5: 45 + 37
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('e1000000-0000-0000-0000-000000000015', 'd1000000-0000-0000-0000-000000000005', '72', false, 1),
('e1000000-0000-0000-0000-000000000016', 'd1000000-0000-0000-0000-000000000005', '82', true,  2),
('e1000000-0000-0000-0000-000000000017', 'd1000000-0000-0000-0000-000000000005', '81', false, 3),
('e1000000-0000-0000-0000-000000000018', 'd1000000-0000-0000-0000-000000000005', '92', false, 4)
ON CONFLICT (id) DO NOTHING;

-- ─── Questions for Quiz 4: Plant Life (Science) ─────────────────────
INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status) VALUES
('d1000000-0000-0000-0000-000000000006', 'c1000000-0000-0000-0000-000000000004',
    'multiple_choice', 'What do plants need to make food?', 'Select the correct answer',
    'Plants use sunlight, water and CO2 to make food through photosynthesis', 'easy', 1, 'published'),
('d1000000-0000-0000-0000-000000000007', 'c1000000-0000-0000-0000-000000000004',
    'multiple_choice', 'Which part of the plant absorbs water?', 'Select the correct answer',
    'Roots absorb water and minerals from the soil', 'easy', 2, 'published'),
('d1000000-0000-0000-0000-000000000008', 'c1000000-0000-0000-0000-000000000004',
    'true_false', 'Leaves are the food factories of a plant', 'Is this statement true?',
    'Leaves contain chlorophyll and perform photosynthesis', 'easy', 3, 'published'),
('d1000000-0000-0000-0000-000000000009', 'c1000000-0000-0000-0000-000000000004',
    'multiple_choice', 'What gas do plants release during photosynthesis?', 'Select the correct answer',
    'Plants release oxygen during photosynthesis', 'easy', 4, 'published'),
('d1000000-0000-0000-0000-000000000010', 'c1000000-0000-0000-0000-000000000004',
    'multiple_choice', 'Which part of the plant makes seeds?', 'Select the correct answer',
    'Flowers are the reproductive parts that make seeds', 'easy', 5, 'published')
ON CONFLICT (id) DO NOTHING;

-- Options for Plant Q1: What do plants need?
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('e1000000-0000-0000-0000-000000000019', 'd1000000-0000-0000-0000-000000000006', 'Sunlight', true,  1),
('e1000000-0000-0000-0000-000000000020', 'd1000000-0000-0000-0000-000000000006', 'Darkness', false, 2),
('e1000000-0000-0000-0000-000000000021', 'd1000000-0000-0000-0000-000000000006', 'Salt', false, 3),
('e1000000-0000-0000-0000-000000000022', 'd1000000-0000-0000-0000-000000000006', 'Metal', false, 4)
ON CONFLICT (id) DO NOTHING;

-- Options for Plant Q2: Which part absorbs water?
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('e1000000-0000-0000-0000-000000000023', 'd1000000-0000-0000-0000-000000000007', 'Leaves', false, 1),
('e1000000-0000-0000-0000-000000000024', 'd1000000-0000-0000-0000-000000000007', 'Roots',  true,  2),
('e1000000-0000-0000-0000-000000000025', 'd1000000-0000-0000-0000-000000000007', 'Stem',   false, 3),
('e1000000-0000-0000-0000-000000000026', 'd1000000-0000-0000-0000-000000000007', 'Flower', false, 4)
ON CONFLICT (id) DO NOTHING;

-- Options for Plant Q3: True/False
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('e1000000-0000-0000-0000-000000000027', 'd1000000-0000-0000-0000-000000000008', 'True',  true,  1),
('e1000000-0000-0000-0000-000000000028', 'd1000000-0000-0000-0000-000000000008', 'False', false, 2)
ON CONFLICT (id) DO NOTHING;

-- Options for Plant Q4: What gas do plants release?
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('e1000000-0000-0000-0000-000000000029', 'd1000000-0000-0000-0000-000000000009', 'Carbon dioxide', false, 1),
('e1000000-0000-0000-0000-000000000030', 'd1000000-0000-0000-0000-000000000009', 'Nitrogen',       false, 2),
('e1000000-0000-0000-0000-000000000031', 'd1000000-0000-0000-0000-000000000009', 'Oxygen',         true,  3),
('e1000000-0000-0000-0000-000000000032', 'd1000000-0000-0000-0000-000000000009', 'Hydrogen',       false, 4)
ON CONFLICT (id) DO NOTHING;

-- Options for Plant Q5: Which part makes seeds?
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('e1000000-0000-0000-0000-000000000033', 'd1000000-0000-0000-0000-000000000010', 'Root',   false, 1),
('e1000000-0000-0000-0000-000000000034', 'd1000000-0000-0000-0000-000000000010', 'Stem',   false, 2),
('e1000000-0000-0000-0000-000000000035', 'd1000000-0000-0000-0000-000000000010', 'Leaf',   false, 3),
('e1000000-0000-0000-0000-000000000036', 'd1000000-0000-0000-0000-000000000010', 'Flower', true,  4)
ON CONFLICT (id) DO NOTHING;

-- ─── Questions for Quiz 6: Parts of Speech (English) ─────────────────
INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status) VALUES
('d1000000-0000-0000-0000-000000000011', 'c1000000-0000-0000-0000-000000000006',
    'multiple_choice', 'What is a noun?', 'Select the best definition',
    'A noun is a word that names a person, place, thing, or idea', 'easy', 1, 'published'),
('d1000000-0000-0000-0000-000000000012', 'c1000000-0000-0000-0000-000000000006',
    'multiple_choice', 'Which word is a verb?', 'Select the verb from these options',
    'A verb is an action word', 'easy', 2, 'published'),
('d1000000-0000-0000-0000-000000000013', 'c1000000-0000-0000-0000-000000000006',
    'true_false', '"Beautiful" is an adjective', 'Is this correct?',
    'Beautiful describes a noun, so it is an adjective', 'easy', 3, 'published'),
('d1000000-0000-0000-0000-000000000014', 'c1000000-0000-0000-0000-000000000006',
    'multiple_choice', 'Find the adjective: "The tall boy ran fast"', 'Which word describes the noun?',
    'Tall describes the boy, making it an adjective', 'easy', 4, 'published'),
('d1000000-0000-0000-0000-000000000015', 'c1000000-0000-0000-0000-000000000006',
    'multiple_choice', 'What type of word is "quickly"?', 'Identify the part of speech',
    'Quickly describes how an action is done, making it an adverb', 'medium', 5, 'published')
ON CONFLICT (id) DO NOTHING;

-- Options for English Q1: What is a noun?
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('e1000000-0000-0000-0000-000000000037', 'd1000000-0000-0000-0000-000000000011', 'An action word', false, 1),
('e1000000-0000-0000-0000-000000000038', 'd1000000-0000-0000-0000-000000000011', 'A naming word', true,  2),
('e1000000-0000-0000-0000-000000000039', 'd1000000-0000-0000-0000-000000000011', 'A describing word', false, 3),
('e1000000-0000-0000-0000-000000000040', 'd1000000-0000-0000-0000-000000000011', 'A joining word', false, 4)
ON CONFLICT (id) DO NOTHING;

-- Options for English Q2: Which word is a verb?
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('e1000000-0000-0000-0000-000000000041', 'd1000000-0000-0000-0000-000000000012', 'Table', false, 1),
('e1000000-0000-0000-0000-000000000042', 'd1000000-0000-0000-0000-000000000012', 'Run',   true,  2),
('e1000000-0000-0000-0000-000000000043', 'd1000000-0000-0000-0000-000000000012', 'Blue',  false, 3),
('e1000000-0000-0000-0000-000000000044', 'd1000000-0000-0000-0000-000000000012', 'Happy', false, 4)
ON CONFLICT (id) DO NOTHING;

-- Options for English Q3: True/False
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('e1000000-0000-0000-0000-000000000045', 'd1000000-0000-0000-0000-000000000013', 'True',  true,  1),
('e1000000-0000-0000-0000-000000000046', 'd1000000-0000-0000-0000-000000000013', 'False', false, 2)
ON CONFLICT (id) DO NOTHING;

-- Options for English Q4: Find the adjective
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('e1000000-0000-0000-0000-000000000047', 'd1000000-0000-0000-0000-000000000014', 'The',  false, 1),
('e1000000-0000-0000-0000-000000000048', 'd1000000-0000-0000-0000-000000000014', 'Tall', true,  2),
('e1000000-0000-0000-0000-000000000049', 'd1000000-0000-0000-0000-000000000014', 'Ran',  false, 3),
('e1000000-0000-0000-0000-000000000050', 'd1000000-0000-0000-0000-000000000014', 'Fast', false, 4)
ON CONFLICT (id) DO NOTHING;

-- Options for English Q5: What type is "quickly"?
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('e1000000-0000-0000-0000-000000000051', 'd1000000-0000-0000-0000-000000000015', 'Noun',      false, 1),
('e1000000-0000-0000-0000-000000000052', 'd1000000-0000-0000-0000-000000000015', 'Adjective', false, 2),
('e1000000-0000-0000-0000-000000000053', 'd1000000-0000-0000-0000-000000000015', 'Adverb',    true,  3),
('e1000000-0000-0000-0000-000000000054', 'd1000000-0000-0000-0000-000000000015', 'Verb',      false, 4)
ON CONFLICT (id) DO NOTHING;

-- ─── Daily Challenges (today) ────────────────────────────────────────
-- Assign Quiz 1 and Quiz 4 as daily challenges for all grades
DO $$
DECLARE v_grade UUID;
BEGIN
    FOR v_grade IN SELECT id FROM public.grades LOOP
        INSERT INTO public.daily_challenges (quiz_id, grade_id, challenge_date, is_active)
        VALUES ('c1000000-0000-0000-0000-000000000001', v_grade, CURRENT_DATE, true)
        ON CONFLICT (quiz_id, challenge_date) DO NOTHING;
        INSERT INTO public.daily_challenges (quiz_id, grade_id, challenge_date, is_active)
        VALUES ('c1000000-0000-0000-0000-000000000004', v_grade, CURRENT_DATE, true)
        ON CONFLICT (quiz_id, challenge_date) DO NOTHING;
    END LOOP;
END $$;

-- ─── Sample Tournament ───────────────────────────────────────────────
INSERT INTO public.tournaments (id, title, description, grade_id, question_count, time_limit_seconds, starts_at, ends_at, status)
SELECT 'f1000000-0000-0000-0000-000000000001', 'Weekly Math Challenge',
    'Test your math skills against other students!',
    g.id, 5, 300, now(), now() + interval '7 days', 'live'
FROM public.grades g WHERE g.code = 'G5'
ON CONFLICT (id) DO NOTHING;

-- Assign questions to tournament
INSERT INTO public.tournament_questions (tournament_id, question_id, sort_order) VALUES
('f1000000-0000-0000-0000-000000000001', 'd1000000-0000-0000-0000-000000000001', 1),
('f1000000-0000-0000-0000-000000000001', 'd1000000-0000-0000-0000-000000000002', 2),
('f1000000-0000-0000-0000-000000000001', 'd1000000-0000-0000-0000-000000000003', 3),
('f1000000-0000-0000-0000-000000000001', 'd1000000-0000-0000-0000-000000000004', 4),
('f1000000-0000-0000-0000-000000000001', 'd1000000-0000-0000-0000-000000000005', 5)
ON CONFLICT (tournament_id, question_id) DO NOTHING;


-- ═══════════════════════════════════════════════════════════════════════
-- PART 12: IQ QUIZ SEED — 60 QUESTIONS (ALL 12 QUESTION TYPES)
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
('1c000000-0000-0000-0001-000000000001', 'c1000000-0000-0000-0000-000000000099', 'multiple_choice', 'What comes next: 2, 6, 18, 54, ?', 'Find the pattern', 'Each number is multiplied by 3. 54 x 3 = 162', 'medium', 1, 'published'),
('1c000000-0000-0000-0001-000000000002', 'c1000000-0000-0000-0000-000000000099', 'multiple_choice', 'If all Bloops are Razzies, and all Razzies are Lazzies, then all Bloops are definitely Lazzies?', 'Logical deduction', 'This is a syllogism: if A⊂B and B⊂C then A⊂C', 'easy', 2, 'published'),
('1c000000-0000-0000-0001-000000000003', 'c1000000-0000-0000-0000-000000000099', 'multiple_choice', 'Which shape completes the pattern? ◯ △ ◯ △ ◯ ?', 'Identify the alternating pattern', 'The pattern alternates between circle and triangle', 'easy', 3, 'published'),
('1c000000-0000-0000-0001-000000000004', 'c1000000-0000-0000-0000-000000000099', 'multiple_choice', 'What is 15% of 200?', 'Calculate the percentage', '15/100 × 200 = 30', 'easy', 4, 'published'),
('1c000000-0000-0000-0001-000000000005', 'c1000000-0000-0000-0000-000000000099', 'multiple_choice', 'Which word does NOT belong: Apple, Banana, Carrot, Mango?', 'Find the odd one out', 'Carrot is a vegetable; the rest are fruits', 'easy', 5, 'published')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('1c100000-0001-0001-0000-000000000001', '1c000000-0000-0000-0001-000000000001', '108', false, 1),
('1c100000-0001-0001-0000-000000000002', '1c000000-0000-0000-0001-000000000001', '162', true, 2),
('1c100000-0001-0001-0000-000000000003', '1c000000-0000-0000-0001-000000000001', '216', false, 3),
('1c100000-0001-0001-0000-000000000004', '1c000000-0000-0000-0001-000000000001', '148', false, 4),
('1c100000-0001-0002-0000-000000000001', '1c000000-0000-0000-0001-000000000002', 'True', true, 1),
('1c100000-0001-0002-0000-000000000002', '1c000000-0000-0000-0001-000000000002', 'False', false, 2),
('1c100000-0001-0002-0000-000000000003', '1c000000-0000-0000-0001-000000000002', 'Cannot be determined', false, 3),
('1c100000-0001-0002-0000-000000000004', '1c000000-0000-0000-0001-000000000002', 'Only sometimes', false, 4),
('1c100000-0001-0003-0000-000000000001', '1c000000-0000-0000-0001-000000000003', '◯', false, 1),
('1c100000-0001-0003-0000-000000000002', '1c000000-0000-0000-0001-000000000003', '△', true, 2),
('1c100000-0001-0003-0000-000000000003', '1c000000-0000-0000-0001-000000000003', '□', false, 3),
('1c100000-0001-0003-0000-000000000004', '1c000000-0000-0000-0001-000000000003', '⬡', false, 4),
('1c100000-0001-0004-0000-000000000001', '1c000000-0000-0000-0001-000000000004', '25', false, 1),
('1c100000-0001-0004-0000-000000000002', '1c000000-0000-0000-0001-000000000004', '30', true, 2),
('1c100000-0001-0004-0000-000000000003', '1c000000-0000-0000-0001-000000000004', '35', false, 3),
('1c100000-0001-0004-0000-000000000004', '1c000000-0000-0000-0001-000000000004', '15', false, 4),
('1c100000-0001-0005-0000-000000000001', '1c000000-0000-0000-0001-000000000005', 'Apple', false, 1),
('1c100000-0001-0005-0000-000000000002', '1c000000-0000-0000-0001-000000000005', 'Banana', false, 2),
('1c100000-0001-0005-0000-000000000003', '1c000000-0000-0000-0001-000000000005', 'Carrot', true, 3),
('1c100000-0001-0005-0000-000000000004', '1c000000-0000-0000-0001-000000000005', 'Mango', false, 4)
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 2: TRUE_FALSE (Questions 6-10)
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status) VALUES
('1c000000-0000-0000-0002-000000000001', 'c1000000-0000-0000-0000-000000000099', 'true_false', 'The square root of 144 is 12', 'True or False?', '12 × 12 = 144', 'easy', 6, 'published'),
('1c000000-0000-0000-0002-000000000002', 'c1000000-0000-0000-0000-000000000099', 'true_false', 'All prime numbers are odd', 'True or False?', '2 is a prime number and it is even', 'medium', 7, 'published'),
('1c000000-0000-0000-0002-000000000003', 'c1000000-0000-0000-0000-000000000099', 'true_false', 'A triangle can have two right angles', 'True or False?', 'Sum of angles = 180. Two right angles = 180, leaving 0 for the third', 'easy', 8, 'published'),
('1c000000-0000-0000-0002-000000000004', 'c1000000-0000-0000-0000-000000000099', 'true_false', 'Water boils at 100 degrees Celsius at sea level', 'True or False?', 'Standard boiling point of water at 1 atm', 'easy', 9, 'published'),
('1c000000-0000-0000-0002-000000000005', 'c1000000-0000-0000-0000-000000000099', 'true_false', 'The sun revolves around the Earth', 'True or False?', 'The Earth revolves around the Sun', 'easy', 10, 'published')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('1c100000-0002-0001-0000-000000000001', '1c000000-0000-0000-0002-000000000001', 'True', true, 1),
('1c100000-0002-0001-0000-000000000002', '1c000000-0000-0000-0002-000000000001', 'False', false, 2),
('1c100000-0002-0002-0000-000000000001', '1c000000-0000-0000-0002-000000000002', 'True', false, 1),
('1c100000-0002-0002-0000-000000000002', '1c000000-0000-0000-0002-000000000002', 'False', true, 2),
('1c100000-0002-0003-0000-000000000001', '1c000000-0000-0000-0002-000000000003', 'True', false, 1),
('1c100000-0002-0003-0000-000000000002', '1c000000-0000-0000-0002-000000000003', 'False', true, 2),
('1c100000-0002-0004-0000-000000000001', '1c000000-0000-0000-0002-000000000004', 'True', true, 1),
('1c100000-0002-0004-0000-000000000002', '1c000000-0000-0000-0002-000000000004', 'False', false, 2),
('1c100000-0002-0005-0000-000000000001', '1c000000-0000-0000-0002-000000000005', 'True', false, 1),
('1c100000-0002-0005-0000-000000000002', '1c000000-0000-0000-0002-000000000005', 'False', true, 2)
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 3: ORDERING (Questions 11-15) — options have correct_position
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status) VALUES
('1c000000-0000-0000-0003-000000000001', 'c1000000-0000-0000-0000-000000000099', 'ordering', 'Arrange from smallest to largest: 0.5, 1/4, 0.75, 1/3', 'Drag to order', '1/4=0.25, 1/3=0.33, 0.5, 0.75', 'medium', 11, 'published'),
('1c000000-0000-0000-0003-000000000002', 'c1000000-0000-0000-0000-000000000099', 'ordering', 'Order these planets from closest to farthest from Sun', 'Drag to order', 'Mercury, Venus, Earth, Mars', 'easy', 12, 'published'),
('1c000000-0000-0000-0003-000000000003', 'c1000000-0000-0000-0000-000000000099', 'ordering', 'Arrange in chronological order of invention', 'Drag to order', 'Wheel → Printing Press → Telephone → Internet', 'medium', 13, 'published'),
('1c000000-0000-0000-0003-000000000004', 'c1000000-0000-0000-0000-000000000099', 'ordering', 'Order from lightest to heaviest: Feather, Book, Car, Elephant', 'Drag to order', 'Feather < Book < Car < Elephant', 'easy', 14, 'published'),
('1c000000-0000-0000-0003-000000000005', 'c1000000-0000-0000-0000-000000000099', 'ordering', 'Arrange these numbers in descending order: 17, 42, 8, 31', 'Drag to order', '42, 31, 17, 8', 'easy', 15, 'published')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order, correct_position) VALUES
('1c100000-0003-0001-0000-000000000001', '1c000000-0000-0000-0003-000000000001', '0.5', false, 1, 3),
('1c100000-0003-0001-0000-000000000002', '1c000000-0000-0000-0003-000000000001', '1/4', false, 2, 1),
('1c100000-0003-0001-0000-000000000003', '1c000000-0000-0000-0003-000000000001', '0.75', false, 3, 4),
('1c100000-0003-0001-0000-000000000004', '1c000000-0000-0000-0003-000000000001', '1/3', false, 4, 2),
('1c100000-0003-0002-0000-000000000001', '1c000000-0000-0000-0003-000000000002', 'Mercury', false, 1, 1),
('1c100000-0003-0002-0000-000000000002', '1c000000-0000-0000-0003-000000000002', 'Venus', false, 2, 2),
('1c100000-0003-0002-0000-000000000003', '1c000000-0000-0000-0003-000000000002', 'Earth', false, 3, 3),
('1c100000-0003-0002-0000-000000000004', '1c000000-0000-0000-0003-000000000002', 'Mars', false, 4, 4),
('1c100000-0003-0003-0000-000000000001', '1c000000-0000-0000-0003-000000000003', 'Wheel', false, 1, 1),
('1c100000-0003-0003-0000-000000000002', '1c000000-0000-0000-0003-000000000003', 'Printing Press', false, 2, 2),
('1c100000-0003-0003-0000-000000000003', '1c000000-0000-0000-0003-000000000003', 'Telephone', false, 3, 3),
('1c100000-0003-0003-0000-000000000004', '1c000000-0000-0000-0003-000000000003', 'Internet', false, 4, 4),
('1c100000-0003-0004-0000-000000000001', '1c000000-0000-0000-0003-000000000004', 'Feather', false, 1, 1),
('1c100000-0003-0004-0000-000000000002', '1c000000-0000-0000-0003-000000000004', 'Book', false, 2, 2),
('1c100000-0003-0004-0000-000000000003', '1c000000-0000-0000-0003-000000000004', 'Car', false, 3, 3),
('1c100000-0003-0004-0000-000000000004', '1c000000-0000-0000-0003-000000000004', 'Elephant', false, 4, 4),
('1c100000-0003-0005-0000-000000000001', '1c000000-0000-0000-0003-000000000005', '42', false, 1, 1),
('1c100000-0003-0005-0000-000000000002', '1c000000-0000-0000-0003-000000000005', '31', false, 2, 2),
('1c100000-0003-0005-0000-000000000003', '1c000000-0000-0000-0003-000000000005', '17', false, 3, 3),
('1c100000-0003-0005-0000-000000000004', '1c000000-0000-0000-0003-000000000005', '8', false, 4, 4)
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 4: MATCH (Questions 16-20) — uses match_pairs table
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status) VALUES
('1c000000-0000-0000-0004-000000000001', 'c1000000-0000-0000-0000-000000000099', 'match', 'Match the country with its capital', 'Draw lines to match', 'Standard geography knowledge', 'medium', 16, 'published'),
('1c000000-0000-0000-0004-000000000002', 'c1000000-0000-0000-0000-000000000099', 'match', 'Match the animal with its sound', 'Draw lines to match', 'Common animal sounds', 'easy', 17, 'published'),
('1c000000-0000-0000-0004-000000000003', 'c1000000-0000-0000-0000-000000000099', 'match', 'Match the shape with its number of sides', 'Draw lines to match', 'Basic geometry', 'easy', 18, 'published'),
('1c000000-0000-0000-0004-000000000004', 'c1000000-0000-0000-0000-000000000099', 'match', 'Match the operator with its meaning', 'Draw lines to match', 'Math operators', 'easy', 19, 'published'),
('1c000000-0000-0000-0004-000000000005', 'c1000000-0000-0000-0000-000000000099', 'match', 'Match the word with its antonym', 'Draw lines to match', 'Antonyms are opposite meanings', 'medium', 20, 'published')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.match_pairs (id, question_id, left_text, right_text, sort_order) VALUES
('a0000000-0004-0001-0000-000000000001', '1c000000-0000-0000-0004-000000000001', 'India', 'New Delhi', 1),
('a0000000-0004-0001-0000-000000000002', '1c000000-0000-0000-0004-000000000001', 'France', 'Paris', 2),
('a0000000-0004-0001-0000-000000000003', '1c000000-0000-0000-0004-000000000001', 'Japan', 'Tokyo', 3),
('a0000000-0004-0001-0000-000000000004', '1c000000-0000-0000-0004-000000000001', 'Australia', 'Canberra', 4),
('a0000000-0004-0002-0000-000000000001', '1c000000-0000-0000-0004-000000000002', 'Dog', 'Bark', 1),
('a0000000-0004-0002-0000-000000000002', '1c000000-0000-0000-0004-000000000002', 'Cat', 'Meow', 2),
('a0000000-0004-0002-0000-000000000003', '1c000000-0000-0000-0004-000000000002', 'Cow', 'Moo', 3),
('a0000000-0004-0002-0000-000000000004', '1c000000-0000-0000-0004-000000000002', 'Duck', 'Quack', 4),
('a0000000-0004-0003-0000-000000000001', '1c000000-0000-0000-0004-000000000003', 'Triangle', '3', 1),
('a0000000-0004-0003-0000-000000000002', '1c000000-0000-0000-0004-000000000003', 'Square', '4', 2),
('a0000000-0004-0003-0000-000000000003', '1c000000-0000-0000-0004-000000000003', 'Pentagon', '5', 3),
('a0000000-0004-0003-0000-000000000004', '1c000000-0000-0000-0004-000000000003', 'Hexagon', '6', 4),
('a0000000-0004-0004-0000-000000000001', '1c000000-0000-0000-0004-000000000004', '+', 'Addition', 1),
('a0000000-0004-0004-0000-000000000002', '1c000000-0000-0000-0004-000000000004', '-', 'Subtraction', 2),
('a0000000-0004-0004-0000-000000000003', '1c000000-0000-0000-0004-000000000004', '×', 'Multiplication', 3),
('a0000000-0004-0004-0000-000000000004', '1c000000-0000-0000-0004-000000000004', '÷', 'Division', 4),
('a0000000-0004-0005-0000-000000000001', '1c000000-0000-0000-0004-000000000005', 'Hot', 'Cold', 1),
('a0000000-0004-0005-0000-000000000002', '1c000000-0000-0000-0004-000000000005', 'Big', 'Small', 2),
('a0000000-0004-0005-0000-000000000003', '1c000000-0000-0000-0004-000000000005', 'Fast', 'Slow', 3),
('a0000000-0004-0005-0000-000000000004', '1c000000-0000-0000-0004-000000000005', 'Happy', 'Sad', 4)
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 5: FILL_BLANK (Questions 21-25)
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status) VALUES
('1c000000-0000-0000-0005-000000000001', 'c1000000-0000-0000-0000-000000000099', 'fill_blank', 'The capital of India is ___', 'Fill in the blank', 'New Delhi is the capital of India', 'easy', 21, 'published'),
('1c000000-0000-0000-0005-000000000002', 'c1000000-0000-0000-0000-000000000099', 'fill_blank', '8 × 7 = ___', 'Fill in the blank', '8 × 7 = 56', 'easy', 22, 'published'),
('1c000000-0000-0000-0005-000000000003', 'c1000000-0000-0000-0000-000000000099', 'fill_blank', 'The chemical symbol for water is ___', 'Fill in the blank', 'H2O is the chemical formula for water', 'easy', 23, 'published'),
('1c000000-0000-0000-0005-000000000004', 'c1000000-0000-0000-0000-000000000099', 'fill_blank', 'A rectangle has ___ sides', 'Fill in the blank', 'A rectangle has 4 sides', 'easy', 24, 'published'),
('1c000000-0000-0000-0005-000000000005', 'c1000000-0000-0000-0000-000000000099', 'fill_blank', 'The past tense of "go" is ___', 'Fill in the blank', 'The past tense of go is went', 'easy', 25, 'published')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('1c100000-0005-0001-0000-000000000001', '1c000000-0000-0000-0005-000000000001', 'New Delhi', true, 1),
('1c100000-0005-0001-0000-000000000002', '1c000000-0000-0000-0005-000000000001', 'Mumbai', false, 2),
('1c100000-0005-0001-0000-000000000003', '1c000000-0000-0000-0005-000000000001', 'Kolkata', false, 3),
('1c100000-0005-0001-0000-000000000004', '1c000000-0000-0000-0005-000000000001', 'Chennai', false, 4),
('1c100000-0005-0002-0000-000000000001', '1c000000-0000-0000-0005-000000000002', '56', true, 1),
('1c100000-0005-0002-0000-000000000002', '1c000000-0000-0000-0005-000000000002', '48', false, 2),
('1c100000-0005-0002-0000-000000000003', '1c000000-0000-0000-0005-000000000002', '54', false, 3),
('1c100000-0005-0002-0000-000000000004', '1c000000-0000-0000-0005-000000000002', '64', false, 4),
('1c100000-0005-0003-0000-000000000001', '1c000000-0000-0000-0005-000000000003', 'H2O', true, 1),
('1c100000-0005-0003-0000-000000000002', '1c000000-0000-0000-0005-000000000003', 'CO2', false, 2),
('1c100000-0005-0003-0000-000000000003', '1c000000-0000-0000-0005-000000000003', 'O2', false, 3),
('1c100000-0005-0003-0000-000000000004', '1c000000-0000-0000-0005-000000000003', 'NaCl', false, 4),
('1c100000-0005-0004-0000-000000000001', '1c000000-0000-0000-0005-000000000004', '4', true, 1),
('1c100000-0005-0004-0000-000000000002', '1c000000-0000-0000-0005-000000000004', '3', false, 2),
('1c100000-0005-0004-0000-000000000003', '1c000000-0000-0000-0005-000000000004', '5', false, 3),
('1c100000-0005-0004-0000-000000000004', '1c000000-0000-0000-0005-000000000004', '6', false, 4),
('1c100000-0005-0005-0000-000000000001', '1c000000-0000-0000-0005-000000000005', 'went', true, 1),
('1c100000-0005-0005-0000-000000000002', '1c000000-0000-0000-0005-000000000005', 'gone', false, 2),
('1c100000-0005-0005-0000-000000000003', '1c000000-0000-0000-0005-000000000005', 'goed', false, 3),
('1c100000-0005-0005-0000-000000000004', '1c000000-0000-0000-0005-000000000005', 'going', false, 4)
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 6: SELECT_WORD (Questions 26-30)
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status) VALUES
('1c000000-0000-0000-0006-000000000001', 'c1000000-0000-0000-0000-000000000099', 'select_word', 'Select the noun in: "The quick brown fox jumps"', 'Tap the noun', 'Fox is the noun (person/place/thing)', 'easy', 26, 'published'),
('1c000000-0000-0000-0006-000000000002', 'c1000000-0000-0000-0000-000000000099', 'select_word', 'Select the verb in: "She reads books daily"', 'Tap the verb', 'Reads is the action word (verb)', 'easy', 27, 'published'),
('1c000000-0000-0000-0006-000000000003', 'c1000000-0000-0000-0000-000000000099', 'select_word', 'Select the adjective in: "A tall building stood there"', 'Tap the adjective', 'Tall describes the building', 'easy', 28, 'published'),
('1c000000-0000-0000-0006-000000000004', 'c1000000-0000-0000-0000-000000000099', 'select_word', 'Select the adverb in: "He ran quickly to school"', 'Tap the adverb', 'Quickly describes how he ran', 'medium', 29, 'published'),
('1c000000-0000-0000-0006-000000000005', 'c1000000-0000-0000-0000-000000000099', 'select_word', 'Select the preposition in: "The cat is under the table"', 'Tap the preposition', 'Under shows position/relationship', 'medium', 30, 'published')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('1c100000-0006-0001-0000-000000000001', '1c000000-0000-0000-0006-000000000001', 'quick', false, 1),
('1c100000-0006-0001-0000-000000000002', '1c000000-0000-0000-0006-000000000001', 'brown', false, 2),
('1c100000-0006-0001-0000-000000000003', '1c000000-0000-0000-0006-000000000001', 'fox', true, 3),
('1c100000-0006-0001-0000-000000000004', '1c000000-0000-0000-0006-000000000001', 'jumps', false, 4),
('1c100000-0006-0002-0000-000000000001', '1c000000-0000-0000-0006-000000000002', 'She', false, 1),
('1c100000-0006-0002-0000-000000000002', '1c000000-0000-0000-0006-000000000002', 'reads', true, 2),
('1c100000-0006-0002-0000-000000000003', '1c000000-0000-0000-0006-000000000002', 'books', false, 3),
('1c100000-0006-0002-0000-000000000004', '1c000000-0000-0000-0006-000000000002', 'daily', false, 4),
('1c100000-0006-0003-0000-000000000001', '1c000000-0000-0000-0006-000000000003', 'A', false, 1),
('1c100000-0006-0003-0000-000000000002', '1c000000-0000-0000-0006-000000000003', 'tall', true, 2),
('1c100000-0006-0003-0000-000000000003', '1c000000-0000-0000-0006-000000000003', 'building', false, 3),
('1c100000-0006-0003-0000-000000000004', '1c000000-0000-0000-0006-000000000003', 'stood', false, 4),
('1c100000-0006-0004-0000-000000000001', '1c000000-0000-0000-0006-000000000004', 'He', false, 1),
('1c100000-0006-0004-0000-000000000002', '1c000000-0000-0000-0006-000000000004', 'ran', false, 2),
('1c100000-0006-0004-0000-000000000003', '1c000000-0000-0000-0006-000000000004', 'quickly', true, 3),
('1c100000-0006-0004-0000-000000000004', '1c000000-0000-0000-0006-000000000004', 'school', false, 4),
('1c100000-0006-0005-0000-000000000001', '1c000000-0000-0000-0006-000000000005', 'cat', false, 1),
('1c100000-0006-0005-0000-000000000002', '1c000000-0000-0000-0006-000000000005', 'is', false, 2),
('1c100000-0006-0005-0000-000000000003', '1c000000-0000-0000-0006-000000000005', 'under', true, 3),
('1c100000-0006-0005-0000-000000000004', '1c000000-0000-0000-0006-000000000005', 'table', false, 4)
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 7: MATRIX (Questions 31-35) — uses prompt_config for matrix data
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status, prompt_config) VALUES
('1c000000-0000-0000-0007-000000000001', 'c1000000-0000-0000-0000-000000000099', 'matrix', 'Complete the 2x2 number matrix', 'Find the missing number',
    'Each row adds up to 10. Missing = 10 - 7 = 3', 'medium', 31, 'published',
    '{"rows":[["5","5"],["7","?"]],"row_labels":[],"col_labels":[]}'::jsonb),
('1c000000-0000-0000-0007-000000000002', 'c1000000-0000-0000-0000-000000000099', 'matrix', 'Find the pattern in this 3x3 matrix', 'What replaces the ?',
    'Each row doubles: 1→2→4, 3→6→12, 5→10→?=20', 'hard', 32, 'published',
    '{"rows":[["1","2","4"],["3","6","12"],["5","10","?"]]}'::jsonb),
('1c000000-0000-0000-0007-000000000003', 'c1000000-0000-0000-0000-000000000099', 'matrix', 'Complete the letter matrix', 'Find the pattern',
    'Rows go A→B→C, D→E→F, G→H→I', 'easy', 33, 'published',
    '{"rows":[["A","B","C"],["D","E","F"],["G","H","?"]]}'::jsonb),
('1c000000-0000-0000-0007-000000000004', 'c1000000-0000-0000-0000-000000000099', 'matrix', 'Multiplication table matrix', 'Find the missing product',
    '4 × 6 = 24', 'easy', 34, 'published',
    '{"rows":[["×","3","6"],["2","6","12"],["4","12","?"]],"col_labels":["×","3","6"]}'::jsonb),
('1c000000-0000-0000-0007-000000000005', 'c1000000-0000-0000-0000-000000000099', 'matrix', 'Complete the subtraction matrix', 'Each row: first - second = third',
    '15 - 6 = 9', 'medium', 35, 'published',
    '{"rows":[["10","4","6"],["12","5","7"],["15","6","?"]]}'::jsonb)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('1c100000-0007-0001-0000-000000000001', '1c000000-0000-0000-0007-000000000001', '3', true, 1),
('1c100000-0007-0001-0000-000000000002', '1c000000-0000-0000-0007-000000000001', '2', false, 2),
('1c100000-0007-0001-0000-000000000003', '1c000000-0000-0000-0007-000000000001', '5', false, 3),
('1c100000-0007-0001-0000-000000000004', '1c000000-0000-0000-0007-000000000001', '4', false, 4),
('1c100000-0007-0002-0000-000000000001', '1c000000-0000-0000-0007-000000000002', '20', true, 1),
('1c100000-0007-0002-0000-000000000002', '1c000000-0000-0000-0007-000000000002', '15', false, 2),
('1c100000-0007-0002-0000-000000000003', '1c000000-0000-0000-0007-000000000002', '25', false, 3),
('1c100000-0007-0002-0000-000000000004', '1c000000-0000-0000-0007-000000000002', '18', false, 4),
('1c100000-0007-0003-0000-000000000001', '1c000000-0000-0000-0007-000000000003', 'I', true, 1),
('1c100000-0007-0003-0000-000000000002', '1c000000-0000-0000-0007-000000000003', 'J', false, 2),
('1c100000-0007-0003-0000-000000000003', '1c000000-0000-0000-0007-000000000003', 'K', false, 3),
('1c100000-0007-0003-0000-000000000004', '1c000000-0000-0000-0007-000000000003', 'L', false, 4),
('1c100000-0007-0004-0000-000000000001', '1c000000-0000-0000-0007-000000000004', '24', true, 1),
('1c100000-0007-0004-0000-000000000002', '1c000000-0000-0000-0007-000000000004', '18', false, 2),
('1c100000-0007-0004-0000-000000000003', '1c000000-0000-0000-0007-000000000004', '20', false, 3),
('1c100000-0007-0004-0000-000000000004', '1c000000-0000-0000-0007-000000000004', '16', false, 4),
('1c100000-0007-0005-0000-000000000001', '1c000000-0000-0000-0007-000000000005', '9', true, 1),
('1c100000-0007-0005-0000-000000000002', '1c000000-0000-0000-0007-000000000005', '8', false, 2),
('1c100000-0007-0005-0000-000000000003', '1c000000-0000-0000-0007-000000000005', '10', false, 3),
('1c100000-0007-0005-0000-000000000004', '1c000000-0000-0000-0007-000000000005', '7', false, 4)
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 8: GRID_PATTERN (Questions 36-40) — uses prompt_config for grid
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status, prompt_config) VALUES
('1c000000-0000-0000-0008-000000000001', 'c1000000-0000-0000-0000-000000000099', 'grid_pattern', 'Which tile completes the 3x3 grid?', 'Find the missing tile',
    'Pattern rotates 90 degrees each cell', 'hard', 36, 'published',
    '{"grid":[["⬆","➡","⬇"],["➡","⬇","⬅"],["⬇","⬅","?"]],"size":3}'::jsonb),
('1c000000-0000-0000-0008-000000000002', 'c1000000-0000-0000-0000-000000000099', 'grid_pattern', 'Complete the color pattern grid', 'Which color goes in the empty cell?',
    'Colors alternate in a checkerboard', 'medium', 37, 'published',
    '{"grid":[["R","B","R"],["B","R","B"],["R","B","?"]],"size":3}'::jsonb),
('1c000000-0000-0000-0008-000000000003', 'c1000000-0000-0000-0000-000000000099', 'grid_pattern', 'Number grid: find the missing value', 'Each row and column has a pattern',
    'Diagonals sum to 15 in a magic square', 'hard', 38, 'published',
    '{"grid":[["2","7","6"],["9","5","1"],["4","3","?"]],"size":3}'::jsonb),
('1c000000-0000-0000-0008-000000000004', 'c1000000-0000-0000-0000-000000000099', 'grid_pattern', 'Shape rotation grid', 'What shape goes in the blank?',
    'Each row cycles through 3 shapes', 'medium', 39, 'published',
    '{"grid":[["◯","△","□"],["△","□","◯"],["□","◯","?"]],"size":3}'::jsonb),
('1c000000-0000-0000-0008-000000000005', 'c1000000-0000-0000-0000-000000000099', 'grid_pattern', 'Dot count pattern', 'How many dots in the missing cell?',
    'Each row increases by 1: 1,2,3 / 2,3,4 / 3,4,?=5', 'medium', 40, 'published',
    '{"grid":[["1","2","3"],["2","3","4"],["3","4","?"]],"size":3}'::jsonb)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('1c100000-0008-0001-0000-000000000001', '1c000000-0000-0000-0008-000000000001', '⬆', true, 1),
('1c100000-0008-0001-0000-000000000002', '1c000000-0000-0000-0008-000000000001', '➡', false, 2),
('1c100000-0008-0001-0000-000000000003', '1c000000-0000-0000-0008-000000000001', '⬇', false, 3),
('1c100000-0008-0001-0000-000000000004', '1c000000-0000-0000-0008-000000000001', '⬅', false, 4),
('1c100000-0008-0002-0000-000000000001', '1c000000-0000-0000-0008-000000000002', 'R', true, 1),
('1c100000-0008-0002-0000-000000000002', '1c000000-0000-0000-0008-000000000002', 'B', false, 2),
('1c100000-0008-0002-0000-000000000003', '1c000000-0000-0000-0008-000000000002', 'G', false, 3),
('1c100000-0008-0002-0000-000000000004', '1c000000-0000-0000-0008-000000000002', 'Y', false, 4),
('1c100000-0008-0003-0000-000000000001', '1c000000-0000-0000-0008-000000000003', '8', true, 1),
('1c100000-0008-0003-0000-000000000002', '1c000000-0000-0000-0008-000000000003', '6', false, 2),
('1c100000-0008-0003-0000-000000000003', '1c000000-0000-0000-0008-000000000003', '7', false, 3),
('1c100000-0008-0003-0000-000000000004', '1c000000-0000-0000-0008-000000000003', '9', false, 4),
('1c100000-0008-0004-0000-000000000001', '1c000000-0000-0000-0008-000000000004', '△', true, 1),
('1c100000-0008-0004-0000-000000000002', '1c000000-0000-0000-0008-000000000004', '□', false, 2),
('1c100000-0008-0004-0000-000000000003', '1c000000-0000-0000-0008-000000000004', '◯', false, 3),
('1c100000-0008-0004-0000-000000000004', '1c000000-0000-0000-0008-000000000004', '⬡', false, 4),
('1c100000-0008-0005-0000-000000000001', '1c000000-0000-0000-0008-000000000005', '5', true, 1),
('1c100000-0008-0005-0000-000000000002', '1c000000-0000-0000-0008-000000000005', '4', false, 2),
('1c100000-0008-0005-0000-000000000003', '1c000000-0000-0000-0008-000000000005', '6', false, 3),
('1c100000-0008-0005-0000-000000000004', '1c000000-0000-0000-0008-000000000005', '3', false, 4)
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 9: STATEMENT_REASON (Questions 41-45)
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status, metadata) VALUES
('1c000000-0000-0000-0009-000000000001', 'c1000000-0000-0000-0000-000000000099', 'statement_reason',
    'Statement: All metals conduct electricity. Reason: Metals have free electrons.',
    'Are both correct? Does the reason explain the statement?',
    'Both are true and the reason correctly explains the statement', 'hard', 41, 'published',
    '{"statement":"All metals conduct electricity","reason":"Metals have free electrons"}'::jsonb),
('1c000000-0000-0000-0009-000000000002', 'c1000000-0000-0000-0000-000000000099', 'statement_reason',
    'Statement: Plants are green. Reason: Plants have chlorophyll.',
    'Evaluate the statement and reason',
    'Both true, chlorophyll gives plants their green color', 'medium', 42, 'published',
    '{"statement":"Plants are green","reason":"Plants contain chlorophyll"}'::jsonb),
('1c000000-0000-0000-0009-000000000003', 'c1000000-0000-0000-0000-000000000099', 'statement_reason',
    'Statement: Ice floats on water. Reason: Ice is denser than water.',
    'Evaluate the statement and reason',
    'Statement is true but reason is false. Ice is LESS dense than water', 'hard', 43, 'published',
    '{"statement":"Ice floats on water","reason":"Ice is denser than water"}'::jsonb),
('1c000000-0000-0000-0009-000000000004', 'c1000000-0000-0000-0000-000000000099', 'statement_reason',
    'Statement: The moon produces its own light. Reason: The moon is a star.',
    'Evaluate the statement and reason',
    'Both are false. The moon reflects sunlight and is a satellite', 'medium', 44, 'published',
    '{"statement":"The moon produces its own light","reason":"The moon is a star"}'::jsonb),
('1c000000-0000-0000-0009-000000000005', 'c1000000-0000-0000-0000-000000000099', 'statement_reason',
    'Statement: Squares have 4 equal sides. Reason: All quadrilaterals have 4 sides.',
    'Evaluate the statement and reason',
    'Both true, but reason does not explain the statement', 'hard', 45, 'published',
    '{"statement":"Squares have 4 equal sides","reason":"All quadrilaterals have 4 sides"}'::jsonb)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('1c100000-0009-0001-0000-000000000001', '1c000000-0000-0000-0009-000000000001', 'Both true; reason explains statement', true, 1),
('1c100000-0009-0001-0000-000000000002', '1c000000-0000-0000-0009-000000000001', 'Both true; reason does NOT explain', false, 2),
('1c100000-0009-0001-0000-000000000003', '1c000000-0000-0000-0009-000000000001', 'Statement true; reason false', false, 3),
('1c100000-0009-0001-0000-000000000004', '1c000000-0000-0000-0009-000000000001', 'Both false', false, 4),
('1c100000-0009-0002-0000-000000000001', '1c000000-0000-0000-0009-000000000002', 'Both true; reason explains statement', true, 1),
('1c100000-0009-0002-0000-000000000002', '1c000000-0000-0000-0009-000000000002', 'Both true; reason does NOT explain', false, 2),
('1c100000-0009-0002-0000-000000000003', '1c000000-0000-0000-0009-000000000002', 'Statement true; reason false', false, 3),
('1c100000-0009-0002-0000-000000000004', '1c000000-0000-0000-0009-000000000002', 'Both false', false, 4),
('1c100000-0009-0003-0000-000000000001', '1c000000-0000-0000-0009-000000000003', 'Both true; reason explains statement', false, 1),
('1c100000-0009-0003-0000-000000000002', '1c000000-0000-0000-0009-000000000003', 'Both true; reason does NOT explain', false, 2),
('1c100000-0009-0003-0000-000000000003', '1c000000-0000-0000-0009-000000000003', 'Statement true; reason false', true, 3),
('1c100000-0009-0003-0000-000000000004', '1c000000-0000-0000-0009-000000000003', 'Both false', false, 4),
('1c100000-0009-0004-0000-000000000001', '1c000000-0000-0000-0009-000000000004', 'Both true; reason explains statement', false, 1),
('1c100000-0009-0004-0000-000000000002', '1c000000-0000-0000-0009-000000000004', 'Both true; reason does NOT explain', false, 2),
('1c100000-0009-0004-0000-000000000003', '1c000000-0000-0000-0009-000000000004', 'Statement true; reason false', false, 3),
('1c100000-0009-0004-0000-000000000004', '1c000000-0000-0000-0009-000000000004', 'Both false', true, 4),
('1c100000-0009-0005-0000-000000000001', '1c000000-0000-0000-0009-000000000005', 'Both true; reason explains statement', false, 1),
('1c100000-0009-0005-0000-000000000002', '1c000000-0000-0000-0009-000000000005', 'Both true; reason does NOT explain', true, 2),
('1c100000-0009-0005-0000-000000000003', '1c000000-0000-0000-0009-000000000005', 'Statement true; reason false', false, 3),
('1c100000-0009-0005-0000-000000000004', '1c000000-0000-0000-0009-000000000005', 'Both false', false, 4)
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 10: TABLE_DATA (Questions 46-50) — uses prompt_config for table
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status, prompt_config) VALUES
('1c000000-0000-0000-0010-000000000001', 'c1000000-0000-0000-0000-000000000099', 'table_data',
    'Study the marks table and answer', 'Who scored the highest in Math?',
    'Priya scored 95 in Math', 'easy', 46, 'published',
    '{"headers":["Student","Math","Science","English"],"rows":[["Amit","85","90","78"],["Priya","95","88","92"],["Raj","72","95","85"]]}'::jsonb),
('1c000000-0000-0000-0010-000000000002', 'c1000000-0000-0000-0000-000000000099', 'table_data',
    'Read the fruit price table', 'Which fruit costs the most per kg?',
    'Mango at 120/kg is the most expensive', 'easy', 47, 'published',
    '{"headers":["Fruit","Price/kg","Season"],"rows":[["Apple","80","Winter"],["Mango","120","Summer"],["Banana","40","All year"]]}'::jsonb),
('1c000000-0000-0000-0010-000000000003', 'c1000000-0000-0000-0000-000000000099', 'table_data',
    'Temperature log: find the coldest day', 'Which day was coldest?',
    'Wednesday at 18C was the coldest', 'easy', 48, 'published',
    '{"headers":["Day","Temp (C)","Weather"],"rows":[["Monday","25","Sunny"],["Tuesday","22","Cloudy"],["Wednesday","18","Rainy"],["Thursday","28","Sunny"]]}'::jsonb),
('1c000000-0000-0000-0010-000000000004', 'c1000000-0000-0000-0000-000000000099', 'table_data',
    'Animal lifespan table', 'Which animal lives the longest?',
    'Elephant at 70 years lives the longest', 'easy', 49, 'published',
    '{"headers":["Animal","Lifespan (years)","Type"],"rows":[["Dog","13","Mammal"],["Cat","15","Mammal"],["Elephant","70","Mammal"],["Parrot","50","Bird"]]}'::jsonb),
('1c000000-0000-0000-0010-000000000005', 'c1000000-0000-0000-0000-000000000099', 'table_data',
    'Class test results', 'How many students scored above 80?',
    '3 students (Anita 85, Vikram 92, Zara 88) scored above 80', 'medium', 50, 'published',
    '{"headers":["Name","Score","Grade"],"rows":[["Anita","85","A"],["Bikash","72","B"],["Vikram","92","A+"],["Zara","88","A"]]}'::jsonb)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
('1c100000-0010-0001-0000-000000000001', '1c000000-0000-0000-0010-000000000001', 'Amit', false, 1),
('1c100000-0010-0001-0000-000000000002', '1c000000-0000-0000-0010-000000000001', 'Priya', true, 2),
('1c100000-0010-0001-0000-000000000003', '1c000000-0000-0000-0010-000000000001', 'Raj', false, 3),
('1c100000-0010-0001-0000-000000000004', '1c000000-0000-0000-0010-000000000001', 'All scored equal', false, 4),
('1c100000-0010-0002-0000-000000000001', '1c000000-0000-0000-0010-000000000002', 'Apple', false, 1),
('1c100000-0010-0002-0000-000000000002', '1c000000-0000-0000-0010-000000000002', 'Mango', true, 2),
('1c100000-0010-0002-0000-000000000003', '1c000000-0000-0000-0010-000000000002', 'Banana', false, 3),
('1c100000-0010-0002-0000-000000000004', '1c000000-0000-0000-0010-000000000002', 'All same price', false, 4),
('1c100000-0010-0003-0000-000000000001', '1c000000-0000-0000-0010-000000000003', 'Monday', false, 1),
('1c100000-0010-0003-0000-000000000002', '1c000000-0000-0000-0010-000000000003', 'Tuesday', false, 2),
('1c100000-0010-0003-0000-000000000003', '1c000000-0000-0000-0010-000000000003', 'Wednesday', true, 3),
('1c100000-0010-0003-0000-000000000004', '1c000000-0000-0000-0010-000000000003', 'Thursday', false, 4),
('1c100000-0010-0004-0000-000000000001', '1c000000-0000-0000-0010-000000000004', 'Dog', false, 1),
('1c100000-0010-0004-0000-000000000002', '1c000000-0000-0000-0010-000000000004', 'Cat', false, 2),
('1c100000-0010-0004-0000-000000000003', '1c000000-0000-0000-0010-000000000004', 'Elephant', true, 3),
('1c100000-0010-0004-0000-000000000004', '1c000000-0000-0000-0010-000000000004', 'Parrot', false, 4),
('1c100000-0010-0005-0000-000000000001', '1c000000-0000-0000-0010-000000000005', '1', false, 1),
('1c100000-0010-0005-0000-000000000002', '1c000000-0000-0000-0010-000000000005', '2', false, 2),
('1c100000-0010-0005-0000-000000000003', '1c000000-0000-0000-0010-000000000005', '3', true, 3),
('1c100000-0010-0005-0000-000000000004', '1c000000-0000-0000-0010-000000000005', '4', false, 4)
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 11: MEMORY (Questions 51-55) — options with visual_label for pairs
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status, prompt_config) VALUES
('1c000000-0000-0000-0011-000000000001', 'c1000000-0000-0000-0000-000000000099', 'memory',
    'Match the pairs: Fruits', 'Find matching fruit pairs', 'Match identical fruits from memory', 'easy', 51, 'published',
    '{"pairs":4,"category":"fruits"}'::jsonb),
('1c000000-0000-0000-0011-000000000002', 'c1000000-0000-0000-0000-000000000099', 'memory',
    'Match the pairs: Numbers and Words', 'Match number with its word form', 'Each number matches its spelled form', 'medium', 52, 'published',
    '{"pairs":4,"category":"numbers"}'::jsonb),
('1c000000-0000-0000-0011-000000000003', 'c1000000-0000-0000-0000-000000000099', 'memory',
    'Match the pairs: Colors', 'Find matching color pairs', 'Match the color emoji with its name', 'easy', 53, 'published',
    '{"pairs":4,"category":"colors"}'::jsonb),
('1c000000-0000-0000-0011-000000000004', 'c1000000-0000-0000-0000-000000000099', 'memory',
    'Match the pairs: Shapes', 'Find matching shape pairs', 'Match the shape with its name', 'easy', 54, 'published',
    '{"pairs":4,"category":"shapes"}'::jsonb),
('1c000000-0000-0000-0011-000000000005', 'c1000000-0000-0000-0000-000000000099', 'memory',
    'Match the pairs: Animals and Sounds', 'Match animal with its sound', 'Each animal matches its characteristic sound', 'medium', 55, 'published',
    '{"pairs":4,"category":"animals"}'::jsonb)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order, visual_label) VALUES
('1c100000-0011-0001-0000-000000000001', '1c000000-0000-0000-0011-000000000001', 'Apple', true, 1, '🍎'),
('1c100000-0011-0001-0000-000000000002', '1c000000-0000-0000-0011-000000000001', 'Apple', true, 2, '🍎'),
('1c100000-0011-0001-0000-000000000003', '1c000000-0000-0000-0011-000000000001', 'Banana', true, 3, '🍌'),
('1c100000-0011-0001-0000-000000000004', '1c000000-0000-0000-0011-000000000001', 'Banana', true, 4, '🍌'),
('1c100000-0011-0001-0000-000000000005', '1c000000-0000-0000-0011-000000000001', 'Grape', true, 5, '🍇'),
('1c100000-0011-0001-0000-000000000006', '1c000000-0000-0000-0011-000000000001', 'Grape', true, 6, '🍇'),
('1c100000-0011-0001-0000-000000000007', '1c000000-0000-0000-0011-000000000001', 'Orange', true, 7, '🍊'),
('1c100000-0011-0001-0000-000000000008', '1c000000-0000-0000-0011-000000000001', 'Orange', true, 8, '🍊'),
('1c100000-0011-0002-0000-000000000001', '1c000000-0000-0000-0011-000000000002', '1', true, 1, 'One'),
('1c100000-0011-0002-0000-000000000002', '1c000000-0000-0000-0011-000000000002', '1', true, 2, 'One'),
('1c100000-0011-0002-0000-000000000003', '1c000000-0000-0000-0011-000000000002', '2', true, 3, 'Two'),
('1c100000-0011-0002-0000-000000000004', '1c000000-0000-0000-0011-000000000002', '2', true, 4, 'Two'),
('1c100000-0011-0002-0000-000000000005', '1c000000-0000-0000-0011-000000000002', '3', true, 5, 'Three'),
('1c100000-0011-0002-0000-000000000006', '1c000000-0000-0000-0011-000000000002', '3', true, 6, 'Three'),
('1c100000-0011-0002-0000-000000000007', '1c000000-0000-0000-0011-000000000002', '4', true, 7, 'Four'),
('1c100000-0011-0002-0000-000000000008', '1c000000-0000-0000-0011-000000000002', '4', true, 8, 'Four'),
('1c100000-0011-0003-0000-000000000001', '1c000000-0000-0000-0011-000000000003', 'Red', true, 1, '🔴'),
('1c100000-0011-0003-0000-000000000002', '1c000000-0000-0000-0011-000000000003', 'Red', true, 2, '🔴'),
('1c100000-0011-0003-0000-000000000003', '1c000000-0000-0000-0011-000000000003', 'Blue', true, 3, '🔵'),
('1c100000-0011-0003-0000-000000000004', '1c000000-0000-0000-0011-000000000003', 'Blue', true, 4, '🔵'),
('1c100000-0011-0003-0000-000000000005', '1c000000-0000-0000-0011-000000000003', 'Green', true, 5, '🟢'),
('1c100000-0011-0003-0000-000000000006', '1c000000-0000-0000-0011-000000000003', 'Green', true, 6, '🟢'),
('1c100000-0011-0003-0000-000000000007', '1c000000-0000-0000-0011-000000000003', 'Yellow', true, 7, '🟡'),
('1c100000-0011-0003-0000-000000000008', '1c000000-0000-0000-0011-000000000003', 'Yellow', true, 8, '🟡'),
('1c100000-0011-0004-0000-000000000001', '1c000000-0000-0000-0011-000000000004', 'Circle', true, 1, '◯'),
('1c100000-0011-0004-0000-000000000002', '1c000000-0000-0000-0011-000000000004', 'Circle', true, 2, '◯'),
('1c100000-0011-0004-0000-000000000003', '1c000000-0000-0000-0011-000000000004', 'Triangle', true, 3, '△'),
('1c100000-0011-0004-0000-000000000004', '1c000000-0000-0000-0011-000000000004', 'Triangle', true, 4, '△'),
('1c100000-0011-0004-0000-000000000005', '1c000000-0000-0000-0011-000000000004', 'Square', true, 5, '□'),
('1c100000-0011-0004-0000-000000000006', '1c000000-0000-0000-0011-000000000004', 'Square', true, 6, '□'),
('1c100000-0011-0004-0000-000000000007', '1c000000-0000-0000-0011-000000000004', 'Star', true, 7, '⭐'),
('1c100000-0011-0004-0000-000000000008', '1c000000-0000-0000-0011-000000000004', 'Star', true, 8, '⭐'),
('1c100000-0011-0005-0000-000000000001', '1c000000-0000-0000-0011-000000000005', 'Dog', true, 1, 'Bark'),
('1c100000-0011-0005-0000-000000000002', '1c000000-0000-0000-0011-000000000005', 'Dog', true, 2, 'Bark'),
('1c100000-0011-0005-0000-000000000003', '1c000000-0000-0000-0011-000000000005', 'Cat', true, 3, 'Meow'),
('1c100000-0011-0005-0000-000000000004', '1c000000-0000-0000-0011-000000000005', 'Cat', true, 4, 'Meow'),
('1c100000-0011-0005-0000-000000000005', '1c000000-0000-0000-0011-000000000005', 'Lion', true, 5, 'Roar'),
('1c100000-0011-0005-0000-000000000006', '1c000000-0000-0000-0011-000000000005', 'Lion', true, 6, 'Roar'),
('1c100000-0011-0005-0000-000000000007', '1c000000-0000-0000-0011-000000000005', 'Bird', true, 7, 'Tweet'),
('1c100000-0011-0005-0000-000000000008', '1c000000-0000-0000-0011-000000000005', 'Bird', true, 8, 'Tweet')
ON CONFLICT (id) DO NOTHING;


-- ══════════════════════════════════════════════════════════════════════
-- TYPE 12: VISUAL_SINGLE_CHOICE (Questions 56-60)
-- ══════════════════════════════════════════════════════════════════════

INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, explanation, difficulty, sort_order, status) VALUES
('1c000000-0000-0000-0012-000000000001', 'c1000000-0000-0000-0000-000000000099', 'visual_single_choice', 'Which image shows a triangle?', 'Select the correct shape', 'A triangle has 3 sides and 3 angles', 'easy', 56, 'published'),
('1c000000-0000-0000-0012-000000000002', 'c1000000-0000-0000-0000-000000000099', 'visual_single_choice', 'Which shows an even number of objects?', 'Count and select', 'Even numbers can be divided by 2 with no remainder', 'easy', 57, 'published'),
('1c000000-0000-0000-0012-000000000003', 'c1000000-0000-0000-0000-000000000099', 'visual_single_choice', 'Which is the largest animal?', 'Select the biggest one', 'Elephant is the largest land animal', 'easy', 58, 'published'),
('1c000000-0000-0000-0012-000000000004', 'c1000000-0000-0000-0000-000000000099', 'visual_single_choice', 'Which shows a symmetrical pattern?', 'Find the mirror image', 'Symmetry means both halves are identical mirrors', 'medium', 59, 'published'),
('1c000000-0000-0000-0012-000000000005', 'c1000000-0000-0000-0000-000000000099', 'visual_single_choice', 'Which clock shows 3 o''clock?', 'Select the correct clock', 'At 3:00 the hour hand points at 3, minute hand at 12', 'easy', 60, 'published')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order, visual_label) VALUES
('1c100000-0012-0001-0000-000000000001', '1c000000-0000-0000-0012-000000000001', 'Shape A', false, 1, '◯ Circle'),
('1c100000-0012-0001-0000-000000000002', '1c000000-0000-0000-0012-000000000001', 'Shape B', true, 2, '△ Triangle'),
('1c100000-0012-0001-0000-000000000003', '1c000000-0000-0000-0012-000000000001', 'Shape C', false, 3, '□ Square'),
('1c100000-0012-0001-0000-000000000004', '1c000000-0000-0000-0012-000000000001', 'Shape D', false, 4, '⬡ Hexagon'),
('1c100000-0012-0002-0000-000000000001', '1c000000-0000-0000-0012-000000000002', 'Group A', false, 1, '⭐⭐⭐ (3 stars)'),
('1c100000-0012-0002-0000-000000000002', '1c000000-0000-0000-0012-000000000002', 'Group B', true, 2, '⭐⭐⭐⭐ (4 stars)'),
('1c100000-0012-0002-0000-000000000003', '1c000000-0000-0000-0012-000000000002', 'Group C', false, 3, '⭐⭐⭐⭐⭐ (5 stars)'),
('1c100000-0012-0002-0000-000000000004', '1c000000-0000-0000-0012-000000000002', 'Group D', false, 4, '⭐ (1 star)'),
('1c100000-0012-0003-0000-000000000001', '1c000000-0000-0000-0012-000000000003', 'Animal A', false, 1, '🐱 Cat'),
('1c100000-0012-0003-0000-000000000002', '1c000000-0000-0000-0012-000000000003', 'Animal B', false, 2, '🐕 Dog'),
('1c100000-0012-0003-0000-000000000003', '1c000000-0000-0000-0012-000000000003', 'Animal C', true, 3, '🐘 Elephant'),
('1c100000-0012-0003-0000-000000000004', '1c000000-0000-0000-0012-000000000003', 'Animal D', false, 4, '🐇 Rabbit'),
('1c100000-0012-0004-0000-000000000001', '1c000000-0000-0000-0012-000000000004', 'Pattern A', false, 1, '⬆⬇⬆⬅ (asymmetric)'),
('1c100000-0012-0004-0000-000000000002', '1c000000-0000-0000-0012-000000000004', 'Pattern B', true, 2, '⬆⬇⬇⬆ (symmetric)'),
('1c100000-0012-0004-0000-000000000003', '1c000000-0000-0000-0012-000000000004', 'Pattern C', false, 3, '➡⬅⬆⬇ (asymmetric)'),
('1c100000-0012-0004-0000-000000000004', '1c000000-0000-0000-0012-000000000004', 'Pattern D', false, 4, '⬆➡⬇⬅ (asymmetric)'),
('1c100000-0012-0005-0000-000000000001', '1c000000-0000-0000-0012-000000000005', 'Clock A', false, 1, '🕐 1:00'),
('1c100000-0012-0005-0000-000000000002', '1c000000-0000-0000-0012-000000000005', 'Clock B', false, 2, '🕑 2:00'),
('1c100000-0012-0005-0000-000000000003', '1c000000-0000-0000-0012-000000000005', 'Clock C', true, 3, '🕒 3:00'),
('1c100000-0012-0005-0000-000000000004', '1c000000-0000-0000-0012-000000000005', 'Clock D', false, 4, '🕓 4:00')
ON CONFLICT (id) DO NOTHING;


-- ═══════════════════════════════════════════════════════════════════════
-- Done! Full setup complete.
--
-- SUMMARY:
-- • 19 tables (15 app + 4 CMS: admin_users, audit_logs, media_uploads, content_status_history)
-- • 45 RPC functions (15 app + 30 CMS/admin)
-- • 60 RLS policies (app users + admin CRUD)
-- • 4 modules (Math, Science, English, IQ Challenge)
-- • 8 chapters, 8 quizzes
-- • 75 questions (15 sample + 60 IQ)
-- • 60 IQ questions covering all 12 types:
--     1. multiple_choice (5)    2. true_false (5)         3. ordering (5)
--     4. match (5)              5. fill_blank (5)         6. select_word (5)
--     7. matrix (5)             8. grid_pattern (5)       9. statement_reason (5)
--    10. table_data (5)        11. memory (5)            12. visual_single_choice (5)
-- • 1 sample tournament + daily challenges
-- • 8 grades, 7 countries, 30+ cities seeded
--
-- NEXT: Seed your first admin user (Part 9) after signing in via Google.
-- ═══════════════════════════════════════════════════════════════════════
