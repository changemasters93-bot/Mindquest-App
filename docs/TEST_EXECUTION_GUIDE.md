# Test Execution Guide - 168 Signup & Signin Test Cases

**Test Session Date:** _______________
**Tester Name:** _______________
**Device/Emulator:** _______________
**App Version:** _______________

---

## Quick Start

1. **Before Testing:**
   - [ ] Clear app data: `adb shell pm clear com.android.mindquest` (Android) or Settings → General → Storage → Offload app (iOS)
   - [ ] Close and restart app
   - [ ] Enable logcat/console logging
   - [ ] Note any known issues or blockers

2. **During Testing:**
   - Document each test result (PASS/FAIL)
   - Capture logs for failures
   - Screenshot errors
   - Note exact timing for timeout-related issues

3. **After Testing:**
   - Calculate pass rate: (Passed / Total) × 100%
   - Identify failure patterns
   - Create bug reports for failures

---

## Phase 1: HAPPY PATHS (12 tests) - Priority: HIGH

Goal: Verify all main flows work without errors

### G1: New Google User with Complete Onboarding
```
Steps:
1. Tap "Continue with Google"
2. Select Google account
3. Complete onboarding (name, avatar, grade, country, city)
4. Tap final continue

Expected: User created in home screen, all profile data saved
Status: [ ] PASS [ ] FAIL
Notes: _____________________
Logs Captured: [ ] Yes [ ] No
```

### G2: New Google User without Onboarding
```
Steps:
1. Tap "Continue with Google"
2. Select Google account
3. Tap "Skip" or exit onboarding

Expected: User created with default avatar (1), no grade/country
Status: [ ] PASS [ ] FAIL
Notes: _____________________
```

### G3: Returning Google User (Same Email)
```
Steps:
1. Use Gmail that previously signed up
2. Tap "Continue with Google"
3. Select same Google account

Expected: Skip onboarding, go straight to home
Status: [ ] PASS [ ] FAIL
Notes: _____________________
```

### A1: Anonymous Signup with Onboarding
```
Steps:
1. Tap "Try Anonymously"
2. Complete onboarding profile

Expected: User created with isAnonymous=true, profile saved
Status: [ ] PASS [ ] FAIL
Notes: _____________________
```

### A2: Anonymous Signup without Onboarding
```
Steps:
1. Tap "Try Anonymously"
2. Skip onboarding (tap skip or back)

Expected: User created with defaults (Guest Player, avatar 1)
Status: [ ] PASS [ ] FAIL
Notes: _____________________
```

### P1: Phone OTP Valid Flow
```
Steps:
1. Tap "Phone"
2. Enter valid phone number
3. Tap "Send OTP"
4. Enter received OTP code
5. Complete onboarding

Expected: User with phone created, authProvider=phone
Status: [ ] PASS [ ] FAIL
Timing: _____ seconds
Notes: _____________________
```

### GL1: Link Google to Anonymous Account
```
Steps:
1. Sign in anonymously
2. Go to Profile/Settings
3. Tap "Link Account" → "Google"
4. Select Google account

Expected: authProvider changed to "google", email added
Status: [ ] PASS [ ] FAIL
Notes: _____________________
Email Saved: [ ] Yes [ ] No
```

### GL2: Link Google to Phone Account
```
Steps:
1. Sign in with phone
2. Tap "Link Account" → "Google"
3. Select Google account

Expected: authProvider changed to "google_and_phone"
Status: [ ] PASS [ ] FAIL
Notes: _____________________
```

### PL1: Link Phone to Anonymous Account
```
Steps:
1. Sign in anonymously
2. Tap "Link Account" → "Phone"
3. Enter phone, receive OTP
4. Enter OTP code

Expected: authProvider changed to "phone"
Status: [ ] PASS [ ] FAIL
Notes: _____________________
```

### D1: Email Duplicate Detection
```
Steps:
1. Sign in with Gmail that already exists (different user)

Expected: Error shown - "Email already used. Sign in instead."
Status: [ ] PASS [ ] FAIL
Error Message: _____________________
```

