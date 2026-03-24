# Supabase API Logging Guide

## Overview

All Supabase API calls now have comprehensive logging with:
- **🔵 Request logs** - Function name, parameters, and call details
- **✅ Success logs** - Response data summary
- **❌ Error logs** - Error messages and exceptions

All logs use the tag `MQ_API` for easy filtering in Android Studio Logcat.

---

## Logging Format

### Request Log (Before API Call)
```
🔵 RPC: function_name | Params: param1=value1, param2=value2
🔵 TABLE: table_name | Filter: filter_key=value
```

### Success Log (After API Call)
```
✅ RPC: function_name SUCCESS | Response: summary_info
✅ TABLE: table_name SUCCESS | Response: count/details
```

### Error Log (On Exception)
```
❌ RPC: function_name FAILED | Error: exception_message
❌ TABLE: table_name FAILED | Error: exception_message
```

---

## Sensitive Data Masking

The logger automatically masks sensitive data in logs:

| Field | Masked As |
|-------|-----------|
| email | `***@***.***` |
| phone | `***XX` (last 2 digits) |
| password | `***` |
| token | `***` |
| secret | `***` |
| hash | `***` |

**Example**: `find_user_by_email` logs as `Params: email=***@***.***`

---

## Complete API Logging Reference

### Dashboard Functions

#### getUserDashboard(userId)
```
🔵 RPC: get_user_dashboard | Params: userId=abc123
✅ RPC: get_user_dashboard SUCCESS | Response: user(id=abc123), modules=5
```

#### getDailyChallenges(userId)
```
🔵 RPC: get_daily_challenges | Params: userId=abc123
✅ RPC: get_daily_challenges SUCCESS | Response: 3 challenges
```

---

### Modules & Chapters

#### getModuleFull(moduleId, userId)
```
🔵 RPC: get_module_full | Params: moduleId=math101, userId=abc123
✅ RPC: get_module_full SUCCESS | Response: 10 chapters
```

#### getChapterQuizzes(chapterId, userId)
```
🔵 RPC: get_chapter_quizzes | Params: chapterId=ch1, userId=abc123
✅ RPC: get_chapter_quizzes SUCCESS | Response: 5 quizzes
```

---

### Quiz Functions

#### getQuizWithQuestions(quizId, userId)
```
🔵 RPC: get_quiz_with_questions | Params: quizId=q1, userId=abc123
✅ RPC: get_quiz_with_questions SUCCESS | Response: 20 questions
```

#### submitQuizAttempt(payload)
```
🔵 RPC: submit_quiz_attempt | Params: payload keys=p_payload
✅ RPC: submit_quiz_attempt SUCCESS | Response: score=85, xpEarned=150
```

---

### Tournament Functions

#### startTournament(userId, tournamentId)
```
🔵 RPC: start_tournament | Params: userId=abc123, tournamentId=t1
✅ RPC: start_tournament SUCCESS | Response: entryId=entry123
```

#### pauseTournament(entryId)
```
🔵 RPC: pause_tournament | Params: entryId=entry123
✅ RPC: pause_tournament SUCCESS | Response: entryId=entry123
```

#### resumeTournament(entryId)
```
🔵 RPC: resume_tournament | Params: entryId=entry123
✅ RPC: resume_tournament SUCCESS | Response: entryId=entry123
```

#### submitTournament(entryId, answers, timeTaken)
```
🔵 RPC: submit_tournament | Params: entryId=entry123, timeTaken=1200, answers=50
✅ RPC: submit_tournament SUCCESS | Response: score=80, status=completed
```

#### submitTournamentAnswer(entryId, answer)
```
🔵 RPC: submit_tournament_answer | Params: entryId=entry123
✅ RPC: submit_tournament_answer SUCCESS
```

#### getTournamentEntry(userId, tournamentId)
```
🔵 TABLE: tournament_entries | Filter: userId=abc123, tournamentId=t1
✅ TABLE: tournament_entries SUCCESS | Response: entryId=entry123
```

#### getActiveTournament(userId, gradeId)
```
🔵 RPC: get_active_tournament | Params: userId=abc123, gradeId=g5
✅ RPC: get_active_tournament SUCCESS | Response: tournamentId=t1
✅ RPC: get_active_tournament SUCCESS | Response: NULL (no active)
```

#### getTournamentLeaderboard(tournamentId, userId, limit, offset)
```
🔵 RPC: get_tournament_leaderboard | Params: tournamentId=t1, userId=abc123, limit=50, offset=0
✅ RPC: get_tournament_leaderboard SUCCESS | Response: 50 entries
```

---

### Leaderboard & Stats

#### getLeaderboard(userId, filter, filterId, limit, offset)
```
🔵 RPC: get_leaderboard | Params: userId=abc123, filter=global, filterId=null, limit=50, offset=0
✅ RPC: get_leaderboard SUCCESS | Response: 50 users, userRank=42
```

#### getUserStats(userId, period)
```
🔵 RPC: get_user_stats | Params: userId=abc123, period=week
✅ RPC: get_user_stats SUCCESS | Response: totalXp=5000, accuracy=82%
```

---

### Profile Functions

#### getProfile(userId)
```
🔵 RPC: get_profile | Params: userId=abc123
✅ RPC: get_profile SUCCESS | Response: user=John Doe, email=***, phone=NULL, authProvider=google
```

#### updateProfile(userId, fields)
```
🔵 RPC: update_profile | Params: userId=abc123, fields=display_name,avatar_id
✅ RPC: update_profile SUCCESS | Updated fields: display_name,avatar_id
```

---

