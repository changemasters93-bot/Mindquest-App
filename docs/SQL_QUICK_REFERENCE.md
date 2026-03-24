# SQL Update - Quick Reference

## TL;DR - Copy & Paste SQL Command

### Execute This in Supabase SQL Editor:

```sql
DROP FUNCTION IF EXISTS public.get_profile(uuid) CASCADE;

CREATE OR REPLACE FUNCTION public.get_profile(p_user_id uuid)
RETURNS jsonb AS $$
DECLARE
  v_user_row public.users%rowtype;
  v_user_json jsonb;
  v_stats_json jsonb;
  v_completed_chapters jsonb;
  v_tournament_results jsonb;
BEGIN
  SELECT * INTO v_user_row
  FROM public.users
  WHERE id = p_user_id;

  IF v_user_row IS NULL THEN
    RETURN jsonb_build_object(
      'error', 'User not found',
      'user_id', p_user_id
    );
  END IF;

  v_user_json := jsonb_build_object(
    'id', v_user_row.id,
    'display_name', v_user_row.display_name,
    'avatar_id', v_user_row.avatar_id,
    'grade_id', v_user_row.grade_id,
    'grade_label', COALESCE((SELECT label FROM public.grades WHERE id = v_user_row.grade_id), ''),
    'auth_provider', v_user_row.auth_provider,
    'email', v_user_row.email,
    'phone', v_user_row.phone,
    'country_id', v_user_row.country_id,
    'city_id', v_user_row.city_id,
    'country_name', COALESCE((SELECT name FROM public.countries WHERE id = v_user_row.country_id), NULL),
    'city_name', COALESCE((SELECT name FROM public.cities WHERE id = v_user_row.city_id), NULL),
    'school_name', v_user_row.school_name
  );

  SELECT jsonb_build_object(
    'total_xp', COALESCE(total_xp, 0),
    'level', COALESCE(level, 1),
    'streak_current', COALESCE(streak_current, 0),
    'streak_best', COALESCE(streak_best, 0),
    'quizzes_completed', COALESCE(quizzes_completed, 0),
    'accuracy_pct', COALESCE(accuracy_pct, 0),
    'tournaments_played', COALESCE(tournaments_played, 0),
    'best_tournament_rank', COALESCE(best_tournament_rank, NULL),
    'iq_best_score', COALESCE(iq_best_score, NULL),
    'last_iq_attempt_at', COALESCE(last_iq_attempt_at::text, NULL),
    'iq_cooldown_hours', COALESCE(iq_cooldown_hours, 168),
    'iq_quiz_id', COALESCE(iq_quiz_id, NULL)
  ) INTO v_stats_json
  FROM public.user_stats
  WHERE user_id = p_user_id;

  v_stats_json := COALESCE(v_stats_json, jsonb_build_object(
    'total_xp', 0,
    'level', 1,
    'streak_current', 0,
    'streak_best', 0,
    'quizzes_completed', 0,
    'accuracy_pct', 0,
    'tournaments_played', 0,
    'best_tournament_rank', NULL,
    'iq_best_score', NULL,
    'last_iq_attempt_at', NULL,
    'iq_cooldown_hours', 168,
    'iq_quiz_id', NULL
  ));

  SELECT jsonb_agg(
    jsonb_build_object(
      'chapter_id', chapter_id,
      'chapter_title', chapter_title,
      'module_title', module_title,
      'module_emoji', module_emoji,
      'completed_at', completed_at::text
    )
  ) INTO v_completed_chapters
  FROM public.completed_chapters
  WHERE user_id = p_user_id
  ORDER BY completed_at DESC;

  v_completed_chapters := COALESCE(v_completed_chapters, '[]'::jsonb);

  SELECT jsonb_agg(
    jsonb_build_object(
      'tournament_id', tr.tournament_id,
      'title', t.title,
      'score', tr.score,
      'total_questions', t.question_count,
      'rank', tr.rank,
      'participant_count', COALESCE(tr.participant_count, 0),
      'certificate_url', tr.certificate_url,
      'date', tr.date::text
    )
  ) INTO v_tournament_results
  FROM public.tournament_results tr
  LEFT JOIN public.tournaments t ON tr.tournament_id = t.id
  WHERE tr.user_id = p_user_id
  ORDER BY tr.date DESC;

  v_tournament_results := COALESCE(v_tournament_results, '[]'::jsonb);

  RETURN jsonb_build_object(
    'user', v_user_json,
    'stats', v_stats_json,
    'completed_chapters', v_completed_chapters,
    'tournament_results', v_tournament_results
  );

EXCEPTION WHEN OTHERS THEN
  RETURN jsonb_build_object(
    'error', SQLERRM,
    'user_id', p_user_id,
    'error_code', SQLSTATE
  );
END;
$$ LANGUAGE plpgsql STABLE SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.get_profile(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_profile(uuid) TO anon;
```

