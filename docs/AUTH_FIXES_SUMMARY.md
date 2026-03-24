# Auth System Fixes — MindQuest V2

## Overview
This document summarizes all critical fixes applied to the authentication system to address use case audits for:
1. **UC1:** Anonymous signup → Link Google (single device)
2. **UC2:** Existing email prompt with proper UX

## Fixes Implemented (P0 & P1)

### Fix #1: Add `getCurrentUserEmail()` Method ✅
**Files Modified:**
- `AuthRepository.kt` (interface)
- `AuthRepositoryImpl.kt` (implementation)

**Change:**
Added a new method to extract the current user's email directly from the Supabase session without requiring a database lookup.

```kotlin
// Interface
suspend fun getCurrentUserEmail(): String?

// Implementation
override suspend fun getCurrentUserEmail(): String? {
    return supabaseClient.auth.currentSessionOrNull()?.user?.email
}
```

**Impact:** Enables downstream methods to access the authenticated user's email during account linking.

---

### Fix #2: Pass Email During Google Account Linking ✅
**Files Modified:**
- `AuthViewModel.kt` (2 locations)

**Changes:**
1. **Session observer case (line ~452):** When linking completes via browser redirect, the session observer now retrieves the email before calling `upsertUserRow()`.
2. **Inline case (line ~701):** When linking completes synchronously (web browsers), the same email retrieval is applied.

**Code:**
```kotlin
val email = authRepository.getCurrentUserEmail()
authRepository.upsertUserRow(
    userId = currentUser.id,
    ...
    email = email,  // ← NOW PASSED
)
```

**Impact:** Google email is now persisted to the users table after linking, preventing email loss.

---

### Fix #3: Fix Phone Linking Session Replacement ✅
**Files Modified:**
- `AuthViewModel.kt`

**Changes:**
1. **Added new state variable:** `pendingPhoneLinkUser: User?` to save the FULL USER OBJECT before phone sign-in replaces the session (not just the ID).
2. **Updated `linkPhoneStart()` (line 768-779):** Captures and validates the current user before session replacement, saves the complete User object.
3. **Updated `linkPhoneVerify()` (line 813-832):** Uses saved User object directly instead of calling `getCurrentUser()` (which would fail after session replacement).

**Why full User object?** After `signInWith(Phone)` replaces the session, `getCurrentUser()` would attempt to fetch the new phone user's profile. But the new phone user doesn't have a profile yet, causing a failure. By saving the full User object in advance, we have all the data we need without requiring a fetch.

**Code:**
```kotlin
// Private state variable
private var pendingPhoneLinkUser: User? = null

// In linkPhoneStart() - line 768-779
val originalUser = authRepository.getCurrentUser()
if (originalUser != null) {
    pendingPhoneLinkUser = originalUser
    AppLogger.d("MQ_AUTH", "linkPhoneStart: saved original user ${originalUser.id}")
}

// In linkPhoneVerify() - line 813-832
val originalUser = pendingPhoneLinkUser
if (originalUser != null) {
    val newProvider = when (originalUser.authProvider) {
        "google" -> "google_and_phone"
        "anonymous" -> "phone"
        else -> "google_and_phone"
    }
    authRepository.upsertUserRow(
        userId = originalUser.id,  // ← USE FROM SAVED OBJECT
        displayName = originalUser.displayName,
        avatarId = originalUser.avatarId,
        gradeId = originalUser.gradeId,
        authProvider = newProvider,
        phone = _phoneNumber.value,
    )
}
```

**Impact:** Phone linking no longer orphans the original user's data. The phone is correctly attached to the original user profile without any database lookups after session replacement.

---

### Fix #4: Sign Out Orphaned Auth User on Duplicate Detection ✅
**Files Modified:**
- `AuthViewModel.kt`

**Change:**
When duplicate email detection fails (user tries to sign up with existing email), the new Supabase auth user is immediately signed out to prevent orphaning.

```kotlin
// When existing.id != user.id (duplicate detected)
try {
    authRepository.signOut()
    AppLogger.d("MQ_AUTH", "Google: signed out orphaned auth user")
} catch (e: Exception) {
    AppLogger.e("MQ_AUTH", "Google: failed to sign out orphaned user", e)
}
```

**Impact:** Prevents Supabase auth table clutter from orphaned users. Database stays clean.

---

### Fix #5: Improve Duplicate Email Error UX with Recovery Dialog ✅
**Files Modified:**
- `AuthViewModel.kt` (added state & methods)
- `LoginJourneyScreen.kt` (added UI dialog)

**Changes:**

1. **New data class in AuthViewModel:**
```kotlin
data class DuplicateEmailError(
    val email: String,
)
```

