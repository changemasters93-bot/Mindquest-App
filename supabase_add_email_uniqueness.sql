-- ╔══════════════════════════════════════════════════════════════════╗
-- ║  ADD EMAIL & PHONE UNIQUENESS CONSTRAINTS                        ║
-- ║  This migration adds UNIQUE constraints to email and phone       ║
-- ║  columns to guarantee data uniqueness at the database level.     ║
-- ║  Run this in the Supabase SQL Editor                            ║
-- ╚══════════════════════════════════════════════════════════════════╝

-- ═══════════════════════════════════════════════════════════════════
-- 1. CLEANUP: Remove existing duplicate email/phone (keep latest)
-- ═══════════════════════════════════════════════════════════════════

-- Create a CTE to identify duplicates (keep the most recent version)
WITH duplicate_emails AS (
    SELECT
        id,
        email,
        ROW_NUMBER() OVER (PARTITION BY email ORDER BY updated_at DESC, created_at DESC) as rn
    FROM public.users
    WHERE email IS NOT NULL
        AND email != ''
        AND TRIM(email) != ''
)
UPDATE public.users
SET email = NULL
WHERE id IN (
    SELECT id FROM duplicate_emails WHERE rn > 1
);

-- Similar cleanup for phone
WITH duplicate_phones AS (
    SELECT
        id,
        phone,
        ROW_NUMBER() OVER (PARTITION BY phone ORDER BY updated_at DESC, created_at DESC) as rn
    FROM public.users
    WHERE phone IS NOT NULL
        AND phone != ''
        AND TRIM(phone) != ''
)
UPDATE public.users
SET phone = NULL
WHERE id IN (
    SELECT id FROM duplicate_phones WHERE rn > 1
);

-- Log what was cleaned up
DO $$
DECLARE
    v_null_emails INT;
    v_null_phones INT;
BEGIN
    SELECT COUNT(*) INTO v_null_emails FROM public.users WHERE email IS NULL;
    SELECT COUNT(*) INTO v_null_phones FROM public.users WHERE phone IS NULL;
    RAISE NOTICE 'After cleanup: % users have NULL email, % users have NULL phone', v_null_emails, v_null_phones;
END $$;

-- ═══════════════════════════════════════════════════════════════════
-- 2. ADD UNIQUE CONSTRAINTS
-- ═══════════════════════════════════════════════════════════════════

-- Drop existing constraints if they exist (for re-running this script)
ALTER TABLE public.users DROP CONSTRAINT IF EXISTS users_email_unique;
ALTER TABLE public.users DROP CONSTRAINT IF EXISTS users_phone_unique;

-- Add UNIQUE constraint on email
-- Note: PostgreSQL treats NULL as different, so multiple NULL emails are allowed
ALTER TABLE public.users
ADD CONSTRAINT users_email_unique UNIQUE (email)
WHERE email IS NOT NULL;

-- Add UNIQUE constraint on phone
ALTER TABLE public.users
ADD CONSTRAINT users_phone_unique UNIQUE (phone)
WHERE phone IS NOT NULL;

-- ═══════════════════════════════════════════════════════════════════
-- 3. CREATE INDEXES FOR PERFORMANCE
-- ═══════════════════════════════════════════════════════════════════

-- Index for email lookups (useful for findUserByEmail queries)
CREATE INDEX IF NOT EXISTS idx_users_email_non_null ON public.users(email) WHERE email IS NOT NULL;

-- Index for phone lookups (useful for findUserByPhone queries)
CREATE INDEX IF NOT EXISTS idx_users_phone_non_null ON public.users(phone) WHERE phone IS NOT NULL;

-- Index for auth_provider lookups
CREATE INDEX IF NOT EXISTS idx_users_auth_provider ON public.users(auth_provider);

-- Combined index for findExistingUser queries
CREATE INDEX IF NOT EXISTS idx_users_email_phone ON public.users(email, phone) WHERE email IS NOT NULL OR phone IS NOT NULL;

-- ═══════════════════════════════════════════════════════════════════
-- 4. VERIFY CONSTRAINTS
-- ═══════════════════════════════════════════════════════════════════

-- Show all constraints on users table
SELECT constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_name = 'users'
ORDER BY constraint_name;

-- Show all indexes on users table
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'users'
AND indexname LIKE 'idx_%'
ORDER BY indexname;

-- ═══════════════════════════════════════════════════════════════════
-- 5. TEST THE CONSTRAINTS
-- ═══════════════════════════════════════════════════════════════════

-- This will demonstrate what happens when you try to insert a duplicate email
-- Uncomment to test (will fail if duplicate exists):
/*
INSERT INTO public.users (
    id, display_name, avatar_id, grade_id, auth_provider, email
) VALUES (
    uuid_generate_v4(),
    'Test User',
    1,
    (SELECT id FROM public.grades LIMIT 1),
    'anonymous',
    'duplicate@example.com'  -- This will fail if another user has this email
) ON CONFLICT DO NOTHING;
*/

-- ═══════════════════════════════════════════════════════════════════
-- SUMMARY OF CHANGES
-- ═══════════════════════════════════════════════════════════════════
-- ✅ Email field: UNIQUE constraint added (allows NULL)
-- ✅ Phone field: UNIQUE constraint added (allows NULL)
-- ✅ Duplicate emails/phones: Cleaned up (kept latest, nullified duplicates)
-- ✅ Indexes: Added for query performance on email, phone, auth_provider
--
-- BEHAVIOR:
-- - Same email can be used only by ONE user
-- - Multiple NULL emails are allowed (for users without email)
-- - App-level duplicate detection still works as backup
-- - Database now enforces email uniqueness (no app bypass possible)
-- ═══════════════════════════════════════════════════════════════════
