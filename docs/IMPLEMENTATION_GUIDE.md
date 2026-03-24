# Authentication System - Complete Implementation Guide

## Overview

This guide walks through the complete authentication system fixes for the MindQuest app. All code fixes have been applied. This document covers:

1. ✅ What was fixed in the Kotlin code
2. ⚠️ What needs to be done in Supabase
3. 🧪 How to test the complete flow
4. 📊 What to verify after implementation

---

## Phase 1: Code Changes - ✅ COMPLETE

### Files Modified
- **AuthRepositoryImpl.kt** - Core authentication logic

### Fixes Applied

#### Fix #1: Email/Phone Now Returned in User Object
```kotlin
// NOW INCLUDES:
val user = User(
    id = userId,
    displayName = profile.user.displayName,
    email = profile.user.email,           // ✅ NEW
    phone = profile.user.phone,           // ✅ NEW
    countryId = profile.user.countryId,   // ✅ NEW
    cityId = profile.user.cityId,         // ✅ NEW
    // ... other fields
)
```
**Affected Functions**:
- `getCurrentUser()`
- `observeAuthState()`
- `signInWithGoogle()`
- `verifyOtp()`

#### Fix #2: Account Linking Now Updates Database
```kotlin
// After linking providers, auth_provider is updated:
override suspend fun linkAccountWithGoogle(): Resource<Unit> {
    supabaseClient.auth.linkIdentity(Google)

    // ✅ NEW: Update database with new auth_provider
    val newAuthProvider = determineAuthProvider()
    apiService.updateProfile(userId, buildJsonObject {
        put("auth_provider", JsonPrimitive(newAuthProvider))
    })
}
```
**Affected Functions**:
- `linkAccountWithGoogle()`
- `linkAccountWithPhone()`
- `linkPhoneVerify()`

#### Fix #3: New Helper Function for Provider Detection
```kotlin
private suspend fun determineAuthProvider(): String {
    val session = supabaseClient.auth.currentSessionOrNull()
    val identities = session?.user?.identities
    val hasGoogle = identities?.any { it.provider == "google" } ?: false
    val hasPhone = !session?.user?.phone.isNullOrBlank()

    return when {
        hasGoogle && hasPhone -> "google_and_phone"
        hasGoogle -> "google"
        hasPhone -> "phone"
        else -> "anonymous"
    }
}
```

#### Fix #4: Enhanced isAnonymous() Check
```kotlin
override suspend fun isAnonymous(): Boolean {
    val sessionIsAnon = session?.user?.email.isNullOrBlank() && session?.user?.phone.isNullOrBlank()
    val profileIsAnon = profile?.user?.authProvider == "anonymous"

    return sessionIsAnon && profileIsAnon  // ✅ Both must be true
}
```

---

## Phase 2: Supabase SQL Update - ⚠️ NEXT STEP

### What Needs to Be Done

The `get_profile()` function in Supabase needs to be updated to return `email`, `phone`, `country_id`, and `city_id`.

### Step-by-Step Instructions

#### Option A: Using Supabase SQL Editor (Recommended)