2. **New StateFlow:**
```kotlin
private val _duplicateEmailError = MutableStateFlow<DuplicateEmailError?>(null)
val duplicateEmailError: StateFlow<DuplicateEmailError?> = _duplicateEmailError.asStateFlow()
```

3. **New recovery methods:**
```kotlin
fun handleDuplicateEmailSignInInstead() { /* dismiss dialog, go to user type */ }
fun handleDuplicateEmailRetry() { /* dismiss dialog, allow retry with different account */ }
```

4. **Updated duplicate detection to show error state:**
```kotlin
_duplicateEmailError.update { DuplicateEmailError(email = googleEmail) }
_authState.update { UiState.Empty }
```

5. **New UI Dialog in LoginJourneyScreen:**
```kotlin
if (duplicateEmailError != null) {
    DuplicateEmailDialog(
        email = duplicateEmailError!!.email,
        onSignInInstead = { viewModel.handleDuplicateEmailSignInInstead() },
        onRetry = { viewModel.handleDuplicateEmailRetry() },
    )
}
```

**Dialog Options:**
- **"Sign In Instead"** → Dismisses error and navigates back to USER_TYPE screen so user can choose "I already have an account"
- **"Use Different Account"** → Dismisses error and allows user to retry Google sign-in (will prompt for different Google account)

**Impact:** Clear, user-friendly error handling with actionable recovery paths.

---

## Testing Checklist

### UC1: Anonymous → Link Google
- [ ] Create anonymous account
- [ ] Verify `users` table has `email = NULL` for anon user
- [ ] Link with Google
- [ ] Verify email is now populated in `users` table
- [ ] Verify `auth_provider` updated to "google"

### UC1: Phone Linking
- [ ] Create anonymous account
- [ ] Start phone linking
- [ ] Verify `pendingPhoneLinkUserId` is saved
- [ ] Enter OTP
- [ ] Verify original user's row is updated (not a new phone user row)
- [ ] Verify `auth_provider` is now "phone"

### UC2: Duplicate Email
- [ ] Sign up as user A with google@example.com
- [ ] Sign out
- [ ] Try to sign up as user B with google@example.com → error dialog appears
- [ ] Verify orphaned auth user is signed out (check Supabase)
- [ ] Click "Sign In Instead" → redirects to USER_TYPE → EXISTING_LOGIN
- [ ] Sign in with google@example.com (user A)
- [ ] Go back and try again
- [ ] Click "Use Different Account"
- [ ] Select different Google account → new signup flow
- [ ] Verify new user is created with different email

---

## Files Changed Summary

| File | Changes |
|------|---------|
| `AuthRepository.kt` | +1 method interface |
| `AuthRepositoryImpl.kt` | +1 method implementation |
| `AuthViewModel.kt` | +3 data classes, +2 StateFlows, +5 methods, +4 logic updates |
| `LoginJourneyScreen.kt` | +1 state collection, +1 dialog display, +1 composable function |

---

## Known Limitations (P2)

The following items remain as P2 (lower priority):

### Anonymous Session Recovery
- Supabase JWT tokens expire in 1 hour with refresh tokens
- If app is force-closed and cache cleared, anonymous session is unrecoverable
- **Workaround:** Users should link an account before uninstalling or clearing app data
- **Future fix:** Implement offline session persistence or auto-recovery flow

### OTP Verification in Direct Phone Linking
- `linkAccountWithPhone()` in repository just calls `updateUser()` without OTP verification
- **Note:** The phone linking flow via sheets (which is user-facing) DOES verify OTP correctly
- **Future fix:** Add proper OTP verification to direct phone linking method

---

## Verification Commands

```bash
# Check email passing in linking
grep -n "email = email" composeApp/src/commonMain/kotlin/com/android/mindquest/presentation/auth/AuthViewModel.kt

# Check phone linking fix
grep -n "pendingPhoneLinkUser = originalUser" composeApp/src/commonMain/kotlin/com/android/mindquest/presentation/auth/AuthViewModel.kt

# Check duplicate handling
grep -n "signOut()" composeApp/src/commonMain/kotlin/com/android/mindquest/presentation/auth/AuthViewModel.kt

# Check UI dialog
grep -n "DuplicateEmailDialog" composeApp/src/commonMain/kotlin/com/android/mindquest/presentation/auth/LoginJourneyScreen.kt
```

---

## Next Steps

1. **Test all fixes** using the testing checklist above
2. **Build and run** the app to verify no compilation errors
3. **Manual QA** on both Android and iOS
4. **Monitor logs** for any auth-related exceptions in production
5. **Plan P2 fixes** for future release (session recovery, OTP verification)