### S1: Session Check - Logged In User
```
Steps:
1. Login with any method
2. Close app
3. Restart app

Expected: Auto-login, no onboarding, home screen shows
Status: [ ] PASS [ ] FAIL
Time to login: _____ seconds
Notes: _____________________
```

### S2: Session Check - Not Logged In
```
Steps:
1. Fresh app launch (clear data first)

Expected: Auth screen shown (Google/Anonymous/Phone options)
Status: [ ] PASS [ ] FAIL
Notes: _____________________
```

---

## Phase 2: ERROR SCENARIOS (48 tests) - Priority: HIGH

### Network Errors (4 tests)

**E1: Network Error - No Internet**
```
Setup: Disable WiFi and mobile data
Steps:
1. Try to sign in with Google/Phone/Anonymous
Expected: "Unable to connect. Please check your internet and try again."
Status: [ ] PASS [ ] FAIL
```

**E2: Timeout Error**
```
Setup: Very slow network or introduce 10+ second delay
Steps:
1. Attempt auth flow
Expected: "Request timed out. Please try again."
Status: [ ] PASS [ ] FAIL
Timeout After: _____ seconds
```

**E3: SSL/Certificate Error**
```
Setup: Network with certificate issues (if available)
Steps:
1. Attempt auth
Expected: "Secure connection failed. Please try again."
Status: [ ] PASS [ ] FAIL
```

### OTP Errors (5 tests)

**P4: Invalid OTP**
```
Steps:
1. Send OTP to phone
2. Enter WRONG code
Expected: "The code you entered is invalid or expired. Please try again."
Status: [ ] PASS [ ] FAIL
```

**P5: Expired OTP**
```
Steps:
1. Send OTP
2. Wait 15+ minutes
3. Enter OTP
Expected: "The code you entered is invalid or expired. Please try again."
Status: [ ] PASS [ ] FAIL
```

**P8: Rate Limiting**
```
Steps:
1. Send OTP
2. Immediately tap Resend 5+ times
Expected: "A verification code was already sent. Please wait before requesting again."
Status: [ ] PASS [ ] FAIL
```

**P6: Resend After Timeout**
```
Steps:
1. Send OTP
2. Wait 30 seconds
3. Tap Resend
Expected: New OTP sent, timer resets to 30s
Status: [ ] PASS [ ] FAIL
```

### Database Constraint Errors (4 tests)

**E7: Email Duplicate Constraint**
```
Setup: Email already exists in database
Steps:
1. Signup with duplicate email
Expected: "This email is already used by another account. Please sign in instead."
Status: [ ] PASS [ ] FAIL
```

**E8: Phone Duplicate Constraint**
```
Setup: Phone already exists in database
Steps:
1. Link or signup with duplicate phone
Expected: "This phone number is already linked to another account."
Status: [ ] PASS [ ] FAIL
```

**E9: RLS Permission Error**
```
Steps:
1. Try account operations without proper permissions
Expected: "You don't have permission to perform this action."
Status: [ ] PASS [ ] FAIL
```

### Session/Auth Errors (3 tests)

**E6: Session Expired**
```
Setup: Wait for session JWT to expire
Steps:
1. App tries to make API call with expired JWT
Expected: "Your session has expired. Please sign in again."
Status: [ ] PASS [ ] FAIL
```

**G8: Session becomes NULL**
```
Setup: Supabase auth issue (simulate in mock if possible)
Steps:
1. Auth succeeds but session is NULL
Expected: "Sign-in failed. Please try again."
Status: [ ] PASS [ ] FAIL
```

### Account Linking Errors (3 tests)

**GL3: Already Google**
```
Setup: User already linked to Google
Steps:
1. Try to link Google again
Expected: "This provider is already linked to your account."
Status: [ ] PASS [ ] FAIL
```

**GL6: updateProfile RPC Fails**
```
Setup: Database error during linking
Steps:
1. Link account but DB update fails
Expected: "Something went wrong. Please try again."
Status: [ ] PASS [ ] FAIL
```

**PL4: getCurrentUser Fails**
```
Setup: API error when fetching current user
Steps:
1. Start phone linking with API error
Expected: Error shown to user
Status: [ ] PASS [ ] FAIL
Error: _____________________
```

