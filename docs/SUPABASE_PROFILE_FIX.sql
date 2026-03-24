-- ============================================================================
-- Supabase Profile Function Update
-- ============================================================================
-- Purpose: Update the get_profile() function to return email, phone, country_id,
--          and city_id in the user profile JSON response
--
-- This fix is required after the Kotlin code updates to ensure the backend
-- returns all necessary user contact and location information
-- ============================================================================

-- First, verify the current function exists
-- SELECT routine_name FROM information_schema.routines
-- WHERE routine_schema = 'public' AND routine_name = 'get_profile';

-- Drop the existing function (if it exists with wrong signature)
DROP FUNCTION IF EXISTS public.get_profile(uuid) CASCADE;

-- Create updated get_profile function with email, phone, country_id, city_id
CREATE OR REPLACE FUNCTION public.get_profile(p_user_id uuid)
RETURNS jsonb AS $$
DECLARE
  v_user_row public.users%rowtype;
  v_user_json jsonb;
  v_stats_json jsonb;
  v_completed_chapters jsonb;
  v_tournament_results jsonb;
BEGIN
  -- Fetch the user row
  SELECT * INTO v_user_row
  FROM public.users
  WHERE id = p_user_id;

  IF v_user_row IS NULL THEN
    RETURN jsonb_build_object(
      'error', 'User not found',
      'user_id', p_user_id
    );
  END IF;

  -- Build user profile JSON with email, phone, country_id, city_id
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

  -- Fetch user stats
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

  -- Default stats if none found
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

  -- Fetch completed chapters
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

  -- Fetch tournament results
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

  -- Return complete profile response
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

-- Grant execute permission to authenticated users
GRANT EXECUTE ON FUNCTION public.get_profile(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_profile(uuid) TO anon;

-- ============================================================================
-- Verification Queries
-- ============================================================================
-- After executing the function creation, you can verify with:
--
-- 1. Test the function with a real user ID:
--    SELECT public.get_profile('your-user-id-here'::uuid);
--
-- 2. Verify the function exists:
--    SELECT routine_name, routine_definition
--    FROM information_schema.routines
--    WHERE routine_schema = 'public' AND routine_name = 'get_profile';
--
-- 3. Check that email and phone fields are returned:
--    SELECT (public.get_profile('your-user-id-here'::uuid) -> 'user' ->> 'email') as user_email,
--           (public.get_profile('your-user-id-here'::uuid) -> 'user' ->> 'phone') as user_phone;
--
-- ============================================================================
