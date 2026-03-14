-- =========================================================================
-- Migration: Make grade_id NOT NULL again
-- =========================================================================
-- grade_id is the main driver of quiz content, tournaments, and all
-- grade-based features. It must always be populated.
--
-- PREREQUISITE: Ensure all existing users have a valid grade_id before running.
-- Run this to check: SELECT id FROM public.users WHERE grade_id IS NULL;
-- If any rows are found, update them first:
--   UPDATE public.users SET grade_id = '<a-valid-grade-uuid>' WHERE grade_id IS NULL;
-- =========================================================================

-- Step 1: Ensure no nulls exist (safety check — will fail if nulls remain)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.users WHERE grade_id IS NULL) THEN
        RAISE EXCEPTION 'Cannot set NOT NULL: some users have NULL grade_id. Fix them first.';
    END IF;
END $$;

-- Step 2: Set NOT NULL constraint
ALTER TABLE public.users ALTER COLUMN grade_id SET NOT NULL;
