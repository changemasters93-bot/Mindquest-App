# Pending Manual Tasks — Requires Your Intervention

Items below cannot be automated and need manual setup before release.

---

## Firebase Setup

- [x] **Create Firebase project** at https://console.firebase.google.com
  - Enable Analytics and Crashlytics
- [x] **Download `google-services.json`** and place it at `composeApp/google-services.json`
- [x] **Download `GoogleService-Info.plist`** for iOS and place at `MQiosApp/iosApp/GoogleService-Info.plist`
- [ ] Add Firebase initialization in `MQiosApp/iosApp/AppDelegate.swift`:
  ```swift
  import FirebaseCore
  // In didFinishLaunchingWithOptions:
  FirebaseApp.configure()
  ```
- [ ] Add `firebase-ios-sdk` via Swift Package Manager in Xcode (FirebaseAnalytics + FirebaseCrashlytics)

---

## Authentication Providers (Supabase Dashboard)

### Google Sign-In ✅ DONE
- [x] **Google Sign-In** — Configured in Supabase Dashboard → Authentication → Providers → Google
  - OAuth 2.0 credentials created in Google Cloud Console
  - Client ID: `774111428139-dv2nhk8c4h5dpe7n0q7qdg2c8fb4edid.apps.googleusercontent.com`
  - Authorized redirect URI set to `https://licxsuvpqoyjlremthwr.supabase.co/auth/v1/callback`
  - Skip nonce checks: enabled (required for iOS)
- [x] **Redirect URL** added: `mindquest://callback` in Supabase → Authentication → URL Configuration

### Phone/OTP Auth
- [ ] **Phone Auth** — Enable in Supabase Dashboard → Authentication → Providers → Phone
  - Configure Twilio or Vonage SMS provider
  - Set OTP expiry (recommended: 600s)
  - Set rate limits (recommended: 3 attempts per 15 min)
  - Test with your phone number

### Account Linking
- [ ] **Enable "Allow manual linking"** in Supabase Dashboard → Authentication → Sign In / Providers
  - This enables automatic identity linking when matching verified email is found
  - Prevents duplicate accounts when same user signs in with Google + Phone

---

## SQL Changes to Run in Supabase SQL Editor

Run these in **Supabase Dashboard → SQL Editor**:

```sql
-- 1. Update update_profile RPC to allow auth_provider changes
CREATE OR REPLACE FUNCTION public.update_profile(p_user_id UUID, p_fields JSON)
RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
    v_key   TEXT;
    v_value TEXT;
    v_sql   TEXT := 'UPDATE public.users SET updated_at = now()';
    v_allowed TEXT[] := ARRAY['display_name','avatar_id','grade_id','school_name','country_id','city_id','auth_provider'];
BEGIN
    FOR v_key, v_value IN SELECT * FROM json_each_text(p_fields) LOOP
        IF v_key = ANY(v_allowed) THEN
            v_sql := v_sql || format(', %I = %L', v_key, v_value);
        END IF;
    END LOOP;
    v_sql := v_sql || format(' WHERE id = %L', p_user_id);
    EXECUTE v_sql;
END;
$$;

-- 2. Add email/phone columns for duplicate detection & account recovery
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS email TEXT;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS phone TEXT;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS is_email_verified BOOLEAN DEFAULT false;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS is_phone_verified BOOLEAN DEFAULT false;
```

---

## Android Release Signing ✅ DONE

- [x] **Release keystore** configured: `mindquest-release.keystore` at project root
- [x] **`gradle.properties`** updated:
  ```properties
  RELEASE_STORE_FILE=mindquest-release.keystore
  RELEASE_KEY_ALIAS=mindquest
  RELEASE_STORE_PASSWORD=mindquest
  RELEASE_KEY_PASSWORD=mindquest
  ```
- [ ] **Never commit the keystore or passwords to git**

---

## iOS Release Signing

- [ ] Set your **Apple Developer Team ID** in Xcode → project → Signing & Capabilities
- [ ] Create an **App ID** in Apple Developer Portal (bundle ID: `com.android.mindquest`)
- [ ] Generate **provisioning profiles** (Development + Distribution)
- [ ] Configure push notification entitlement if needed

---

## App Store Assets

- [ ] **App icon** — Replace placeholder with actual 1024x1024 icon
  - Android: replace `composeApp/src/androidMain/res/mipmap-*` icons
  - iOS: replace `MQiosApp/iosApp/Assets.xcassets/AppIcon.appiconset/` images
- [ ] **Screenshots** for Play Store (phone, 7" tablet, 10" tablet)
- [ ] **Screenshots** for App Store (6.7", 6.5", 5.5")
- [ ] **Feature graphic** (1024x500) for Play Store
- [ ] **App description** and **release notes** for both stores
- [ ] **Privacy policy URL** (required by both stores)

---

## Play Store / App Store Setup

- [ ] Create **Google Play Console** listing
- [ ] Create **App Store Connect** listing
- [ ] Set up **content rating questionnaire** (Play Store)
- [ ] Set up **age rating** (App Store — likely 4+)
- [ ] Configure **in-app purchases** if planned

---

## Supabase Production Environment

- [ ] Review **Row Level Security (RLS)** policies for all tables
- [ ] Set up **database backups** schedule
- [ ] Configure **rate limiting** for API endpoints
- [ ] Set up **monitoring/alerts** for error rates
- [ ] Review **Edge Functions** if any are used
- [ ] Test all RPC functions with production-like data
- [ ] Set **JWT Expiry** (recommended: 1 hour for mobile)
- [ ] Set **Refresh Token Expiry** (recommended: 30 days)

---

## Optional Enhancements (Post-MVP)

- [ ] **Certificate pinning** for Supabase domain (prevents MITM)
- [ ] **Biometric auth** for returning users
- [ ] **Screen capture protection** (`FLAG_SECURE`) for sensitive screens
- [ ] **Root/jailbreak detection** (optional security layer)
- [ ] **Deep link testing** (mindquest:// and https://mindquest.app/*)
- [ ] **Push notifications** via Firebase Cloud Messaging
- [ ] **SQLDelight offline cache** — re-enable when Kotlin is upgraded to 2.2.x
- [ ] **Smart login suggestion UI** — use `SessionPrefs.lastAuthProvider` to highlight preferred login method on ExistingLoginScreen
- [ ] **Phone linking** — wire `linkAccountWithPhone()` through OTP flow in AccountLinkingSheet
- [ ] **Post-linking action resumption** — auto-resume tournament entry / leaderboard after successful linking
- [ ] **Device/session management** — allow users to see and revoke sessions on other devices
- [ ] **Offline sync conflict resolution** — handle data collisions for multi-device usage