### Duplicate Detection Errors (3 tests)

**D2: Recover - Sign In Instead**
```
Steps:
1. See duplicate email error
2. Tap "Sign In Instead"
Expected: Navigate to auth main screen, can try signing in
Status: [ ] PASS [ ] FAIL
```

**D3: Recover - Try Different Account**
```
Steps:
1. See duplicate email error
2. Tap "Try Different Account"
Expected: Error clears, can tap Google again
Status: [ ] PASS [ ] FAIL
```

**D5: findExistingUser API Error**
```
Setup: API fails during duplicate check
Steps:
1. Signup - findExistingUser fails
Expected: Continue with signup (graceful fallback)
Status: [ ] PASS [ ] FAIL
```

### Reference Data Errors (2 tests)

**R3: Grades Timeout**
```
Setup: Slow grades API (>10 seconds)
Steps:
1. Start onboarding before grades load
Expected: "Grades not loaded yet. Please try again."
Status: [ ] PASS [ ] FAIL
```

**R8: Cities Fetch Fails**
```
Setup: Cities API returns error
Steps:
1. Select country during onboarding
Expected: Gracefully handle, show empty cities
Status: [ ] PASS [ ] FAIL
```

### Generic Errors (2 tests)

**E10: Generic Error**
```
Setup: Unmapped exception (varies)
Steps:
1. Trigger unmapped error
Expected: "Something went wrong. Please try again."
Status: [ ] PASS [ ] FAIL
```

**E11: Identity Already Linked**
```
Steps:
1. Try to link provider that's already linked
Expected: "This provider is already linked to your account."
Status: [ ] PASS [ ] FAIL
```

---

## Phase 3: INTEGRATION FLOWS (25 tests) - Priority: MEDIUM

### Multi-Step Google Flows (6 tests)

**G9: Email Extraction Fallback**
```
Steps:
1. Google signin but email missing from session
Expected: Use default or handle gracefully
Status: [ ] PASS [ ] FAIL
```

**G10: Concurrent Google Attempts**
```
Steps:
1. Tap Google multiple times rapidly
Expected: Second attempt blocked (debounce)
Status: [ ] PASS [ ] FAIL
```

**G11: Incomplete Grades Data**
```
Steps:
1. Grades not fully loaded
2. Try to complete onboarding
Expected: Grade selection works or shows error
Status: [ ] PASS [ ] FAIL
```

**GL5: Email Extraction Fails After Linking**
```
Steps:
1. Link Google account
2. Email not in session initially
Expected: Retry logic (3 attempts) handles it
Status: [ ] PASS [ ] FAIL
Retries Logged: [ ] 1 [ ] 2 [ ] 3
```

**GL7: Session NULL During Linking**
```
Steps:
1. Link Google but session becomes NULL
Expected: Skip DB update, continue gracefully
Status: [ ] PASS [ ] FAIL
```

**GL8: New Email Extracted**
```
Steps:
1. Link with different Google account
2. Email should be different from first
Expected: New email extracted and saved
Status: [ ] PASS [ ] FAIL
New Email: _____________________
```

### Multi-Step Phone Flows (6 tests)

**PL5: OTP Send Fails Network**
```
Steps:
1. Start phone linking
2. Network fails during OTP send
Expected: Error shown
Status: [ ] PASS [ ] FAIL
```

**PL6: Invalid OTP During Linking**
```
Steps:
1. Start phone linking
2. Enter invalid OTP
Expected: "Invalid or expired code"
Status: [ ] PASS [ ] FAIL
```

**PL8: OTP Verify Timeout During Linking**
```
Steps:
1. Start phone linking
2. Timeout during OTP verify
Expected: Timeout error
Status: [ ] PASS [ ] FAIL
```

**PL9: upsertUserRow Fails During Linking**
```
Steps:
1. OTP verified but DB upsert fails
Expected: Error shown, linking failed
Status: [ ] PASS [ ] FAIL
```