1. **Open Supabase Dashboard**
   - Go to [app.supabase.com](https://app.supabase.com)
   - Select your MindQuest project
   - Navigate to **SQL Editor**

2. **Copy the SQL Command**
   - Open file: `docs/SUPABASE_PROFILE_FIX.sql`
   - Copy the entire content

3. **Execute in Supabase**
   - Paste into SQL Editor
   - Click **Run** button
   - Wait for confirmation: "Function created successfully"

4. **Verify the Update**
   ```sql
   -- Test with a real user ID
   SELECT public.get_profile('your-user-id'::uuid);
   ```

#### Option B: Using psql CLI

```bash
# Connect to your Supabase database
psql "postgresql://postgres:[password]@[host]:[port]/postgres"

# Execute the SQL file
\i docs/SUPABASE_PROFILE_FIX.sql

# Verify
SELECT public.get_profile('test-user-id'::uuid);
```

---

## Phase 3: Testing & Verification

### Before Testing

Make sure:
- ✅ Code changes are committed and deployed
- ✅ SQL update is applied to Supabase
- ✅ App is rebuilt with new code

### Test Scenario 1: Anonymous Signup
```
1. Launch app
2. Tap "Continue as Guest"
3. Go through onboarding
4. Check profile screen:
   - Auth method shows: "Anonymous"
   - Email field: empty/null
   - Phone field: empty/null
```

### Test Scenario 2: Google Signup
```
1. Launch app
2. Tap "Sign in with Google"
3. Complete Google OAuth
4. Go through onboarding
5. Check profile screen:
   - Auth method shows: "Google"
   - Email field: shows Google email ✅
   - Phone field: empty/null
   - Display name: from Google profile
```

### Test Scenario 3: Anonymous → Google Link
```
1. Start as anonymous guest
2. Go to profile screen
3. Tap "Link Google Account"
4. Complete Google OAuth
5. Verify:
   - Auth method shows: "Google + More options"
   - Email field: shows Google email ✅
   - Phone field: empty/null
   - auth_provider in DB: "google_and_phone"
```

### Test Scenario 4: Anonymous → Phone Link (Hidden, Code-Ready)
```
1. Start as anonymous guest
2. (Programmatically trigger phone linking UI when enabled)
3. Enter phone number
4. Verify OTP
5. Verify:
   - Auth method shows: "Phone + Google"
   - Phone field: shows linked phone ✅
   - auth_provider in DB: "google_and_phone"
```

### Test Scenario 5: Session Persistence
```
1. Sign in with Google
2. Navigate to profile
3. Verify email shows ✅
4. Force close app
5. Reopen app
6. Navigate to profile
7. Verify email still shows (from DB) ✅
```

### Verification Queries

Run these in Supabase SQL Editor to verify:

```sql
-- 1. Verify function exists and has correct parameters
SELECT routine_name, routine_definition
FROM information_schema.routines
WHERE routine_schema = 'public' AND routine_name = 'get_profile';

-- 2. Test function returns email and phone
SELECT
  (public.get_profile('test-user-id'::uuid) -> 'user' ->> 'email') as user_email,
  (public.get_profile('test-user-id'::uuid) -> 'user' ->> 'phone') as user_phone,
  (public.get_profile('test-user-id'::uuid) -> 'user' ->> 'auth_provider') as auth_provider;

-- 3. Check a specific user's profile data
SELECT * FROM public.users WHERE id = 'test-user-id'::uuid;

-- 4. Verify auth_provider values for different user types
SELECT id, auth_provider, email, phone
FROM public.users
WHERE auth_provider IN ('google', 'phone', 'anonymous', 'google_and_phone')
LIMIT 10;
```

---

## Phase 4: Monitoring & Debugging

### Enable Detailed Logging

The code already logs with tags:
- `MQ_AUTH` - Authentication flow details
- `MQ_DB` - Database operations

Check Logcat/Console for:
```
MQ_AUTH: AuthRepo: Google metadata — name='...', email='...', avatarUrl=true
MQ_DB: linkAccountWithGoogle: updating auth_provider to=google_and_phone
MQ_DB: linkAccountWithPhone: updating database for userId=xxx, phone=+1234567890
```

### Common Issues & Solutions

#### Issue: Email not showing on profile screen
**Check**:
1. ✅ SQL update applied to get_profile()
2. ✅ Verify function returns email in response
3. ✅ Profile screen code includes email field
4. ✅ User logged in with Google (anonymous users won't have email)

#### Issue: Auth provider shows "anonymous" after linking
**Check**:
1. ✅ linkAccountWithGoogle() or linkPhoneVerify() was called
2. ✅ determineAuthProvider() is being executed
3. ✅ updateProfile() was called with new auth_provider
4. ✅ Check database: `SELECT auth_provider FROM public.users WHERE id='user-id'`

#### Issue: Phone not persisting after OTP
**Check**:
1. ✅ verifyOtp() returns User with phone field set
2. ✅ upsertUserRow() is called with phone parameter during onboarding
3. ✅ Check database: `SELECT phone FROM public.users WHERE id='user-id'`

---

## Deployment Checklist

### Pre-Deployment
- [ ] All code changes reviewed and tested locally
- [ ] SQL script tested in Supabase staging environment
- [ ] No breaking changes to existing user data
- [ ] All authentication flows tested manually
- [ ] Logging in place for debugging

### Deployment Steps
1. [ ] Deploy code to production
2. [ ] Execute SQL update in production Supabase
3. [ ] Test with real user accounts
4. [ ] Monitor logs for errors
5. [ ] Verify all auth methods work

### Post-Deployment
- [ ] Monitor crash reports and error logs
- [ ] Check user feedback for auth issues
- [ ] Verify profile screens display correctly
- [ ] Confirm email/phone visibility as expected
- [ ] Review database for auth_provider updates

---

## Summary of Changes

### Kotlin Code Changes
| Component | Change | Impact |
|-----------|--------|--------|
| getCurrentUser() | Added email, phone, countryId, cityId | Profile has more data |
| observeAuthState() | Added email, phone, countryId, cityId | Session observer includes contact info |
| signInWithGoogle() | Enhanced profile data extraction | Google info logged and stored |
| verifyOtp() | Added phone to User object | Phone signup ready |
| linkAccountWithGoogle() | Added database update | Auth provider synced |
| linkAccountWithPhone() | Added database update | Phone & provider synced |
| linkPhoneVerify() | Added database update | Phone & provider synced |
| isAnonymous() | Enhanced with profile check | More accurate detection |
| determineAuthProvider() | New helper function | Proper provider determination |

### Database Changes Required
| Field | Current | After SQL Update |
|-------|---------|------------------|
| get_profile() return | No email/phone | ✅ Includes email, phone, country_id, city_id |

### User Experience Improvements
- ✅ Profile screen shows email from Google signup
- ✅ Profile screen shows phone after phone linking
- ✅ Auth method clearly displayed (Google, Phone, Anonymous, or Combined)
- ✅ Location info (country, city) available for display
- ✅ All data persists correctly after logout/login

---

## Contact & Support

For issues or questions:
1. Check logs with tags `MQ_AUTH` and `MQ_DB`
2. Review this guide's "Common Issues" section
3. Run verification queries in Supabase
4. Review the AUTH_FIXES_COMPLETE.md document

---

**Status**: Code complete, ready for SQL deployment ✅
