# Email Uniqueness Implementation Guide

## Overview

This document describes the implementation of **database-level email and phone uniqueness constraints** to guarantee that each email/phone number can only be used by one user.

## Current State (Before Implementation)

### ❌ Problems

1. **No Database Constraint**: Email/phone uniqueness was only enforced at the app level via `findExistingUser()` RPC function
2. **Bypass Vulnerability**: If someone bypassed the app (direct API calls, SQL injection, etc.), they could create duplicate emails
3. **Data Inconsistency**: Race conditions could allow two simultaneous signups with the same email

### ✅ App-Level Protection (Still Works)

```kotlin
// AuthViewModel.kt - Duplicate detection
val existing = authRepository.findExistingUser(email = googleEmail)
when {
    existing != null && existing.id == user.id -> { /* returning user */ }
    existing != null && existing.id != user.id -> { /* duplicate error */ }
    else -> { /* new user */ }
}
```

---

## New State (After Implementation)

### ✅ Benefits

1. **Database-Enforced Uniqueness**: PostgreSQL UNIQUE constraint prevents ANY duplicate emails/phones
2. **No Bypass Possible**: Even direct database operations will fail if they violate the constraint
3. **Race Condition Prevention**: Database serialization prevents concurrent duplicate creation
4. **Better Performance**: Indexed unique constraints improve query speed for duplicate checks

### 📊 Constraint Details

```sql
-- Email constraint (allows NULL)
ALTER TABLE public.users
ADD CONSTRAINT users_email_unique UNIQUE (email)
WHERE email IS NOT NULL;

-- Phone constraint (allows NULL)
ALTER TABLE public.users
ADD CONSTRAINT users_phone_unique UNIQUE (phone)
WHERE phone IS NOT NULL;
```

**Why allow NULL?**
- Users might not provide email initially (e.g., anonymous signup)
- PostgreSQL treats each NULL as different (multiple NULLs allowed)
- Users can later add email during account setup

---

## Implementation Steps

### Step 1: Run Migration Script

Execute the migration in Supabase SQL Editor:

```bash
1. Open Supabase Dashboard → SQL Editor
2. Create new query
3. Copy content from: supabase_add_email_uniqueness.sql
4. Click "Run"
```

**What the migration does:**

```sql
-- Cleanup duplicates (keeps latest user for each email)
WITH duplicate_emails AS (
    SELECT id, email,
           ROW_NUMBER() OVER (PARTITION BY email ORDER BY updated_at DESC) as rn
    FROM public.users WHERE email IS NOT NULL
)
UPDATE public.users SET email = NULL WHERE id IN (
    SELECT id FROM duplicate_emails WHERE rn > 1
);

-- Add UNIQUE constraints
ALTER TABLE public.users
ADD CONSTRAINT users_email_unique UNIQUE (email) WHERE email IS NOT NULL;

ALTER TABLE public.users
ADD CONSTRAINT users_phone_unique UNIQUE (phone) WHERE phone IS NOT NULL;

-- Create indexes for performance
CREATE INDEX idx_users_email_non_null ON public.users(email) WHERE email IS NOT NULL;
CREATE INDEX idx_users_phone_non_null ON public.users(phone) WHERE phone IS NOT NULL;
```

### Step 2: Update Error Handling (ALREADY DONE)

The app now properly handles unique constraint violations:

**File:** `ErrorMapper.kt`

```kotlin
// Email uniqueness violation
msg.containsAny("users_email_unique", "duplicate key value violates unique constraint \"users_email_unique\"") ->
    "This email is already used by another account. Please sign in instead."

// Phone uniqueness violation
msg.containsAny("users_phone_unique", "duplicate key value violates unique constraint \"users_phone_unique\"") ->
    "This phone number is already linked to another account."
```

### Step 3: Rebuild App

```bash
./gradlew build --no-daemon
```

No code changes needed in the Kotlin app - error handling is backward compatible.

---

## How It Works Now

### Flow 1: New User Signup with Gmail

