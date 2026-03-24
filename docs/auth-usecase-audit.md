# Authentication Use-Case Audit

**Date**: 2026-03-21
**Scope**: All 17 auth use cases across ViewModel, Repository, Database, and Supabase config

---

## Summary

| Status | Count | Use Cases |
|--------|-------|-----------|
| ✅ Handled | 10 | #1, #2, #5, #6, #7, #8, #9, #10, #11, #15 |
| ⚠️ Partial | 5 | #3, #4, #12, #13, #14 |
| ❌ Not handled | 2 | #16 (seamless linking UX), #17 (guest auto-merge) |

---

## Detailed Status

### ✅ Fully Handled

| # | Use Case | Implementation |
|---|----------|---------------|
| 1 | First-time signup (Google/Phone/Anonymous) | `AuthViewModel.signInWithGoogle()`, `sendOtp()/verifyOtp()`, `signInAnonymously()` → `upsertUserRow()` |
| 2 | Returning user login | `checkSession()` → `getCurrentUser()` + `sessionPrefs.isLoggedIn` → route to HOME |
| 5 | Google without phone | Google OAuth returns email only; phone is optional |
| 6 | Phone without email | Phone OTP works without email; email is optional |
| 7 | OTP failures | `ErrorMapper` handles invalid/expired/rate-limited OTP; 30s resend timer; auto-verify on 6 digits |
| 8 | Google auth failure | try-catch + `ErrorMapper` for network/SSL/auth errors; 60s timeout resets Loading state; session observer fallback |
| 9 | Session expiry | `ErrorMapper` catches "jwt expired"/"not authenticated"; Supabase SDK auto-refresh enabled by default |
| 10 | Logout | `signOut()` → `supabaseClient.auth.signOut()` + `sessionPrefs.clear()`; `lastAuthProvider` preserved |
| 11 | Phone verification | OTP is implicit verification; Supabase marks phone as verified in `auth.users` |
| 15 | Smart login suggestion | `SessionPrefs.lastAuthProvider` persists last used provider across sessions |

### ⚠️ Partially Handled

| # | Use Case | What Works | What's Missing |
|---|----------|-----------|----------------|
| 3 | Account linking | `linkAccount("google")` → `linkIdentity(Google)` + updates `auth_provider` in `public.users` | Phone linking not wired through OTP flow |
| 4 | Duplicate prevention | Supabase email/phone uniqueness constraints; `ErrorMapper` handles "already exists" | No app-level pre-check; no auto-merge prompt |
| 12 | Google token validation | Supabase handles server-side JWT validation | No certificate pinning |
| 13 | Account takeover protection | `ErrorMapper` catches "identity already linked" | No device tracking, session revocation, or login activity log |
| 14 | Multi-device | Same account works across devices via Supabase; Realtime subscriptions for sync | No session list/management, no offline conflict resolution |

### ❌ Not Yet Handled (Post-MVP)

| # | Use Case | What's Needed |
|---|----------|--------------|
| 16 | Seamless linking flow | Show "You already signed up with phone. Continue and link Google?" instead of generic error. Wire phone linking through OTP. Auto-resume action after linking. |
| 17 | Guest auto-merge | Detect when guest signs in with Google/Phone matching existing account; offer merge instead of creating new account. |

---

## Supabase Schema

### `public.users` Table
```sql
id              UUID PK → auth.users(id) CASCADE
display_name    TEXT NOT NULL
avatar_id       INT DEFAULT 4
grade_id        UUID → grades(id)
auth_provider   TEXT CHECK ('google','phone','anonymous','google_and_phone')
google_sub      TEXT
email           TEXT           -- NEW: for duplicate detection
phone           TEXT           -- NEW: for duplicate detection
is_email_verified BOOLEAN      -- NEW: for verification tracking
is_phone_verified BOOLEAN      -- NEW: for verification tracking
country_id      UUID
city_id         UUID
school_name     TEXT
total_xp        BIGINT DEFAULT 0
level           INT DEFAULT 1
streak_current  INT DEFAULT 0
streak_best     INT DEFAULT 0
```

### RLS Policies
- `users_select_own`: SELECT where `auth.uid() = id`
- `users_insert_own`: INSERT where `auth.uid() = id`
- `users_update_own`: UPDATE where `auth.uid() = id`

### `update_profile` RPC Whitelist
Allowed fields: `display_name`, `avatar_id`, `grade_id`, `school_name`, `country_id`, `city_id`, `auth_provider`

---

## Manual Steps Required

### Supabase Dashboard
1. ✅ Google provider enabled with OAuth credentials
2. ✅ `mindquest://callback` added to Redirect URLs
3. ✅ Skip nonce checks enabled (for iOS)
4. **TODO**: Enable "Allow manual linking" toggle (Authentication → Sign In / Providers)
5. **TODO**: Enable Phone provider with SMS credentials (Twilio/Vonage)
6. **TODO**: Run SQL migration for `email`/`phone`/`is_verified` columns + `update_profile` whitelist update

### Google Cloud Console
1. ✅ OAuth 2.0 Web Application client created
2. ✅ Authorized redirect URI: `https://licxsuvpqoyjlremthwr.supabase.co/auth/v1/callback`

---

## Key Files

| File | Role |
|------|------|
| `AuthViewModel.kt` | All auth UI logic, linking, session check, grade resolution |
| `AuthRepositoryImpl.kt` | Supabase SDK calls, session observation, user creation |
| `AuthRepository.kt` | Interface for auth operations |
| `LoginJourneyScreen.kt` | Onboarding + auth UI (SPLASH → USER_TYPE → STEP1-3 → DONE) |
| `SupabaseClientProvider.kt` | Supabase client config (Auth scheme/host, Postgrest, Realtime) |
| `SessionProvider.kt` | Provides current user ID from Supabase session |
| `SessionPrefs.kt` | Persisted session flags (isLoggedIn, lastAuthProvider, onboarding) |
| `ErrorMapper.kt` | Maps raw exceptions to user-friendly messages |
| `MainActivity.kt` | Android deep link handler for OAuth callback |
| `AppDelegate.swift` | iOS deep link handler + crash diagnostics |
| `Info.plist` | iOS URL scheme (`mindquest://`) + `CADisableMinimumFrameDurationOnPhone` |
| `AndroidManifest.xml` | Android intent filters for deep links |