**PL12: Cancelled After OTP Sent**
```
Steps:
1. Send OTP for phone linking
2. Tap back/cancel
Expected: Linking cancelled, OTP ignored
Status: [ ] PASS [ ] FAIL
```

**PL14: Session Replaced During Linking**
```
Steps:
1. Start phone linking (session will be replaced)
2. Complete OTP
Expected: pendingPhoneLinkUser restores original user
Status: [ ] PASS [ ] FAIL
```

### Profile Data Flows (4 tests)

**PR1: Upsert New User**
```
Steps:
1. First-time signup
Expected: User row created in DB
Status: [ ] PASS [ ] FAIL
```

**PR2: Update Existing User**
```
Steps:
1. Returning user changes profile
Expected: User row updated (not duplicated)
Status: [ ] PASS [ ] FAIL
```

**PR3: Email Persisted**
```
Steps:
1. Google signup
Expected: Email visible in profile
Status: [ ] PASS [ ] FAIL
Email in DB: _____________________
```

**PR10: Empty Display Name**
```
Steps:
1. Signup without entering display name
Expected: Use default "Player"
Status: [ ] PASS [ ] FAIL
```

### Session Flows (3 tests)

**S4: Observer Detects Login**
```
Steps:
1. Complete auth flow
Expected: observeSessionUserId() emits new userId
Status: [ ] PASS [ ] FAIL
```

**S5: Observer Detects Logout**
```
Steps:
1. Sign out
Expected: Session observer emits null
Status: [ ] PASS [ ] FAIL
```

**S7: Session Restored From Prefs**
```
Steps:
1. Logout
2. Restart app
Expected: Not auto-logged in (logout persisted)
Status: [ ] PASS [ ] FAIL
```

---

## Phase 4: EDGE CASES & RACE CONDITIONS (20 tests) - Priority: MEDIUM

**X1: App Backgrounded During OAuth**
```
Steps:
1. Start Google linking
2. Switch to another app (background)
3. Return after 30+ seconds
Expected: Session observer detects completion
Status: [ ] PASS [ ] FAIL
```

**X2: Rapid Auth Attempts**
```
Steps:
1. Tap Google rapidly 5 times
Expected: Debounce prevents concurrent ops
Status: [ ] PASS [ ] FAIL
```

**X3: Late OTP Verification**
```
Steps:
1. OTP sent to +1-111-1111
2. Change phone field to +2-222-2222
3. Verify old OTP
Expected: Code mismatch fails
Status: [ ] PASS [ ] FAIL
```

**X4: Stale User Object**
```
Steps:
1. Start auth flow
2. User data changes in DB
3. Complete flow
Expected: Use current user (fresh), not stale
Status: [ ] PASS [ ] FAIL
```

**X5: Profile Fetch Race**
```
Steps:
1. upsertUser and getProfile called simultaneously
Expected: May return incomplete data (race ok if handled)
Status: [ ] PASS [ ] FAIL
```

**X6: Multiple OAuth Callbacks**
```
Setup: Open multiple browser OAuth flows
Steps:
1. Complete both
Expected: Last callback wins (or both handled)
Status: [ ] PASS [ ] FAIL
```

**X7: Network Split During Linking**
```
Steps:
1. Start linking
2. Disconnect network
3. Wait 70+ seconds
Expected: Timeout error or eventual completion
Status: [ ] PASS [ ] FAIL
```

**X8: Concurrent Phone/Google**
```
Steps:
1. Start Google linking
2. Immediately start phone linking
Expected: pendingLinkProvider allows only one
Status: [ ] PASS [ ] FAIL
```

**X9: Rapid Grade/Country Loads**
```
Steps:
1. Multiple reference data API calls
Expected: Both emit independently (no race)
Status: [ ] PASS [ ] FAIL
```

**X10: User Row Deleted Mid-Linking**
```
Setup: Delete user from DB during linking
Steps:
1. Start linking, user deleted
Expected: API error, linking fails
Status: [ ] PASS [ ] FAIL
```

**X11: Session Consistency**
```
Steps:
1. Rapid session changes
Expected: Session observer stays current
Status: [ ] PASS [ ] FAIL
```

