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
-- 2. ADD UNIQUE CONSTRAINTS (using partial indexes)
-- ═══════════════════════════════════════════════════════════════════

-- Drop existing indexes if they exist (for re-running this script)
DROP INDEX IF EXISTS users_email_unique;
DROP INDEX IF EXISTS users_phone_unique;

-- Add UNIQUE constraint on email using PARTIAL INDEX
-- Note: PostgreSQL treats NULL as different, so multiple NULL emails are allowed
-- Only non-NULL emails must be unique
CREATE UNIQUE INDEX users_email_unique ON public.users(email)
WHERE email IS NOT NULL;

-- Add UNIQUE constraint on phone using PARTIAL INDEX
-- Only non-NULL phones must be unique
CREATE UNIQUE INDEX users_phone_unique ON public.users(phone)
WHERE phone IS NOT NULL;

-- ═══════════════════════════════════════════════════════════════════
-- 3. CREATE ADDITIONAL INDEXES FOR PERFORMANCE
-- ═══════════════════════════════════════════════════════════════════

-- Note: users_email_unique and users_phone_unique are already created in section 2
-- These additional indexes help with other queries

-- Index for auth_provider lookups
CREATE INDEX IF NOT EXISTS idx_users_auth_provider ON public.users(auth_provider);

-- Combined index for findExistingUser queries
CREATE INDEX IF NOT EXISTS idx_users_email_phone ON public.users(email, phone) WHERE email IS NOT NULL OR phone IS NOT NULL;

-- ═══════════════════════════════════════════════════════════════════
-- 4. VERIFY INDEXES (UNIQUE CONSTRAINTS)
-- ═══════════════════════════════════════════════════════════════════

-- Show all indexes on users table (includes UNIQUE indexes for constraints)
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'users'
ORDER BY indexname;

-- Verify the UNIQUE indexes exist for email and phone
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'users'
AND (indexname = 'users_email_unique' OR indexname = 'users_phone_unique');

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
-- ✅ Email field: UNIQUE constraint added via partial index (allows NULL)
-- ✅ Phone field: UNIQUE constraint added via partial index (allows NULL)
-- ✅ Duplicate emails/phones: Cleaned up (kept latest, nullified duplicates)
-- ✅ Partial Indexes: Only non-NULL emails/phones must be unique
-- ✅ Additional Indexes: Added for query performance on auth_provider
--
-- BEHAVIOR:
-- - Same non-NULL email can be used only by ONE user
-- - Multiple NULL emails are allowed (PostgreSQL treats each NULL as distinct)
-- - Same non-NULL phone can be used only by ONE user
-- - Multiple NULL phones are allowed
-- - App-level duplicate detection still works as backup
-- - Database now enforces email/phone uniqueness (no app bypass possible)
--
-- TECHNICAL DETAILS:
-- - Uses PostgreSQL PARTIAL UNIQUE INDEX (not traditional UNIQUE constraint)
-- - Partial indexes only enforce uniqueness WHERE email IS NOT NULL
-- - This allows multiple NULL values (which would fail with traditional constraint)
-- ═══════════════════════════════════════════════════════════════════
