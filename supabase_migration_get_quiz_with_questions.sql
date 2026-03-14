-- Migration: Add get_quiz_with_questions RPC
-- Run this in Supabase SQL Editor after supabase_final.sql

CREATE OR REPLACE FUNCTION public.get_quiz_with_questions(p_quiz_id UUID, p_user_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  v_result JSONB;
BEGIN
  SELECT jsonb_build_object(
    'id', q.id,
    'title', q.title,
    'quiz_type', q.quiz_type,
    'question_count', q.question_count,
    'time_limit_secs', q.time_limit_secs,
    'max_xp', q.max_xp,
    'difficulty', q.difficulty,
    'sort_order', q.sort_order,
    'cooldown_hours', q.cooldown_hours,
    'best_score', (
      SELECT MAX(qa.score)
      FROM quiz_attempts qa
      WHERE qa.quiz_id = q.id AND qa.user_id = p_user_id
    ),
    'attempt_count', (
      SELECT COUNT(*)::INT
      FROM quiz_attempts qa
      WHERE qa.quiz_id = q.id AND qa.user_id = p_user_id
    ),
    'last_attempt_at', (
      SELECT MAX(qa.completed_at)
      FROM quiz_attempts qa
      WHERE qa.quiz_id = q.id AND qa.user_id = p_user_id
    ),
    'is_locked', CASE
      WHEN q.cooldown_hours IS NOT NULL AND EXISTS(
        SELECT 1 FROM quiz_attempts qa
        WHERE qa.quiz_id = q.id
          AND qa.user_id = p_user_id
          AND qa.completed_at > NOW() - (q.cooldown_hours || ' hours')::INTERVAL
      ) THEN TRUE
      ELSE FALSE
    END,
    'unlocks_at', (
      SELECT MAX(qa.completed_at) + (q.cooldown_hours || ' hours')::INTERVAL
      FROM quiz_attempts qa
      WHERE qa.quiz_id = q.id
        AND qa.user_id = p_user_id
        AND qa.completed_at > NOW() - (q.cooldown_hours || ' hours')::INTERVAL
    ),
    'questions', COALESCE((
      SELECT jsonb_agg(
        jsonb_build_object(
          'id', qq.id,
          'question_type', qq.question_type,
          'title', qq.title,
          'prompt', qq.prompt,
          'explanation', qq.explanation,
          'difficulty', qq.difficulty,
          'time_limit_secs', qq.time_limit_secs,
          'allow_multiple', qq.allow_multiple,
          'prompt_config', qq.prompt_config,
          'metadata', qq.metadata,
          'media_url', qq.media_url,
          'options', COALESCE((
            SELECT jsonb_agg(
              jsonb_build_object(
                'id', qo.id,
                'label', qo.label,
                'is_correct', qo.is_correct,
                'sort_order', qo.sort_order
              ) ORDER BY qo.sort_order
            )
            FROM question_options qo
            WHERE qo.question_id = qq.id
          ), '[]'::JSONB),
          'match_pairs', COALESCE((
            SELECT jsonb_agg(
              jsonb_build_object(
                'id', mp.id,
                'left_side', mp.left_side,
                'right_side', mp.right_side,
                'sort_order', mp.sort_order
              ) ORDER BY mp.sort_order
            )
            FROM match_pairs mp
            WHERE mp.question_id = qq.id
          ), NULL)
        ) ORDER BY qq.sort_order
      )
      FROM questions qq
      WHERE qq.quiz_id = q.id
    ), '[]'::JSONB)
  ) INTO v_result
  FROM quizzes q
  WHERE q.id = p_quiz_id;

  RETURN v_result;
END;
$$;

GRANT EXECUTE ON FUNCTION public.get_quiz_with_questions(UUID, UUID) TO anon, authenticated;