### User Management

#### upsertUser(data)
```
🔵 TABLE UPSERT: users | Columns: id,display_name,avatar_id,grade_id,auth_provider,email,phone
✅ TABLE UPSERT: users SUCCESS | userId=abc123
```

---

### Account Linking & Duplicate Detection

#### findUserByEmail(email)
```
🔵 RPC: find_user_by_email | Params: email=***@***.***
✅ RPC: find_user_by_email SUCCESS | Response: FOUND (id=user456)
✅ RPC: find_user_by_email SUCCESS | Response: NOT FOUND
```

#### findUserByPhone(phone)
```
🔵 RPC: find_user_by_phone | Params: phone=***
✅ RPC: find_user_by_phone SUCCESS | Response: FOUND (id=user456)
✅ RPC: find_user_by_phone SUCCESS | Response: NOT FOUND
```

#### mergeUsers(fromId, toId)
```
🔵 RPC: merge_users | Params: fromId=anon123 → toId=google456
✅ RPC: merge_users SUCCESS | Merged anon123 → google456
```

---

### Reference Data

#### getGrades()
```
🔵 TABLE: grades
✅ TABLE: grades SUCCESS | Response: 12 grades
```

#### getCountries()
```
🔵 TABLE: countries | Filter: is_active=true
✅ TABLE: countries SUCCESS | Response: 195 countries
```

#### getCities(countryId)
```
🔵 TABLE: cities | Filter: countryId=US
✅ TABLE: cities SUCCESS | Response: 50 cities for US
```

---

## How to Use in Logcat

### Android Studio Logcat Filtering

1. **Show all API calls**: Filter by `MQ_API`
2. **Show only errors**: Filter by `MQ_API` and level `Error`
3. **Show specific function**: Filter by `MQ_API` and text `get_profile`
4. **Show request/response pairs**: Filter by `MQ_API` and look for 🔵 and ✅ pairs

### Example Logcat Searches

```
# Show all API activity
MQ_API

# Show only errors
MQ_API -v:E

# Show profile function calls
MQ_API.*get_profile

# Show authentication flows
MQ_API.*(auth|find_user|merge)

# Show only successes
✅.*MQ_API

# Show only failures
❌.*MQ_API
```

---

## Performance Monitoring

Use the logs to monitor API performance:

1. **Time between 🔵 and ✅ = API call duration**
2. **Look for repeated ❌ logs = potential retry loops**
3. **Missing ✅ logs = timeout or connection issues**
4. **Error messages = specific failure reasons**

**Example Analysis**:
```
13:45:23.123 🔵 RPC: get_profile | userId=abc123
13:45:24.456 ✅ RPC: get_profile SUCCESS
// Duration: ~1.3 seconds (normal)

13:45:25.100 🔵 RPC: submit_quiz_attempt | ...
13:45:30.200 ❌ RPC: submit_quiz_attempt FAILED | Error: Timeout
// Duration: ~5 seconds (timeout, should retry)
```

---

## Error Debugging

When troubleshooting API issues, the logs show:

1. **Which API was called** - RPC function or table name
2. **What parameters were sent** - With sensitive data masked
3. **What the response was** - With relevant data summary
4. **What error occurred** - Exception type and message

**Example**:
```
❌ RPC: get_user_dashboard FAILED | Error: 401 Unauthorized
→ Indicates authentication token expired or invalid

❌ TABLE: users FAILED | Error: Network timeout
→ Indicates connection issue with Supabase

❌ RPC: merge_users FAILED | Error: Foreign key constraint violated
→ Indicates data integrity issue
```

---

## Code Changes Summary

**File Modified**: `ApiService.kt`

**Total Changes**:
- Added `AppLogger` import
- Added `maskSensitiveData()` helper function
- Updated 25 API functions with logging
- ~250 new lines of logging code
- **No functional changes to API behavior**

**Log Tag**: Always use `MQ_API` for all API logs

---

## Best Practices

1. ✅ **Filter by `MQ_API` tag** - All API logs use this tag
2. ✅ **Check response summaries** - Get quick overview of what's happening
3. ✅ **Pair 🔵 with ✅ or ❌** - Track complete request/response cycles
4. ✅ **Look for masking** - Sensitive data is automatically hidden
5. ✅ **Monitor error patterns** - Repeated errors indicate issues

---

## Example: Complete User Login Flow Logs

```
// 1. User taps "Sign in with Google"
13:45:00.100 🔵 RPC: get_active_tournament | userId=google123, gradeId=g5
13:45:00.500 ✅ RPC: get_active_tournament SUCCESS | tournamentId=t1

// 2. Load profile after signin
13:45:01.000 🔵 RPC: get_profile | userId=google123
13:45:01.200 ✅ RPC: get_profile SUCCESS | email=***, authProvider=google

// 3. Load dashboard
13:45:01.500 🔵 RPC: get_user_dashboard | userId=google123
13:45:01.800 ✅ RPC: get_user_dashboard SUCCESS | modules=5

// 4. Load daily challenges
13:45:02.000 🔵 RPC: get_daily_challenges | userId=google123
13:45:02.300 ✅ RPC: get_daily_challenges SUCCESS | 3 challenges
```

---

## Summary

All Supabase API calls now have complete logging with:
- ✅ **Request logging** - See what's being called
- ✅ **Response logging** - See what came back
- ✅ **Error logging** - See what went wrong
- ✅ **Sensitive data masking** - Automatic protection of PII
- ✅ **Single log tag** - Easy filtering with `MQ_API`

This makes debugging API issues much easier and provides full visibility into all backend calls.