**X12: Offline to Online**
```
Steps:
1. App offline
2. Go online
3. Try auth
Expected: Reauth on next API call
Status: [ ] PASS [ ] FAIL
```

**X13: SessionPrefs Race**
```
Steps:
1. Multiple updates to SessionPrefs
Expected: Last write wins (no data loss)
Status: [ ] PASS [ ] FAIL
```

**X14: Email Changes Mid-Flow**
```
Steps:
1. Email updated during signup
Expected: Use final session state
Status: [ ] PASS [ ] FAIL
```

**X15: Profile Partially Exists**
```
Steps:
1. Some user fields in DB, others missing
2. upsertUserRow called
Expected: Complete update (all fields set)
Status: [ ] PASS [ ] FAIL
```

**X16: Concurrent Profile Updates**
```
Steps:
1. Two updates to same user simultaneously
Expected: Last update wins (DB constraints)
Status: [ ] PASS [ ] FAIL
```

**X17: Session Refresh Race**
```
Steps:
1. Session refresh during operation
Expected: Use current session state
Status: [ ] PASS [ ] FAIL
```

**X18: State Machine Violation**
```
Steps:
1. Skip intermediate state (invalid transition)
Expected: State validation prevents (or graceful)
Status: [ ] PASS [ ] FAIL
```

**X19: Deep Linking After Auth**
```
Steps:
1. Authenticate then navigate to deep link
Expected: Respect pending deep link
Status: [ ] PASS [ ] FAIL
```

**X20: Notification During Auth**
```
Steps:
1. Notification arrives during auth flow
Expected: Pause/resume flow gracefully
Status: [ ] PASS [ ] FAIL
```

---

## Phase 5: STATE & UI TRANSITIONS (15 tests) - Priority: LOW

**U1: MAIN → PHONE**
```
Steps:
1. On auth main screen
2. Tap "Phone"
Expected: authScreen → PHONE
Status: [ ] PASS [ ] FAIL
```

**U2: PHONE → OTP**
```
Steps:
1. On phone entry screen
2. Enter phone, tap "Send OTP"
Expected: authScreen → OTP, isOtpSent=true
Status: [ ] PASS [ ] FAIL
```

**U3: OTP → VERIFIED**
```
Steps:
1. On OTP entry screen
2. Enter valid OTP
Expected: authScreen → VERIFIED, navigate to home
Status: [ ] PASS [ ] FAIL
```

**U4: Navigation Back**
```
Steps:
1. On PHONE or OTP screen
2. Tap back
Expected: authScreen → MAIN, states cleared
Status: [ ] PASS [ ] FAIL
```

**U5: Linking Sheet Visible**
```
Steps:
1. Logged-in user
2. Tap "Link Account"
Expected: isLinkingSheetVisible=true, sheet shown
Status: [ ] PASS [ ] FAIL
```

**U6: Linking Sheet Dismiss**
```
Steps:
1. Linking sheet shown
2. Tap close/X
Expected: isLinkingSheetVisible=false
Status: [ ] PASS [ ] FAIL
```

**U7: Confirmation Dialog Show**
```
Steps:
1. Before linking
Expected: Confirmation dialog shown
Status: [ ] PASS [ ] FAIL
```

**U8: Confirmation Confirm**
```
Steps:
1. Confirmation dialog shown
2. Tap Confirm
Expected: Proceed with linking
Status: [ ] PASS [ ] FAIL
```

**U9: Confirmation Cancel**
```
Steps:
1. Confirmation dialog shown
2. Tap Cancel
Expected: Clear dialog & sheet, return to profile
Status: [ ] PASS [ ] FAIL
```

**U10: Auth Success Navigate**
```
Steps:
1. Complete auth successfully
Expected: authState → Success triggers navigation
Status: [ ] PASS [ ] FAIL
Time to navigate: _____ seconds
```

**R1: Grades Loaded on Init**
```
Steps:
1. App launch
Expected: _grades populated before onboarding
Status: [ ] PASS [ ] FAIL
```

**R2: Countries Loaded on Init**
```
Steps:
1. App launch
Expected: _countries populated
Status: [ ] PASS [ ] FAIL
```

