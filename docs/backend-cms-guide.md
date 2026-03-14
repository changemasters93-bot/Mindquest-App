# Mindquest V2 - Backend CMS/Admin Guide

## Table of Contents
1. [Overview](#overview)
2. [Admin Tables](#admin-tables)
3. [Admin Authentication](#admin-authentication)
4. [Content Management RPCs](#content-management-rpcs)
5. [User Management RPCs](#user-management-rpcs)
6. [Reference Data RPCs](#reference-data-rpcs)
7. [Media Management](#media-management)
8. [Tournament Management RPCs](#tournament-management-rpcs)
9. [Daily Challenge Management](#daily-challenge-management)
10. [Analytics & Audit RPCs](#analytics--audit-rpcs)
11. [Content Publishing Workflow](#content-publishing-workflow)
12. [Audit Trail](#audit-trail)

---

## Overview

The CMS backend provides admin RPC functions for managing all Mindquest content, users, tournaments, and analytics. All admin functions are protected by the `is_admin()` security check.

**Admin capabilities:**
- CRUD operations for modules, chapters, quizzes, questions
- Content publishing workflow (draft → review → published → archived)
- User management (view, ban, unban)
- Tournament lifecycle management
- Daily challenge scheduling
- Media upload registration
- Audit log viewing
- Platform analytics

---

## Admin Tables

### `admin_users`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK, FK → auth.users(id) ON DELETE CASCADE | Supabase auth ID |
| display_name | TEXT | NOT NULL | Admin's name |
| email | TEXT | | Admin's email |
| role | TEXT | NOT NULL, DEFAULT 'content_editor' | CHECK: super_admin/admin/moderator/content_editor |
| permissions | JSONB | DEFAULT '{}' | Fine-grained permission flags |
| is_active | BOOLEAN | NOT NULL, DEFAULT true | Can be deactivated |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |

**Roles:**

| Role | Description |
|------|-------------|
| `super_admin` | Full access, can manage other admins |
| `admin` | Full content + user management |
| `moderator` | User management, content review |
| `content_editor` | Content CRUD only |

**Permissions JSON Example:**
```json
{
  "can_create_modules": true,
  "can_edit_questions": true,
  "can_ban_users": true,
  "can_publish_content": true,
  "can_view_analytics": true
}
```

### `audit_logs`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | |
| admin_id | UUID | NOT NULL | Who performed the action |
| action | TEXT | NOT NULL | CHECK: create/update/delete/publish/archive/ban/unban |
| table_name | TEXT | NOT NULL | Affected table |
| record_id | UUID | | Affected record |
| old_values | JSONB | | Previous state |
| new_values | JSONB | | New state |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |

**Indexes:**
- `idx_audit_logs_admin(admin_id, created_at)` - Filter by admin
- `idx_audit_logs_table(table_name, record_id)` - Filter by content

### `media_uploads`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | |
| uploaded_by | UUID | NOT NULL | Admin who uploaded |
| file_name | TEXT | NOT NULL | Original filename |
| file_size | INT | NOT NULL, DEFAULT 0 | Size in bytes |
| mime_type | TEXT | NOT NULL, DEFAULT 'image/png' | |
| storage_path | TEXT | NOT NULL | Supabase Storage path |
| cdn_url | TEXT | | Public CDN URL |
| alt_text | TEXT | | Accessibility text |
| width | INT | | Image width |
| height | INT | | Image height |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |

**Index:** `idx_media_uploads_by(uploaded_by, created_at)`

### `content_status_history`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | |
| content_type | TEXT | NOT NULL | CHECK: module/chapter/quiz/question/tournament |
| content_id | UUID | NOT NULL | ID of the content item |
| status | TEXT | NOT NULL | CHECK: draft/review/published/archived |
| changed_by | UUID | NOT NULL | Admin who changed status |
| reason | TEXT | | Reason for status change |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |

**Index:** `idx_content_history(content_id, created_at)`

---

## Admin Authentication

### `is_admin()` Security Check

All admin RPC functions call `is_admin()` as their first operation. This function:
1. Gets `auth.uid()` from the current Supabase session
2. Checks if the user exists in `admin_users` table
3. Verifies `is_active = true`
4. Returns `true` if valid admin, `false` otherwise

**If not admin:** Functions raise an exception or return an error response.

### How to Add an Admin

```sql
INSERT INTO public.admin_users (id, display_name, email, role)
VALUES (
    'auth-user-uuid',          -- Must be a valid auth.users(id)
    'Admin Name',
    'admin@example.com',
    'super_admin'              -- or 'admin', 'moderator', 'content_editor'
);
```

The user must first have a Supabase Auth account (sign up via any provider), then be manually added to `admin_users`.

---

## Content Management RPCs

### List Functions

#### `admin_list_modules(p_status TEXT, p_grade_id UUID, p_search TEXT, p_limit INT, p_offset INT) → JSON`
Lists all modules with optional filters:
- `p_status`: Filter by draft/review/published/archived (NULL = all)
- `p_grade_id`: Filter by grade (NULL = all)
- `p_search`: Search in title/subtitle
- Returns: id, title, subtitle, emoji, accent_color, status, grade, question count, chapter count

#### `admin_list_chapters(p_module_id UUID, p_status TEXT, p_limit INT, p_offset INT) → JSON`
Lists chapters for a module.

#### `admin_list_quizzes(p_chapter_id UUID, p_status TEXT, p_search TEXT, p_limit INT, p_offset INT) → JSON`
Lists quizzes for a chapter.

#### `admin_list_questions(p_quiz_id UUID, p_type TEXT, p_status TEXT, p_limit INT, p_offset INT) → JSON`
Lists questions for a quiz. Can filter by `question_type`.

#### `admin_get_question_detail(p_question_id UUID) → JSON`
Full question detail including all options and match pairs. Used for the question editor.

### CRUD Functions

#### `admin_upsert_module(p_data JSON) → JSON`
Create or update a module. If `p_data.id` exists, updates; otherwise creates.

**Input:**
```json
{
  "id": "uuid (optional for create)",
  "title": "Mathematics",
  "subtitle": "Number Theory",
  "description": "Learn about prime numbers...",
  "emoji": "🔢",
  "accent_color": "#6366F1",
  "grade_id": "grade-uuid",
  "display_order": 1
}
```
Creates audit log entry. Returns the module record.

#### `admin_upsert_chapter(p_data JSON) → JSON`
Create or update a chapter.

**Input:**
```json
{
  "id": "uuid (optional)",
  "module_id": "module-uuid",
  "title": "Chapter 1: Basics",
  "description": "Introduction to...",
  "chapter_number": 1,
  "sort_order": 1
}
```

#### `admin_upsert_quiz(p_data JSON) → JSON`
Create or update a quiz.

**Input:**
```json
{
  "id": "uuid (optional)",
  "chapter_id": "chapter-uuid",
  "title": "Basic Addition",
  "quiz_type": "practice",
  "question_count": 10,
  "time_limit_secs": 300,
  "max_xp": 100,
  "difficulty": "easy",
  "passing_score_pct": 60,
  "cooldown_hours": null
}
```

#### `admin_upsert_question(p_data JSON) → JSON`
Create or update a question with its options and match pairs.

**Input:**
```json
{
  "id": "uuid (optional)",
  "quiz_id": "quiz-uuid",
  "question_type": "multiple_choice",
  "title": "What is 2 + 2?",
  "explanation": "2 + 2 = 4",
  "difficulty": "easy",
  "prompt_config": {"key": "value"},
  "metadata": {"key": "value"},
  "options": [
    {"label": "3", "is_correct": false, "sort_order": 1},
    {"label": "4", "is_correct": true, "sort_order": 2},
    {"label": "5", "is_correct": false, "sort_order": 3}
  ],
  "match_pairs": [
    {"left_text": "France", "right_text": "Paris", "sort_order": 1}
  ]
}
```

Handles: deleting old options/pairs, inserting new ones, updating the question row.

### Delete & Reorder

#### `admin_delete_content(p_table TEXT, p_id UUID) → JSON`
Soft-delete or hard-delete content. Creates audit log with `old_values`.
- `p_table`: 'modules', 'chapters', 'quizzes', 'questions'

#### `admin_reorder(p_table TEXT, p_order JSON) → JSON`
Batch update `sort_order` for items.

**Input:**
```json
[
  {"id": "uuid-1", "sort_order": 1},
  {"id": "uuid-2", "sort_order": 2},
  {"id": "uuid-3", "sort_order": 3}
]
```

### Publishing

#### `admin_publish_content(p_type TEXT, p_id UUID) → JSON`
Changes status from any state to `published`. Sets `published_at = now()`.
- `p_type`: 'module', 'chapter', 'quiz', 'question'
- Creates `content_status_history` entry
- Creates audit log

#### `admin_archive_content(p_type TEXT, p_id UUID) → JSON`
Changes status to `archived`.
- Creates `content_status_history` entry
- Creates audit log

---

## User Management RPCs

#### `admin_get_users(p_filter TEXT, p_search TEXT, p_limit INT, p_offset INT) → JSON`
List users with filtering:
- `p_filter`: 'all', 'active', 'banned'
- `p_search`: Search in display_name
- Returns: user profile, stats, grade, country, ban status

#### `admin_ban_user(p_user_id UUID, p_reason TEXT) → JSON`
Bans a user:
- Sets `is_banned = true`, `banned_reason`, `banned_at`
- Creates audit log with action 'ban'

#### `admin_unban_user(p_user_id UUID) → JSON`
Unbans a user:
- Sets `is_banned = false`, clears `banned_reason`
- Creates audit log with action 'unban'

---

## Reference Data RPCs

#### `admin_upsert_grade(p_code TEXT, p_label TEXT, p_sort_order INT) → JSON`
Create or update a grade. Uses `ON CONFLICT (code) DO UPDATE`.

#### `admin_upsert_country(p_name TEXT, p_code TEXT, p_is_active BOOLEAN) → JSON`
Create or update a country. Uses `ON CONFLICT (code) DO UPDATE`.

#### `admin_upsert_city(p_country_id UUID, p_name TEXT) → JSON`
Create or update a city. Uses `ON CONFLICT (country_id, name) DO NOTHING`.

---

## Media Management

### Upload Workflow

1. **Upload file** to Supabase Storage (client-side or via edge function)
2. **Register metadata** via `admin_register_media()` RPC
3. **Use CDN URL** in content (module thumbnails, question media, option images)

#### `admin_register_media(p_file_name TEXT, p_file_size INT, p_mime_type TEXT, p_storage_path TEXT, p_cdn_url TEXT, p_alt_text TEXT, p_width INT, p_height INT) → JSON`

Registers an uploaded file in the `media_uploads` table. Returns the media record with generated UUID.

#### `admin_list_media(p_mime_filter TEXT, p_limit INT, p_offset INT) → JSON`

Lists uploaded media:
- `p_mime_filter`: Filter by MIME type prefix (e.g., 'image/' for all images)
- Returns: id, file_name, cdn_url, mime_type, file_size, uploaded_by, created_at

### Storage Paths Convention

```
media/modules/{module_id}/thumbnail.png
media/questions/{question_id}/image.png
media/options/{option_id}/visual.png
media/avatars/{avatar_id}.png
```

---

## Tournament Management RPCs

#### `admin_list_tournaments(p_status TEXT, p_limit INT, p_offset INT) → JSON`
Lists tournaments with optional status filter. Returns participant counts.

#### `admin_upsert_tournament(p_data JSON) → JSON`
Create or update a tournament.

**Input:**
```json
{
  "id": "uuid (optional)",
  "title": "Math Olympics Week 1",
  "description": "Weekly math tournament",
  "grade_id": "grade-uuid",
  "question_count": 10,
  "time_limit_seconds": 600,
  "max_participants": 100,
  "starts_at": "2026-03-15T10:00:00Z",
  "ends_at": "2026-03-15T22:00:00Z"
}
```

#### `admin_set_tournament_questions(p_tournament_id UUID, p_question_ids JSON) → JSON`
Assigns questions to a tournament. Replaces all existing assignments.

**Input:**
```json
["question-uuid-1", "question-uuid-2", "question-uuid-3"]
```

#### `admin_tournament_lifecycle(p_tournament_id UUID, p_action TEXT) → JSON`
Changes tournament status. Valid transitions:

```
draft → scheduled → live → closed → finalized
```

| Action | From | To |
|--------|------|----|
| `schedule` | draft | scheduled |
| `go_live` | scheduled | live |
| `close` | live | closed |
| `finalize` | closed | finalized |

---

## Daily Challenge Management

#### `admin_set_daily_challenge(p_grade_id UUID, p_quiz_id UUID, p_date DATE) → JSON`
Assigns a quiz as a daily challenge for a specific grade and date.

#### `admin_list_daily_challenges(p_date DATE, p_limit INT, p_offset INT) → JSON`
Lists daily challenges. If `p_date` is NULL, lists all.

---

## Analytics & Audit RPCs

#### `admin_get_analytics(p_days INT) → JSON`
Platform analytics for the last N days:
- Total users (active, new, banned)
- Total XP distributed
- Average accuracy
- Quizzes completed
- Active tournaments
- Media upload count
- Daily signups trend

#### `admin_get_audit_logs(p_table_name TEXT, p_action TEXT, p_admin_id UUID, p_limit INT, p_offset INT) → JSON`
View admin actions with filters:
- `p_table_name`: Filter by affected table
- `p_action`: Filter by action type (create/update/delete/publish/archive/ban/unban)
- `p_admin_id`: Filter by specific admin

#### `admin_dashboard_stats() → JSON`
Quick summary stats for the admin dashboard home page.

---

## Content Publishing Workflow

### Status Flow

```
draft ──→ review ──→ published ──→ archived
  ↑          |           |
  └──────────┘           |
  (reject back)          |
                    (can republish from archive)
```

### Rules

1. **New content** always starts as `draft`
2. **Review** is optional - admins can publish directly from draft
3. **Published** content is visible to app users (via RLS + `is_active` flag)
4. **Archived** content is hidden from app users
5. Every status change creates a `content_status_history` entry with `changed_by` and optional `reason`
6. Every status change creates an `audit_logs` entry

### Content Visibility

For content to appear in the app:
- `status = 'published'`
- `is_active = true`
- Parent must also be published (e.g., quiz needs published chapter, which needs published module)

---

## Audit Trail

Every admin action is automatically logged to `audit_logs`:

| Action | When |
|--------|------|
| `create` | New module/chapter/quiz/question/tournament created |
| `update` | Any content field updated |
| `delete` | Content deleted |
| `publish` | Status changed to published |
| `archive` | Status changed to archived |
| `ban` | User banned |
| `unban` | User unbanned |

Each log entry stores:
- `admin_id` - who did it
- `table_name` - what table was affected
- `record_id` - which record
- `old_values` - previous state (JSONB)
- `new_values` - new state (JSONB)
- `created_at` - when

This provides a complete history trail for compliance and debugging.