---

## What This Command Does

✅ **Updates the `get_profile()` function to return**:
- `email` - User's email address
- `phone` - User's phone number
- `country_id` - Country reference
- `city_id` - City reference
- All existing fields (display_name, avatar_id, grade_id, etc.)

✅ **Maintains backward compatibility**:
- No existing data is deleted
- All previous fields still returned
- No schema changes to tables

✅ **Includes error handling**:
- Returns proper error if user not found
- Exception handling with error details

---

## How to Execute

### Method 1: Supabase Dashboard
1. Go to [app.supabase.com](https://app.supabase.com)
2. Select your MindQuest project
3. Click **SQL Editor** (left sidebar)
4. Click **New query**
5. Copy the SQL command above
6. Click **Run**
7. Wait for: "Query executed successfully"

### Method 2: Command Line
```bash
# Using psql
psql "postgresql://postgres:[password]@[project].supabase.co:5432/postgres" \
  -c "$(cat docs/SUPABASE_PROFILE_FIX.sql)"
```

---

## Verify It Works

After executing the SQL, run this query to test:

```sql
-- Replace 'test-user-id' with a real user ID from your database
SELECT
  (public.get_profile('test-user-id'::uuid) -> 'user' ->> 'email') as user_email,
  (public.get_profile('test-user-id'::uuid) -> 'user' ->> 'phone') as user_phone,
  (public.get_profile('test-user-id'::uuid) -> 'user' ->> 'country_id') as country_id;
```

**Expected result**: Should show email, phone (if set), and country_id fields

---

## Before & After

### Before (Old Function)
```json
{
  "user": {
    "id": "xxx",
    "display_name": "John",
    "avatar_id": 1,
    "auth_provider": "google"
    // ❌ email, phone NOT included
  }
}
```

### After (New Function)
```json
{
  "user": {
    "id": "xxx",
    "display_name": "John",
    "avatar_id": 1,
    "auth_provider": "google",
    "email": "john@example.com",  // ✅ NEW
    "phone": null,                // ✅ NEW
    "country_id": "US",           // ✅ NEW
    "city_id": "NY",              // ✅ NEW
    "country_name": "United States",  // ✅ NEW
    "city_name": "New York"       // ✅ NEW
  }
}
```

---

## Troubleshooting

### Error: "Function already exists"
- This is expected if function exists
- SQL command includes `DROP FUNCTION IF EXISTS` to handle this
- Just click Run again

### Error: "Permission denied"
- Make sure you're logged in with correct Supabase account
- Check that you have admin/owner access to project

### Query returns NULL for email/phone
- User probably hasn't linked those credentials yet
- Anonymous users won't have email or phone
- This is normal and expected behavior

---

## Rollback (If Needed)

If you need to revert, Supabase keeps a backup. You can:
1. Contact Supabase support
2. Or restore from a point-in-time backup
3. Or manually restore the old function definition

---

**Status**: Ready to execute ✅