**R5: Grade ID Resolution**
```
Steps:
1. Select "Grade 10" in onboarding
Expected: Returns correct UUID
Status: [ ] PASS [ ] FAIL
Grade ID: _____________________
```

**R6: Country ID Resolution**
```
Steps:
1. Select "India" in onboarding
Expected: Returns correct UUID
Status: [ ] PASS [ ] FAIL
Country ID: _____________________
```

**R7: City ID Resolution**
```
Steps:
1. Select city for country
Expected: Returns correct UUID
Status: [ ] PASS [ ] FAIL
```

---

## Phase 6: PROFILE & DATA HANDLING (10 tests) - Priority: MEDIUM

**PR5: Missing Avatar Default**
```
Steps:
1. Onboarding without selecting avatar
Expected: Use default avatar (1)
Status: [ ] PASS [ ] FAIL
```

**PR6: Missing Grade Handling**
```
Steps:
1. Onboarding without grade
Expected: Empty gradeId or error on completion
Status: [ ] PASS [ ] FAIL
```

**PR7: Missing Country OK**
```
Steps:
1. Onboarding without country
Expected: countryId=null (allowed)
Status: [ ] PASS [ ] FAIL
```

**PR8: Missing City OK**
```
Steps:
1. Onboarding without city
Expected: cityId=null (allowed)
Status: [ ] PASS [ ] FAIL
```

**PR9: Long Display Name**
```
Steps:
1. Enter 200+ character display name
Expected: Store as-is or truncate (check implementation)
Status: [ ] PASS [ ] FAIL
Characters stored: _____
```

**PR4: Phone Persisted**
```
Steps:
1. Phone signup
Expected: Phone visible in profile
Status: [ ] PASS [ ] FAIL
Phone in DB: _____________________
```

**D6: Merge - Confirm**
```
Steps:
1. See merge suggestion
2. Tap Confirm
Expected: mergeUsers() RPC called
Status: [ ] PASS [ ] FAIL
```

**D7: Merge - Skip**
```
Steps:
1. See merge suggestion
2. Tap Skip
Expected: Continue with new user (no merge)
Status: [ ] PASS [ ] FAIL
```

**D8: Merge Data Integrity**
```
Steps:
1. Merge users with different quiz stats
Expected: All history & XP aggregated
Status: [ ] PASS [ ] FAIL
```

**R9: Missing Grade Validation**
```
Steps:
1. Invalid grade label selected
Expected: resolveGradeId() returns "", validation fails
Status: [ ] PASS [ ] FAIL
```

---

## Phase 7: LOGGING & OBSERVABILITY (7 tests) - Priority: LOW

**L1: Google Auth Logs**
```
Steps:
1. Complete Google signup
2. Check logs with MQ_AUTH tags
Expected: All steps logged
Status: [ ] PASS [ ] FAIL
Logs: [ ] Retrieved [ ] Not found
```

**L2: Phone OTP Logs**
```
Steps:
1. Phone signup
2. Check logs
Expected: OTP attempts logged
Status: [ ] PASS [ ] FAIL
```

**L3: Linking Logs**
```
Steps:
1. Account linking
2. Check logs
Expected: Linking steps logged
Status: [ ] PASS [ ] FAIL
```

**L4: Data Masking**
```
Steps:
1. Complete flow
2. Check logs for emails/phones
Expected: Masked (***@***.***), tokens masked
Status: [ ] PASS [ ] FAIL
```

**L5: DB Operation Logs**
```
Steps:
1. Any DB operation
2. Check MQ_DB/MQ_API logs
Expected: RPC function, params, result logged
Status: [ ] PASS [ ] FAIL
```

**L6: Error Logs**
```
Steps:
1. Trigger any error
2. Check logs
Expected: Error tag + user message logged
Status: [ ] PASS [ ] FAIL
```

**L7: Session Observer Logs**
```
Steps:
1. Session changes
2. Check logs
Expected: Login/logout events logged
Status: [ ] PASS [ ] FAIL
```

---

## Phase 8: MOCK DATA MODE (8 tests) - Priority: LOW