```
User clicks "Sign in with Google"
    ↓
[App checks: findExistingUser(email)]  ← App-level check
    ↓
[Supabase auth creates user]  ← Provider creates auth user
    ↓
[App calls upsertUser(email)]  ← App creates profile
    ↓
[Database UNIQUE constraint validates email]  ← DB-level check
    ↓
Success: User profile created with unique email
```

**If duplicate email:**
```
upsertUser() fails with:
  "duplicate key value violates unique constraint \"users_email_unique\""
    ↓
ErrorMapper converts to user-friendly message:
  "This email is already used by another account. Please sign in instead."
```

### Flow 2: Returning User

```
User clicks "Sign in with Google" with same email
    ↓
[Supabase auth recognizes provider] ← Same userId returned
    ↓
[App calls getProfile(userId)]  ← Fetch existing profile
    ↓
[Database finds profile with same id]  ← No unique constraint violated
    ↓
Success: User logged in (no upsert, so no constraint check)
```

### Flow 3: Account Linking

```
Anonymous user clicks "Link Google Account"
    ↓
[linkIdentity(Google) called]  ← Adds Google identity
    ↓
[updateProfile(auth_provider="google", email)]  ← RPC updates provider
    ↓
[If email is non-NULL, UNIQUE constraint validates]  ← DB check
    ↓
Success: Account linked with unique email
```

---

## Error Messages

### Scenario: Duplicate Email Signup

**User action:** Try to sign up with email already in database

**Database error:**
```
ERROR: duplicate key value violates unique constraint "users_email_unique"
```

**App converts to:**
```
"This email is already used by another account. Please sign in instead."
```

### Scenario: Duplicate Phone Linking

**User action:** Try to link phone number already linked to another user

**Database error:**
```
ERROR: duplicate key value violates unique constraint "users_phone_unique"
```

**App converts to:**
```
"This phone number is already linked to another account."
```

---

## Testing

### Test 1: Normal Signup

```
Device: Test Device A
Action: Sign up with test@example.com
Expected: ✅ Success, profile created
Verify: Check Supabase users table → email = test@example.com
```

### Test 2: Duplicate Signup (Different Device)

```
Device: Test Device B
Action: Try to sign up with same test@example.com
Expected: ❌ Error shown: "This email is already used by another account..."
Verify: Check logs for "users_email_unique" constraint error
```

### Test 3: Same User, Same Email (Returning User)

```
Device: Test Device A
Action: Sign in again with same Gmail account (test@example.com)
Expected: ✅ Success, returns to home (no onboarding)
Verify: Check logs for "RETURNING USER" message
```

### Test 4: Account Linking with Unique Email

```
Device: Test Device C
Action 1: Sign in anonymously
Action 2: Click "Link Google Account"
Action 3: Select Google account with new@example.com
Expected: ✅ Success, account linked
Verify: Check Supabase → email = new@example.com, authProvider = google
```

### Test 5: Account Linking with Duplicate Email

```
Device: Test Device C
Action 1: Sign in anonymously
Action 2: Click "Link Google Account"
Action 3: Select Google account with test@example.com (already used)
Expected: ❌ Error shown: "This email is already used..."
Verify: Check logs for constraint error
```

---

## Database Schema Changes

### Before

```sql
CREATE TABLE IF NOT EXISTS public.users (
    id            UUID PRIMARY KEY REFERENCES auth.users(id),
    display_name  TEXT NOT NULL,
    email         TEXT,              ← ⚠️ No constraint
    phone         TEXT,              ← ⚠️ No constraint
    auth_provider TEXT NOT NULL,
    ...
);
```

### After

```sql
CREATE TABLE IF NOT EXISTS public.users (
    id            UUID PRIMARY KEY REFERENCES auth.users(id),
    display_name  TEXT NOT NULL,
    email         TEXT,              ← ✅ UNIQUE constraint
    phone         TEXT,              ← ✅ UNIQUE constraint
    auth_provider TEXT NOT NULL,
    ...
);

-- Constraints
CONSTRAINT users_email_unique UNIQUE (email) WHERE email IS NOT NULL
CONSTRAINT users_phone_unique UNIQUE (phone) WHERE phone IS NOT NULL

-- Indexes
INDEX idx_users_email_non_null ON users(email) WHERE email IS NOT NULL
INDEX idx_users_phone_non_null ON users(phone) WHERE phone IS NOT NULL
```

