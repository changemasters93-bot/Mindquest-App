# Mindquest V2 - Backend App Guide (Supabase)

## Table of Contents
1. [Overview](#overview)
2. [Database Schema](#database-schema)
3. [RPC Functions (App-Facing)](#rpc-functions-app-facing)
4. [Row Level Security (RLS)](#row-level-security)
5. [Business Logic](#business-logic)
6. [Authentication](#authentication)
7. [Data Flow Diagrams](#data-flow-diagrams)
8. [Extensions & Indexes](#extensions--indexes)

---

## Overview

The Mindquest V2 backend runs on **Supabase** (managed PostgreSQL). All app communication goes through **RPC functions** (PostgREST). The client never queries tables directly.

**Key Design Principles:**
- All business logic lives in SQL functions (XP, leveling, streaks, cooldowns)
- RLS policies enforce per-user data access
- Idempotency keys prevent duplicate quiz submissions
- JSONB columns (`prompt_config`, `metadata`, `answers`) provide schema flexibility

**Required Extensions:**
```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";   -- UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";    -- Cryptographic functions
```

---

## Database Schema

### Reference Tables

#### `grades`
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK, DEFAULT uuid_generate_v4() | |
| code | TEXT | NOT NULL, UNIQUE | e.g., "G1", "G2" |
| label | TEXT | NOT NULL | e.g., "Grade 1" |
| sort_order | INT | NOT NULL, DEFAULT 0 | Display order |
| created_by | UUID | | Admin who created |
| updated_at | TIMESTAMPTZ | DEFAULT now() | |

Seed: G1-G8 (8 grades)

#### `countries`
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | |
| name | TEXT | NOT NULL | e.g., "India" |
| code | TEXT | NOT NULL, UNIQUE | ISO 3166-1 alpha-2 |
| is_active | BOOLEAN | NOT NULL, DEFAULT true | |
| updated_at | TIMESTAMPTZ | DEFAULT now() | |

Seed: IN, US, GB, CA, AU, SG, AE

#### `cities`
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | |
| country_id | UUID | FK → countries(id), NOT NULL | |
| name | TEXT | NOT NULL | |
| created_by | UUID | | |
| updated_at | TIMESTAMPTZ | DEFAULT now() | |

UNIQUE(country_id, name)

---

### User Management

#### `users`
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK, FK → auth.users(id) ON DELETE CASCADE | Supabase auth ID |
| display_name | TEXT | NOT NULL | |
| avatar_id | INT | NOT NULL, DEFAULT 4 | Avatar index |
| grade_id | UUID | FK → grades(id), NOT NULL | |
| auth_provider | TEXT | NOT NULL, DEFAULT 'anonymous' | CHECK: google/phone/anonymous/google_and_phone |
| country_id | UUID | FK → countries(id) | Optional |
| city_id | UUID | FK → cities(id) | Optional |
| school_name | TEXT | | Optional |
| google_sub | TEXT | | Google subject ID |
| total_xp | BIGINT | NOT NULL, DEFAULT 0 | Cumulative XP |
| level | INT | NOT NULL, DEFAULT 1 | Current level (1-10) |
| streak_current | INT | NOT NULL, DEFAULT 0 | Current consecutive days |
| streak_best | INT | NOT NULL, DEFAULT 0 | All-time best streak |
| iq_best_score | INT | DEFAULT NULL | Best IQ test score |
| is_banned | BOOLEAN | NOT NULL, DEFAULT false | Admin ban flag |
| banned_reason | TEXT | | |
| banned_at | TIMESTAMPTZ | | |
| last_active_at | TIMESTAMPTZ | DEFAULT now() | Last quiz activity |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |

---

### Content Tables

#### `modules`
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | |
| title | TEXT | NOT NULL | Module name |
| subtitle | TEXT | | Short description |
| description | TEXT | | Long description |
| emoji | TEXT | NOT NULL, DEFAULT '📚' | Display emoji |
| accent_color | TEXT | NOT NULL, DEFAULT '#4F46E5' | Hex color |
| thumbnail_url | TEXT | | Image URL |
| display_order | INT | NOT NULL, DEFAULT 0 | Sort position |
| grade_id | UUID | FK → grades(id) | Optional grade filter |
| is_active | BOOLEAN | NOT NULL, DEFAULT true | |
| status | TEXT | NOT NULL, DEFAULT 'draft' | CHECK: draft/review/published/archived |
| created_by | UUID | | |
| updated_by | UUID | | |
| published_at | TIMESTAMPTZ | | |
| created_at/updated_at | TIMESTAMPTZ | | |

#### `chapters`
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | |
| module_id | UUID | FK → modules(id) ON DELETE CASCADE, NOT NULL | |
| title | TEXT | NOT NULL | |
| description | TEXT | | |
| chapter_number | INT | NOT NULL, DEFAULT 1 | |
| sort_order | INT | NOT NULL, DEFAULT 0 | |
| is_active/status | | Same as modules | |
| created_by/updated_by | UUID | | |
| published_at | TIMESTAMPTZ | | |

#### `quizzes`
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | |
| chapter_id | UUID | FK → chapters(id) ON DELETE CASCADE, NOT NULL | |
| title | TEXT | NOT NULL | |
| description | TEXT | | |
| quiz_type | TEXT | NOT NULL, DEFAULT 'practice' | CHECK: practice/iq/tournament/daily |
| question_count | INT | NOT NULL, DEFAULT 0 | |
| time_limit_secs | INT | NOT NULL, DEFAULT 300 | |
| max_xp | INT | NOT NULL, DEFAULT 100 | Max XP earnable |
| difficulty | TEXT | CHECK: easy/medium/hard | |
| passing_score_pct | INT | NOT NULL, DEFAULT 60 | |
| shuffle_options | BOOLEAN | NOT NULL, DEFAULT true | |
| show_explanation | BOOLEAN | NOT NULL, DEFAULT true | |
| cooldown_hours | INT | DEFAULT NULL | e.g., 168 for IQ quiz (7 days) |
| sort_order | INT | NOT NULL, DEFAULT 0 | |
| is_active/status | | Same as modules | |

#### `questions`
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | |
| quiz_id | UUID | FK → quizzes(id) ON DELETE CASCADE, NOT NULL | |
| question_type | TEXT | NOT NULL, DEFAULT 'multiple_choice' | CHECK: 15 types (see below) |
| title | TEXT | NOT NULL | Question text |
| prompt | TEXT | | Additional prompt text |
| explanation | TEXT | NOT NULL, DEFAULT '' | Answer explanation |
| difficulty | TEXT | CHECK: easy/medium/hard | |
| time_limit_secs | INT | | Per-question time limit |
| allow_multiple | BOOLEAN | NOT NULL, DEFAULT false | |
| prompt_config | JSONB | | Dynamic question config |
| metadata | JSONB | | Question-specific data |
| media_url | TEXT | | Image/video URL |
| sort_order | INT | NOT NULL, DEFAULT 0 | |
| status | TEXT | Same as modules | |

**15 Allowed question_type values:**
`multiple_choice`, `true_false`, `ordering`, `match`, `fill_blank`, `select_word`, `matrix`, `grid_pattern`, `statement_reason`, `table_data`, `memory`, `visual_single_choice`, `grid_cell_select`, `grid_pattern_boolean`, `sequence_tap`

#### `question_options`
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | |
| question_id | UUID | FK → questions(id) ON DELETE CASCADE, NOT NULL | |
| label | TEXT | NOT NULL | Option text |
| is_correct | BOOLEAN | NOT NULL, DEFAULT false | |
| sort_order | INT | NOT NULL, DEFAULT 0 | Display order |
| correct_position | INT | | For ordering questions |
| media_url | TEXT | | Option image |
| visual_label | TEXT | | Icon/emoji for visual questions |

#### `match_pairs`
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | |
| question_id | UUID | FK → questions(id) ON DELETE CASCADE, NOT NULL | |
| left_text | TEXT | NOT NULL | Left side of pair |
| right_text | TEXT | NOT NULL | Right side of pair |
| sort_order | INT | NOT NULL, DEFAULT 0 | |

---

### User Progress

#### `quiz_attempts`
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | |
| user_id | UUID | FK → users(id) ON DELETE CASCADE, NOT NULL | |
| quiz_id | UUID | FK → quizzes(id) ON DELETE CASCADE, NOT NULL | |
| score | INT | NOT NULL, DEFAULT 0 | Questions correct |
| total_questions | INT | NOT NULL, DEFAULT 0 | |
| xp_earned | INT | NOT NULL, DEFAULT 0 | XP for this attempt |
| time_taken_secs | INT | NOT NULL, DEFAULT 0 | |
| answers | JSONB | NOT NULL, DEFAULT '[]' | Array of answer records |
| idempotency_key | TEXT | | Duplicate prevention |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |

UNIQUE(user_id, idempotency_key)

#### `daily_challenges`
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | |
| quiz_id | UUID | FK → quizzes(id) ON DELETE CASCADE, NOT NULL | |
| grade_id | UUID | FK → grades(id), NOT NULL | |
| challenge_date | DATE | NOT NULL, DEFAULT CURRENT_DATE | |
| is_active | BOOLEAN | NOT NULL, DEFAULT true | |
| created_by | UUID | | |

UNIQUE(quiz_id, challenge_date)

---

### Tournament Tables

#### `tournaments`
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | |
| title | TEXT | NOT NULL | |
| description | TEXT | | |
| grade_id | UUID | FK → grades(id), NOT NULL | Target grade |
| question_count | INT | NOT NULL, DEFAULT 10 | |
| time_limit_seconds | INT | NOT NULL, DEFAULT 600 | |
| max_participants | INT | | Capacity limit (NULL = unlimited) |
| starts_at | TIMESTAMPTZ | NOT NULL | |
| ends_at | TIMESTAMPTZ | NOT NULL | |
| status | TEXT | NOT NULL, DEFAULT 'draft' | CHECK: draft/scheduled/live/closed/finalized |
| created_by/updated_by | UUID | | |

#### `tournament_questions`
| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | PK |
| tournament_id | UUID | FK → tournaments(id) ON DELETE CASCADE, NOT NULL |
| question_id | UUID | FK → questions(id) ON DELETE CASCADE, NOT NULL |
| sort_order | INT | NOT NULL, DEFAULT 0 |

UNIQUE(tournament_id, question_id)

#### `tournament_entries`
| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | |
| tournament_id | UUID | FK → tournaments(id) ON DELETE CASCADE, NOT NULL | |
| user_id | UUID | FK → users(id) ON DELETE CASCADE, NOT NULL | |
| status | TEXT | NOT NULL, DEFAULT 'not_started' | CHECK: not_started/in_progress/paused/completed/auto_submitted |
| score | INT | NOT NULL, DEFAULT 0 | |
| time_taken_seconds | INT | | |
| rank | INT | | Final rank |
| time_remaining_secs | INT | | For pause/resume |
| answers_so_far | JSONB | DEFAULT '[]' | Running answer log |
| started_at | TIMESTAMPTZ | | |
| completed_at | TIMESTAMPTZ | | |

UNIQUE(tournament_id, user_id)

---

## RPC Functions (App-Facing)

### Dashboard & Profile

#### `get_user_dashboard(p_user_id UUID) → JSON`
Returns the user's home screen data bundle:
- User profile (name, avatar, grade, auth provider)
- Stats (total_xp, level, streak, accuracy, last IQ attempt, IQ cooldown, IQ quiz ID)
- Modules with progress (completed chapters/total, completed quizzes/total)
- Active tournament (if any for user's grade)

#### `get_profile(p_user_id UUID) → JSON`
Full profile with grade label, country/city names, school, all stats.

#### `update_profile(p_user_id UUID, p_fields JSON) → JSON`
Updates allowed fields: display_name, avatar_id, grade_id, country_id, city_id, school_name.

### Content Navigation

#### `get_module_full(p_module_id UUID, p_user_id UUID) → JSON`
Module details with all chapters. Each chapter includes:
- State: LOCKED/UNLOCKED/COMPLETED
- Progress: completed quizzes / total quizzes
- Chapter unlocking: first chapter always unlocked; subsequent chapters unlock when previous chapter's quizzes are all passed

#### `get_chapter_quizzes(p_chapter_id UUID, p_user_id UUID) → JSON`
All quizzes in a chapter with full questions, options, and match pairs. Each quiz includes:
- Best score, attempt count, locked status
- All questions with options (for immediate play)

#### `get_quiz_with_questions(p_quiz_id UUID, p_user_id UUID) → JSONB`
Single quiz with all questions, used for lazy-loading (daily challenges, IQ tests).

#### `get_daily_challenges(p_user_id UUID) → JSON`
Today's daily challenges for the user's grade.

### Quiz Submission

#### `submit_quiz_attempt(p_payload JSON) → JSON`

**Input:**
```json
{
  "userId": "uuid",
  "quizId": "uuid",
  "timeTakenSecs": 120,
  "idempotencyKey": "unique-string",
  "answers": [
    {"question_id": "uuid", "is_correct": true, "time_ms": 5000, "selected": "option_id"}
  ]
}
```

**Logic:**
1. Check idempotency key → return cached result if duplicate
2. Check cooldown → return `status: "cooldown"` with `unlocks_at` if within window
3. Calculate `xp_earned = ROUND(max_xp * score / total_questions)`
4. First attempt → full XP; replay → 0 XP
5. Update user: `total_xp`, `level`, `streak_current`, `streak_best`, `last_active_at`
6. For IQ quizzes: update `iq_best_score`
7. Calculate `rank_global`
8. Determine `next_quiz_id` (next quiz in chapter)

**Output:**
```json
{
  "status": "success",
  "attempt_id": "uuid",
  "score": 8,
  "total_questions": 10,
  "xp_earned": 80,
  "total_xp": 1250,
  "level": 4,
  "level_changed": true,
  "rank_global": 45,
  "is_replay": false,
  "next_quiz_id": "uuid or null"
}
```

### Tournament Functions

#### `get_active_tournament(p_user_id UUID, p_grade_id UUID) → JSON`
Returns the next live or scheduled tournament for the user's grade, including participant count and user's entry status.

#### `start_tournament(p_user_id UUID, p_tournament_id UUID) → JSON`
Creates/returns tournament entry. Returns questions and time limit. Enforces `max_participants`.

#### `pause_tournament(p_entry_id UUID) → JSON`
Pauses entry, saves `time_remaining_secs`.

#### `resume_tournament(p_entry_id UUID) → JSON`
Resumes paused entry.

#### `submit_tournament(p_entry_id UUID, p_answers JSON, p_time_taken INT) → JSON`
Final submission. Calculates score, rank, awards XP (10 per correct answer).

#### `submit_tournament_answer(p_entry_id UUID, p_answer JSON) → VOID`
Fire-and-forget per-question save. Appends to `answers_so_far` JSONB array.

#### `get_tournament_leaderboard(p_tournament_id UUID, p_user_id UUID, p_limit INT, p_offset INT) → JSON`
Tournament standings. Ranked by score DESC, time_taken ASC (tiebreaker). Includes user's own rank.

### Leaderboard & Stats

#### `get_leaderboard(p_user_id UUID, p_filter TEXT, p_filter_id UUID, p_limit INT, p_offset INT) → JSON`
Global leaderboard with filter options:
- `"all"` - all users
- `"grade"` - filter by grade_id
- `"country"` - filter by country_id

Returns ranked entries with user avatars, XP, level. Includes current user's rank.

#### `get_user_stats(p_user_id UUID, p_period TEXT) → JSON`
Period options: `week`, `month`, `last_week`, `last_month`, `last_6_months`, `all`

Returns: XP earned, accuracy, quizzes completed, daily activity heatmap, subject performance breakdown.

---

## Row Level Security

All tables have RLS enabled. Access is controlled via `auth.uid()`.

### Read Access

| Table | Policy | Who |
|-------|--------|-----|
| grades, countries, cities | `_select` | All authenticated |
| modules, chapters, quizzes | `_select` | All authenticated |
| questions, question_options, match_pairs | `_select` | All authenticated |
| daily_challenges | `_select` | All authenticated |
| tournaments, tournament_questions | `_select` | All authenticated |
| tournament_entries | `entries_select` | All authenticated (leaderboard visibility) |
| users | `users_select_own` | Own row only (`auth.uid() = id`) |
| quiz_attempts | `attempts_select_own` | Own rows only |

### Write Access

| Table | Policy | Who |
|-------|--------|-----|
| users | `users_insert_own` | Own row (`auth.uid() = id`) |
| users | `users_update_own` | Own row |
| quiz_attempts | `attempts_insert_own` | Own rows |
| tournament_entries | `entries_insert_own` | Own rows |
| tournament_entries | `entries_update_own` | Own rows |

**Note:** Content tables (modules, chapters, quizzes, questions) have no user write policies. All content is managed via admin RPC functions that use `SECURITY DEFINER` to bypass RLS.

---

## Business Logic

### XP System

**Formula:** `xp_earned = ROUND(max_xp * (score / total_questions))`
- First attempt: full XP
- Replay: 0 XP (tracked via existing `quiz_attempts` for same user+quiz)
- Tournament: 10 XP per correct answer

### Leveling (10 tiers)

| Level | XP Threshold |
|-------|-------------|
| 1 | 0 |
| 2 | 300 |
| 3 | 600 |
| 4 | 1,000 |
| 5 | 1,400 |
| 6 | 1,900 |
| 7 | 2,500 |
| 8 | 3,200 |
| 9 | 4,000 |
| 10 | 5,000+ |

Level is recalculated after every quiz submission. `level_changed: true` in response if level increased.

### Streak System

Updated on every quiz submission:

```
IF last_active_date IS NULL         → streak = 1 (first activity)
IF last_active_date = TODAY         → streak unchanged (same day)
IF last_active_date = YESTERDAY     → streak + 1 (consecutive)
ELSE                                → streak = 1 (broken)

streak_best = MAX(streak_best, streak_current)
```

### IQ Score

Only for `quiz_type = 'iq'`:
```
iq_best_score = MAX(COALESCE(iq_best_score, 0), current_score)
```

### Cooldown

Stored as `cooldown_hours` in `quizzes` table (e.g., 168 = 7 days for IQ quiz):
```
IF last_attempt + cooldown_hours > now() THEN
    RETURN {status: "cooldown", unlocks_at: timestamp}
```

### Tournament Ranking

```sql
ROW_NUMBER() OVER (ORDER BY score DESC, time_taken_seconds ASC)
```
Tiebreaker: faster completion time wins.

### Idempotency

`UNIQUE(user_id, idempotency_key)` on `quiz_attempts`. If duplicate key, return cached previous result.

---

## Authentication

### Supabase Auth Providers

| Provider | Config Location |
|----------|----------------|
| Google OAuth | Supabase Dashboard → Auth → Providers → Google |
| Phone OTP | Supabase Dashboard → Auth → Providers → Phone (requires SMS provider: Twilio/Vonage/MessageBird) |
| Anonymous | Supabase Dashboard → Auth → Settings → Enable anonymous sign-ins |

### `auth.uid()` Usage

All RPC functions receive `p_user_id` as a parameter. The client passes the Supabase session user ID. RLS policies use `auth.uid()` to enforce row-level access.

### User Row Creation

After auth, the client calls `upsertUser()` to create/update the `public.users` row:
```json
{
  "id": "auth.uid()",
  "display_name": "User Name",
  "avatar_id": 4,
  "grade_id": "grade-uuid",
  "auth_provider": "google"
}
```

---

## Data Flow Diagrams

### Quiz Attempt Flow

```
Client                          Supabase RPC
  |                                  |
  |  submit_quiz_attempt(payload)    |
  |─────────────────────────────────>|
  |                                  |── Check idempotency key
  |                                  |── Check cooldown
  |                                  |── Insert quiz_attempt
  |                                  |── Calculate XP
  |                                  |── Update user (xp, level, streak)
  |                                  |── Calculate global rank
  |                                  |── Find next quiz in chapter
  |  {status, score, xp, level, ...} |
  |<─────────────────────────────────|
```

### Dashboard Load Flow

```
Client                          Supabase RPC
  |                                  |
  |  get_user_dashboard(user_id)     |
  |─────────────────────────────────>|
  |                                  |── Fetch user profile
  |                                  |── Calculate stats (accuracy, streaks)
  |                                  |── Fetch modules with progress
  |                                  |── Check active tournament
  |                                  |── Check IQ cooldown
  |  {user, stats, modules, tournament} |
  |<─────────────────────────────────|
```

### Tournament Flow

```
Client                          Supabase RPC
  |                                  |
  |  get_active_tournament()         |
  |─────────────────────────────────>|
  |  {tournament_info}               |
  |<─────────────────────────────────|
  |                                  |
  |  start_tournament()              |
  |─────────────────────────────────>|
  |                                  |── Create/get entry
  |                                  |── Check max_participants
  |                                  |── Load tournament questions
  |  {entry_id, questions, timer}    |
  |<─────────────────────────────────|
  |                                  |
  |  [per question]                  |
  |  submit_tournament_answer()      |
  |─────────────────────────────────>|  (fire-and-forget)
  |                                  |
  |  submit_tournament()             |
  |─────────────────────────────────>|
  |                                  |── Calculate score, rank, XP
  |  {score, rank, xp}              |
  |<─────────────────────────────────|
```

---

## Extensions & Indexes

### PostgreSQL Extensions

```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
```

### Performance Indexes

```sql
-- Admin indexes
CREATE INDEX idx_audit_logs_admin ON public.audit_logs(admin_id, created_at);
CREATE INDEX idx_audit_logs_table ON public.audit_logs(table_name, record_id);
CREATE INDEX idx_media_uploads_by ON public.media_uploads(uploaded_by, created_at);
CREATE INDEX idx_content_history ON public.content_status_history(content_id, created_at);

-- Phase 1: Critical app performance indexes
CREATE INDEX idx_questions_quiz_id ON public.questions(quiz_id);
CREATE INDEX idx_question_options_question_id ON public.question_options(question_id);
CREATE INDEX idx_match_pairs_question_id ON public.match_pairs(question_id);
CREATE INDEX idx_quiz_attempts_user_id ON public.quiz_attempts(user_id);
CREATE INDEX idx_quiz_attempts_user_quiz ON public.quiz_attempts(user_id, quiz_id);
CREATE INDEX idx_users_total_xp ON public.users(total_xp DESC) WHERE NOT is_banned;
CREATE INDEX idx_chapters_module_id ON public.chapters(module_id);
CREATE INDEX idx_quizzes_chapter_id ON public.quizzes(chapter_id);
CREATE INDEX idx_tournament_entries_tournament ON public.tournament_entries(tournament_id, score DESC, time_taken_seconds ASC);

-- Phase 3: Materialized view for leaderboard rankings
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_user_ranks AS ...;
```

### CHECK Constraints Summary

| Table | Column | Allowed Values |
|-------|--------|---------------|
| users | auth_provider | google, phone, anonymous, google_and_phone |
| quizzes | quiz_type | practice, iq, tournament, daily |
| quizzes | difficulty | easy, medium, hard |
| questions | question_type | 15 types (see Questions section) |
| questions | difficulty | easy, medium, hard |
| modules/chapters/quizzes/questions | status | draft, review, published, archived |
| tournaments | status | draft, scheduled, live, closed, finalized |
| tournament_entries | status | not_started, in_progress, paused, completed, auto_submitted |