**M1: Mock Google Signin**
```
Setup: USE_MOCK_DATA=true
Steps:
1. Tap Google
Expected: Returns mock user immediately
Status: [ ] PASS [ ] FAIL
```

**M2: Mock Phone OTP**
```
Setup: USE_MOCK_DATA=true
Steps:
1. Send OTP → Verify
Expected: Returns mock user without API calls
Status: [ ] PASS [ ] FAIL
```

**M3: Mock Anonymous**
```
Setup: USE_MOCK_DATA=true
Steps:
1. Tap Anonymous
Expected: Returns mock anon user
Status: [ ] PASS [ ] FAIL
```

**M4: Mock Profile Fetch**
```
Setup: USE_MOCK_DATA=true
Steps:
1. Get current user
Expected: Returns mock user
Status: [ ] PASS [ ] FAIL
```

**M5: Mock Session Check**
```
Setup: USE_MOCK_DATA=true
Steps:
1. App launch
Expected: Uses sessionPrefs only (no API)
Status: [ ] PASS [ ] FAIL
```

**M6: Mock upsertUserRow**
```
Setup: USE_MOCK_DATA=true
Steps:
1. Any signup
Expected: Returns success (no DB operation)
Status: [ ] PASS [ ] FAIL
```

**M7: Mock Linking**
```
Setup: USE_MOCK_DATA=true
Steps:
1. Link account
Expected: Returns success immediately
Status: [ ] PASS [ ] FAIL
```

**M8: Mock Reference Data**
```
Setup: USE_MOCK_DATA=true
Steps:
1. Load grades/countries
Expected: Returns empty lists
Status: [ ] PASS [ ] FAIL
```

---

## Test Summary Report

**Testing Period:** _____ to _____

### Overall Results
- **Total Tests:** 168
- **Passed:** _____ (____% )
- **Failed:** _____ (____% )
- **Blocked/Skipped:** _____ (____% )

### By Phase
| Phase | Tests | Passed | Failed | % Pass |
|-------|-------|--------|--------|--------|
| 1: Happy Paths | 12 | __ | __ | __% |
| 2: Error Scenarios | 48 | __ | __ | __% |
| 3: Integration Flows | 25 | __ | __ | __% |
| 4: Edge Cases | 20 | __ | __ | __% |
| 5: State & UI | 15 | __ | __ | __% |
| 6: Profile Data | 10 | __ | __ | __% |
| 7: Logging | 7 | __ | __ | __% |
| 8: Mock Mode | 8 | __ | __ | __% |
| **TOTAL** | **168** | **__** | **__** | **__%** |

### Critical Failures (HIGH Priority)
1. _____________________
2. _____________________
3. _____________________

### Patterns Identified
- Timeout issues: [ ] Yes [ ] No (How many: ____)
- Email/Phone issues: [ ] Yes [ ] No (How many: ____)
- RLS violations: [ ] Yes [ ] No (How many: ____)
- Session issues: [ ] Yes [ ] No (How many: ____)
- Concurrency issues: [ ] Yes [ ] No (How many: ____)

### Recommendations for Fixes
1. _____________________
2. _____________________
3. _____________________

### Sign-Off
**Tested By:** _____________________
**Date:** _____________________
**Approved By:** _____________________
**Next Steps:** _____________________

---

## How to Use This Guide

1. **Print or digitize** this document
2. **Work through phases** in order (start with Phase 1: Happy Paths)
3. **Mark PASS/FAIL** for each test as you go
4. **Capture logs** for any failures (save with timestamps)
5. **Screenshot errors** for bug reports
6. **Complete summary report** at the end
7. **Create bug reports** for all failures with this guide as reference

## Tips for Efficient Testing

- **Batch similar tests:** Do all Google tests together, then all Phone tests
- **Reuse setups:** Don't clear data between tests unless needed
- **Use multiple devices:** Test on phone AND emulator in parallel
- **Automate where possible:** Use Firebase Test Lab for some flows
- **Log everything:** Saves time debugging later
- **Document deviations:** If you deviate from test steps, note why

---

**Good luck with testing! 🚀**