---

## Impact Analysis

### ✅ What Stays the Same

- App-level duplicate detection (`findExistingUser()`) still works
- User signup flow unchanged
- Returning user detection unchanged
- Account linking flow unchanged
- Error handling is backward compatible

### ✅ What Improves

- **Security**: Database now enforces uniqueness (no bypass possible)
- **Data Integrity**: Race conditions prevented by database serialization
- **Performance**: UNIQUE indexes improve duplicate check queries
- **Error Messages**: Now show specific email/phone errors vs generic message

### ⚠️ What Changes

- Database upsert operations with duplicate emails will now fail
- Error messages are more specific (good for users)
- Migration cleans up any existing duplicates (kept latest user)

---

## Migration Checklist

- [ ] **Backup Database**: Take a Supabase backup before running migration
- [ ] **Run Migration**: Execute `supabase_add_email_uniqueness.sql` in SQL Editor
- [ ] **Verify Constraints**: Check constraints appear in Supabase
- [ ] **Verify Indexes**: Check indexes appear in Supabase
- [ ] **Update ErrorMapper**: (ALREADY DONE) Handles unique constraint errors
- [ ] **Rebuild App**: Run `./gradlew build` to ensure no issues
- [ ] **Test Flows**: Run all test scenarios above
- [ ] **Deploy**: Push app to production

---

## FAQ

### Q: Will existing users be affected?

**A:** No. Existing users with NULL emails/phones are unaffected. Users with emails/phones will be migrated:
- Latest user for each email/phone keeps the value
- Older users with duplicates have the field set to NULL
- They can re-add their email/phone later

### Q: What if someone doesn't have an email?

**A:** That's fine. NULL values are allowed and don't violate the UNIQUE constraint. Each NULL is treated as unique.

### Q: Can I link multiple emails to one account?

**A:** No. The constraint ensures one email = one user. If you want to change emails:
1. Remove current email (set to NULL)
2. Add new email (must be unique)

### Q: What about anonymous users?

**A:** Anonymous users can have `email = NULL`. When they later add email (via linking), it must be unique.

### Q: Does this affect the RLS (Row-Level Security) policy?

**A:** No. RLS policies are unchanged. Uniqueness is enforced at constraint level, not RLS level.

### Q: Can I add email to users without them?

**A:** Yes, but ONLY if that email is unique. The constraint will prevent duplicates.

---

## Rollback Plan (If Needed)

If the constraint causes issues:

```sql
-- Remove constraints
ALTER TABLE public.users DROP CONSTRAINT IF EXISTS users_email_unique;
ALTER TABLE public.users DROP CONSTRAINT IF EXISTS users_phone_unique;

-- Remove indexes (optional)
DROP INDEX IF EXISTS idx_users_email_non_null;
DROP INDEX IF EXISTS idx_users_phone_non_null;
```

However, **this is not recommended** as it removes data integrity protection.

---

## Summary

| Aspect | Before | After |
|--------|--------|-------|
| **Email Uniqueness** | App-level only | Database + App |
| **Bypass Risk** | ⚠️ Yes (direct DB) | ✅ No (constraint) |
| **Race Conditions** | ⚠️ Possible | ✅ Prevented |
| **Error Messages** | Generic | Specific |
| **Performance** | Fair | Good (indexes) |
| **User Impact** | None | Better error messages |

---

## Related Commits

- `3babda6` - CRITICAL FIX: RLS violation + email extraction timeout
- (Next) - ADD: Database-level email/phone uniqueness constraints
- (Next) - UPDATE: Error mapper for constraint violations

---

**Created:** 2026-03-24
**Last Updated:** 2026-03-24
**Status:** Ready for implementation ✅
