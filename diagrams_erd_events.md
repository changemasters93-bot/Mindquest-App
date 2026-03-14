# MindQuest V2 — ERD & Event Diagrams

---

## 1. ENTITY RELATIONSHIP DIAGRAM (ERD)

```
 ┌─────────────────────────────────────────────────────────────────────────────────────────────┐
 │                              AUTHENTICATION (Supabase Auth)                                 │
 │                                                                                             │
 │   ┌───────────────┐                                                                         │
 │   │  auth.users   │  (managed by Supabase — Google, Phone OTP, Anonymous)                   │
 │   │───────────────│                                                                         │
 │   │ id       (PK) │◄─────────────────────────────────────────────────────────────┐           │
 │   │ email         │                                                              │           │
 │   │ phone         │                                                              │           │
 │   │ provider      │                                                              │           │
 │   └──────┬────────┘                                                              │           │
 │          │ 1:1                                                                   │ 1:1       │
 └──────────┼───────────────────────────────────────────────────────────────────────┼───────────┘
            │                                                                       │
            ▼                                                                       ▼
 ┌──────────────────────┐                                              ┌────────────────────────┐
 │      users            │                                              │    admin_users (CMS)   │
 │──────────────────────│                                              │────────────────────────│
 │ id           (PK/FK) │──►auth.users                                 │ id          (PK/FK)    │──►auth.users
 │ display_name         │                                              │ display_name           │
 │ avatar_id            │                                              │ email                  │
 │ grade_id      (FK)   │──┐                                           │ role (super_admin/     │
 │ auth_provider        │  │                                           │   admin/moderator/     │
 │ country_id    (FK)   │──┼──┐                                        │   content_editor)      │
 │ city_id       (FK)   │──┼──┼──┐                                     │ permissions  (JSONB)   │
 │ school_name          │  │  │  │                                     │ is_active              │
 │ google_sub           │  │  │  │                                     │ created_at             │
 │ total_xp             │  │  │  │                                     │ updated_at             │
 │ level                │  │  │  │                                     └────────────────────────┘
 │ streak_current       │  │  │  │
 │ streak_best          │  │  │  │
 │ is_banned            │  │  │  │
 │ banned_reason        │  │  │  │
 │ banned_at            │  │  │  │
 │ last_active_at       │  │  │  │
 │ created_at           │  │  │  │
 │ updated_at           │  │  │  │
 └─────────┬────────────┘  │  │  │
           │               │  │  │
           │               │  │  │
           │               ▼  │  │
           │    ┌──────────────────┐          ┌──────────────────┐
           │    │     grades       │          │   countries      │
           │    │──────────────────│          │──────────────────│
           │    │ id        (PK)   │          │ id        (PK)   │◄──┐
           │    │ code      (UQ)   │          │ name             │   │
           │    │ label            │          │ code      (UQ)   │   │
           │    │ sort_order       │          │ is_active        │   │
           │    │ created_by       │          │ updated_at       │   │
           │    │ updated_at       │          └──────────────────┘   │
           │    └──────────────────┘                                 │
           │              ▲                     ┌──────────────────┐ │
           │              │                     │     cities       │ │
           │              │                     │──────────────────│ │
           │              │                     │ id        (PK)   │ │
           │              │                     │ country_id (FK)  │─┘
           │              │                     │ name             │
           │              │                     │ created_by       │
           │              │                     │ updated_at       │
           │              │                     │ UQ(country,name) │
           │              │                     └──────────────────┘
           │              │
           │              │
```

### Content Hierarchy (Module → Chapter → Quiz → Question)

```
 ┌───────────────────────────┐
 │         modules            │
 │───────────────────────────│
 │ id              (PK)      │
 │ title                     │
 │ subtitle                  │
 │ description               │
 │ emoji                     │
 │ accent_color              │
 │ thumbnail_url             │
 │ display_order             │
 │ grade_id          (FK)    │──► grades
 │ is_active                 │
 │ status (draft/review/     │
 │   published/archived)     │
 │ created_by                │
 │ updated_by                │
 │ published_at              │
 │ created_at / updated_at   │
 └──────────┬────────────────┘
            │ 1:N
            ▼
 ┌───────────────────────────┐
 │        chapters            │
 │───────────────────────────│
 │ id              (PK)      │
 │ module_id        (FK)     │──► modules (CASCADE)
 │ title                     │
 │ description               │
 │ chapter_number            │
 │ sort_order                │
 │ is_active                 │
 │ status                    │
 │ created_by / updated_by   │
 │ published_at              │
 │ created_at / updated_at   │
 └──────────┬────────────────┘
            │ 1:N
            ▼
 ┌───────────────────────────┐
 │         quizzes            │
 │───────────────────────────│
 │ id              (PK)      │
 │ chapter_id       (FK)     │──► chapters (CASCADE)
 │ title                     │
 │ description               │
 │ quiz_type (practice/iq/   │
 │   tournament/daily)       │
 │ question_count            │
 │ time_limit_secs           │
 │ max_xp                    │
 │ difficulty                │
 │ passing_score_pct         │
 │ shuffle_options           │
 │ show_explanation          │
 │ sort_order                │
 │ is_active / status        │
 │ created_by / updated_by   │
 │ published_at              │
 │ created_at / updated_at   │
 └──────────┬────────────────┘
            │ 1:N
            ▼
 ┌───────────────────────────┐        ┌───────────────────────────┐
 │       questions            │        │    question_options       │
 │───────────────────────────│        │───────────────────────────│
 │ id              (PK)      │◄───┐   │ id              (PK)      │
 │ quiz_id          (FK)     │──► │   │ question_id      (FK)     │──► questions (CASCADE)
 │ question_type             │    │   │ label                     │
 │   (multiple_choice/       │    │   │ is_correct                │
 │    true_false/ordering/   │    │   │ sort_order                │
 │    match/fill_blank/      │    │   │ correct_position          │  ← for ordering type
 │    select_word/matrix/    │    │   │ media_url                 │
 │    grid_pattern/          │    │   │ visual_label              │  ← for memory/visual types
 │    statement_reason/      │    │   └───────────────────────────┘
 │    table_data/memory/     │    │
 │    visual_single_choice)  │    │   ┌───────────────────────────┐
 │ title                     │    │   │      match_pairs          │
 │ prompt                    │    │   │───────────────────────────│
 │ explanation               │    │   │ id              (PK)      │
 │ difficulty                │    └───│ question_id      (FK)     │──► questions (CASCADE)
 │ time_limit_secs           │        │ left_text                 │
 │ allow_multiple            │        │ right_text                │
 │ prompt_config   (JSONB)   │        │ sort_order                │
 │ metadata        (JSONB)   │        └───────────────────────────┘
 │ media_url                 │
 │ sort_order / status       │
 │ created_by / updated_by   │
 │ created_at / updated_at   │
 └───────────────────────────┘
```

### User Activity & Tournaments

```
 ┌─────────────────────────────┐
 │       quiz_attempts          │
 │─────────────────────────────│
 │ id                (PK)      │
 │ user_id            (FK)     │──► users (CASCADE)
 │ quiz_id            (FK)     │──► quizzes (CASCADE)
 │ score                       │
 │ total_questions              │
 │ xp_earned                   │
 │ time_taken_secs              │
 │ answers           (JSONB)   │
 │ idempotency_key             │
 │ created_at                  │
 │ UQ(user_id, idempotency_key)│
 └─────────────────────────────┘

 ┌─────────────────────────────┐
 │       daily_challenges       │
 │─────────────────────────────│
 │ id                (PK)      │
 │ quiz_id            (FK)     │──► quizzes (CASCADE)
 │ grade_id           (FK)     │──► grades
 │ challenge_date              │
 │ is_active                   │
 │ created_by                  │
 │ created_at                  │
 │ UQ(quiz_id, challenge_date) │
 └─────────────────────────────┘

 ┌─────────────────────────────┐       ┌──────────────────────────────┐
 │       tournaments            │       │    tournament_questions       │
 │─────────────────────────────│       │──────────────────────────────│
 │ id                (PK)      │◄──┐   │ id                (PK)       │
 │ title                       │   │   │ tournament_id      (FK)      │──► tournaments (CASCADE)
 │ description                 │   │   │ question_id        (FK)      │──► questions (CASCADE)
 │ grade_id           (FK)     │──►│   │ sort_order                   │
 │ question_count              │   │   │ UQ(tournament_id,question_id)│
 │ time_limit_seconds          │   │   └──────────────────────────────┘
 │ max_participants            │   │
 │ starts_at / ends_at         │   │   ┌──────────────────────────────┐
 │ status (draft/scheduled/    │   │   │    tournament_entries         │
 │   live/closed/finalized)    │   │   │──────────────────────────────│
 │ created_by / updated_by     │   │   │ id                (PK)       │
 │ created_at / updated_at     │   └───│ tournament_id      (FK)      │──► tournaments (CASCADE)
 └─────────────────────────────┘       │ user_id            (FK)      │──► users (CASCADE)
                                       │ status (not_started/         │
                                       │   in_progress/paused/        │
                                       │   completed/auto_submitted)  │
                                       │ score                        │
                                       │ time_taken_seconds           │
                                       │ rank                         │
                                       │ time_remaining_secs          │
                                       │ answers_so_far    (JSONB)    │
                                       │ started_at / completed_at    │
                                       │ created_at                   │
                                       │ UQ(tournament_id, user_id)   │
                                       └──────────────────────────────┘
```

### CMS / Admin Tables

```
 ┌─────────────────────────────┐    ┌─────────────────────────────┐
 │        audit_logs (CMS)      │    │    media_uploads (CMS)       │
 │─────────────────────────────│    │─────────────────────────────│
 │ id                (PK)      │    │ id                (PK)      │
 │ admin_id                    │    │ uploaded_by                 │
 │ action (create/update/      │    │ file_name                   │
 │   delete/publish/archive/   │    │ file_size                   │
 │   ban/unban)                │    │ mime_type                   │
 │ table_name                  │    │ storage_path                │
 │ record_id                   │    │ cdn_url                     │
 │ old_values       (JSONB)    │    │ alt_text                    │
 │ new_values       (JSONB)    │    │ width / height              │
 │ created_at                  │    │ created_at                  │
 └─────────────────────────────┘    └─────────────────────────────┘

 ┌───────────────────────────────┐
 │  content_status_history (CMS)  │
 │───────────────────────────────│
 │ id                  (PK)      │
 │ content_type (module/chapter/ │
 │   quiz/question/tournament)   │
 │ content_id                    │
 │ status                        │
 │ changed_by                    │
 │ reason                        │
 │ created_at                    │
 └───────────────────────────────┘
```

### Complete Relationship Map (Summary)

```
  auth.users ─────┬──── 1:1 ────► users ──────────┬── N:1 ──► grades
                  │                                ├── N:1 ──► countries
                  │                                └── N:1 ──► cities ──── N:1 ──► countries
                  │
                  └──── 1:1 ────► admin_users

  grades ◄── N:1 ── modules ◄── 1:N ── chapters ◄── 1:N ── quizzes ◄── 1:N ── questions
                                                                                  │
                                                               ┌──────────────────┤
                                                               ▼                  ▼
                                                        question_options     match_pairs

  users ──── 1:N ──► quiz_attempts ◄── N:1 ── quizzes
  users ──── 1:N ──► tournament_entries ◄── N:1 ── tournaments ──── N:1 ──► grades

  tournaments ── 1:N ──► tournament_questions ◄── N:1 ── questions
  quizzes ── 1:N ──► daily_challenges ◄── N:1 ── grades
```

---

## 2. EVENT / FLOW DIAGRAMS

### 2A. Mobile App — User Authentication Flow

```
  ┌─────────────┐
  │   App Launch  │
  └──────┬───────┘
         │
         ▼
  ┌──────────────────┐     YES    ┌──────────────────┐
  │ Session exists?  │───────────►│  get_user_dashboard│
  └──────┬───────────┘            │  (p_user_id)      │
         │ NO                     └────────┬──────────┘
         ▼                                 │
  ┌──────────────────┐                     ▼
  │ User Type Choice  │            ┌──────────────────┐
  │                   │            │  HOME SCREEN      │
  │ ┌──────────────┐  │            │  • user info      │
  │ │ I'm New Here │  │            │  • stats          │
  │ └──────┬───────┘  │            │  • modules list   │
  │        │          │            │  • tournament      │
  │ ┌──────────────┐  │            └──────────────────┘
  │ │ Already Have │  │
  │ │  Account     │  │
  │ └──────┬───────┘  │
  └────────┼──────────┘
    NEW    │    EXISTING
   ┌───────┘     └──────────────┐
   ▼                            ▼
  ┌──────────────┐    ┌──────────────────┐
  │ 3 Onboarding │    │ Existing Login   │
  │   Slides     │    │  • Google Sign-In│
  └──────┬───────┘    │  • Phone OTP     │
         │            └───────┬──────────┘
         ▼                    │
  ┌──────────────┐            │
  │ Profile Setup│            │
  │ Step 1: Name │            │
  │   Country    │  SELECT    │
  │   City       │──grades──► │
  │   Avatar     │  SELECT    │
  │ Step 2: Grade│──countries─►│
  │   School     │  SELECT    │
  │ Step 3: Auth │──cities───► │
  │  (Google/    │            │
  │   Phone/Anon)│            │
  └──────┬───────┘            │
         │                    │
         ▼                    ▼
  ┌──────────────────────────────┐
  │  UPSERT public.users         │
  │  (id, display_name, avatar,  │
  │   grade_id, auth_provider,   │
  │   country_id, city_id,       │
  │   school_name, google_sub)   │
  └──────────────┬───────────────┘
                 │
                 ▼
          ┌──────────────────┐
          │ get_user_dashboard│──────► HOME SCREEN
          └──────────────────┘
```

### 2B. Mobile App — Quiz Flow

```
  HOME SCREEN
       │
       ▼
  ┌──────────────────┐
  │ Tap Module        │
  │                   │
  │ get_module_full   │
  │ (p_module_id,     │
  │  p_user_id)       │
  └────────┬──────────┘
           │
           ▼
  ┌──────────────────┐
  │ MODULE DETAIL     │
  │  • chapters list  │
  │  • lock/unlock    │
  │  • progress       │
  │                   │
  │ Tap Chapter       │
  │                   │
  │ get_chapter_quizzes│
  │ (p_chapter_id,    │
  │  p_user_id)       │
  └────────┬──────────┘
           │
           ▼
  ┌──────────────────┐
  │ QUIZ LIST         │
  │  • quiz cards     │
  │  • best_score     │
  │  • attempt_count  │
  │                   │
  │ Tap Quiz          │
  └────────┬──────────┘
           │
           ▼
  ┌──────────────────────────────────────────────────────────────┐
  │ QUIZ PLAYER                                                  │
  │                                                              │
  │  12 Question Types:                                          │
  │  ┌─────────────────┐ ┌─────────────────┐ ┌────────────────┐ │
  │  │ multiple_choice  │ │ true_false      │ │ ordering       │ │
  │  │ (4-option MCQ)   │ │ (2-option)      │ │ (drag-to-order)│ │
  │  └─────────────────┘ └─────────────────┘ └────────────────┘ │
  │  ┌─────────────────┐ ┌─────────────────┐ ┌────────────────┐ │
  │  │ match            │ │ fill_blank      │ │ select_word    │ │
  │  │ (draw lines)     │ │ (___ in title)  │ │ (tap the word) │ │
  │  └─────────────────┘ └─────────────────┘ └────────────────┘ │
  │  ┌─────────────────┐ ┌─────────────────┐ ┌────────────────┐ │
  │  │ matrix           │ │ grid_pattern    │ │ statement_     │ │
  │  │ (prompt_config)  │ │ (prompt_config) │ │ reason         │ │
  │  │ {"rows":[...]}   │ │ {"grid":[...]}  │ │ (metadata)     │ │
  │  └─────────────────┘ └─────────────────┘ └────────────────┘ │
  │  ┌─────────────────┐ ┌─────────────────┐ ┌────────────────┐ │
  │  │ table_data       │ │ memory          │ │ visual_single  │ │
  │  │ (prompt_config)  │ │ (visual_label   │ │ _choice        │ │
  │  │ {"headers":[...]}│ │  emoji pairs)   │ │ (visual_label) │ │
  │  └─────────────────┘ └─────────────────┘ └────────────────┘ │
  └──────────────────────────┬───────────────────────────────────┘
                             │
                             ▼
  ┌──────────────────────────────────────────┐
  │ submit_quiz_attempt(payload)              │
  │                                          │
  │ payload = {                              │
  │   user_id, quiz_id, time_taken_secs,     │
  │   idempotency_key,                       │
  │   answers: [{question_id, selected_id,   │
  │              is_correct}, ...]            │
  │ }                                        │
  └──────────────────┬───────────────────────┘
                     │
                     ▼
  ┌──────────────────────────────────────────┐
  │ SERVER-SIDE PROCESSING                    │
  │                                          │
  │ 1. Check idempotency → skip if duplicate │
  │ 2. Count score from answers              │
  │ 3. Check if replay → XP = 0 if replay   │
  │ 4. INSERT quiz_attempts                  │
  │ 5. UPDATE users.total_xp + level         │
  │    Level thresholds:                     │
  │    300→L2, 600→L3, 1000→L4, 1400→L5     │
  │    1900→L6, 2500→L7, 3200→L8            │
  │    4000→L9, 5000→L10                    │
  │ 6. Calculate global rank                 │
  │ 7. Find next_quiz_id                    │
  └──────────────────┬───────────────────────┘
                     │
                     ▼
  ┌──────────────────────────────────────────┐
  │ RESULT SCREEN                             │
  │  • score / total_questions               │
  │  • xp_earned                             │
  │  • level (+ level_changed animation)     │
  │  • rank_global                           │
  │  • next_quiz_id → "Next Quiz" button     │
  └──────────────────────────────────────────┘
```

### 2C. Mobile App — Tournament Flow

```
  HOME SCREEN
       │
       │  get_active_tournament(p_user_id, p_grade_id)
       ▼
  ┌───────────────────────────┐
  │ TOURNAMENT CARD            │
  │  • title, starts_at       │
  │  • participant_count      │
  │  • user_entry_status      │
  │  • time_limit_seconds     │
  └──────────┬────────────────┘
             │ Tap "Join/Resume"
             ▼
  ┌───────────────────────────────────────────────┐
  │ TOURNAMENT STATE MACHINE                       │
  │                                               │
  │  ┌────────────┐  start_tournament   ┌────────────────┐
  │  │ not_started │──────────────────►│  in_progress    │
  │  └────────────┘                    └───┬────┬───────┘
  │                                        │    │
  │                         pause_tournament│    │submit_tournament_answer
  │                                        ▼    │  (per question)
  │                                  ┌─────────┐│
  │                                  │ paused   ││
  │                                  └────┬────┘│
  │                        resume_tournament│    │
  │                                        ▼    │
  │                                  ┌─────────┐│
  │                                  │in_progress│◄┘
  │                                  └────┬────┘
  │                          submit_tournament│
  │                          (p_entry_id,     │
  │                           p_answers,      │
  │                           p_time_taken)   │
  │                                        ▼
  │                                  ┌───────────┐
  │                                  │ completed  │
  │                                  └───────────┘
  └───────────────────────────────────────────────┘
                     │
                     ▼
  ┌──────────────────────────────────────────┐
  │ TOURNAMENT RESULT (QuizResultDto)        │
  │  • score, xp_earned, rank_global        │
  │  • level, level_changed                 │
  └──────────────────────────────────────────┘
```

### 2D. Mobile App — Daily Challenge Flow

```
  HOME SCREEN
       │
       │  get_daily_challenges(p_user_id)
       ▼
  ┌───────────────────────────┐
  │ DAILY CHALLENGES LIST      │
  │  • quiz_id, title         │
  │  • time_in_minutes        │
  │  • is_done (✓/✗)          │
  └──────────┬────────────────┘
             │ Tap challenge
             ▼
  ┌───────────────────────────┐
  │ get_chapter_quizzes       │
  │ (chapter_id, user_id)     │──────► QUIZ PLAYER (same as 2B)
  └───────────────────────────┘
```

### 2E. Mobile App — Leaderboard & Stats Flow

```
  ┌──────────────────────────────────────────────────────────────┐
  │ LEADERBOARD TAB                                              │
  │                                                              │
  │  get_leaderboard(p_user_id, p_filter, p_filter_id,           │
  │                  p_limit, p_offset)                           │
  │                                                              │
  │  Filters: ┌──────────┐ ┌──────────┐ ┌──────┐ ┌──────────┐   │
  │           │ Global   │ │ Country  │ │ City │ │ School   │   │
  │           └──────────┘ └──────────┘ └──────┘ └──────────┘   │
  │                                                              │
  │  Response:                                                   │
  │  • ranked_users [{user_id, display_name, avatar_id,          │
  │                   total_xp, rank}, ...]                      │
  │  • user_rank {rank_global, rank_country, rank_city, xp_gap}  │
  └──────────────────────────────────────────────────────────────┘

  ┌──────────────────────────────────────────────────────────────┐
  │ STATS TAB                                                    │
  │                                                              │
  │  get_user_stats(p_user_id, p_period)                         │
  │                                                              │
  │  Periods: ┌──────┐ ┌───────┐ ┌──────────┐                   │
  │           │ Week │ │ Month │ │ All-time │                   │
  │           └──────┘ └───────┘ └──────────┘                   │
  │                                                              │
  │  Response:                                                   │
  │  • stats {total_xp, level, streak, quizzes, accuracy, ...}   │
  │  • daily_activity [{date, quizzes, xp, time_spent}, ...]     │
  │  • subject_performance [{module_id, title, emoji,            │
  │                          best_score, accuracy}, ...]          │
  └──────────────────────────────────────────────────────────────┘

  ┌──────────────────────────────────────────────────────────────┐
  │ PROFILE TAB                                                  │
  │                                                              │
  │  get_profile(p_user_id)                                      │
  │                                                              │
  │  Response:                                                   │
  │  • user {name, avatar, grade, country, city, school,         │
  │          auth_provider}                                       │
  │  • stats {xp, level, streaks, quizzes, tournaments}          │
  │  • completed_chapters [{chapter, module, date}, ...]         │
  │  • tournament_results [{title, score, rank, date}, ...]      │
  │                                                              │
  │  update_profile(p_user_id, p_fields)                         │
  │  • allowed: display_name, avatar_id, grade_id,               │
  │             school_name, country_id, city_id                 │
  └──────────────────────────────────────────────────────────────┘
```

### 2F. CMS Admin Panel — Content Management Flow

```
  ┌─────────────────────────────────────────────────────────────────────────┐
  │ CMS ADMIN PANEL                                                        │
  │                                                                         │
  │ ┌─────────────┐    is_admin() check on EVERY RPC                       │
  │ │ Admin Login  │    (must be in admin_users table + is_active)          │
  │ └──────┬──────┘                                                        │
  │        ▼                                                                │
  │ ┌─────────────────────────────────────────────────────────────────────┐ │
  │ │ DASHBOARD                                                           │ │
  │ │  admin_dashboard_stats() →                                          │ │
  │ │    total_users, total_modules, total_chapters, total_quizzes,       │ │
  │ │    total_questions, total_attempts, active_tournaments,              │ │
  │ │    users_today, attempts_today, banned_users                         │ │
  │ │                                                                     │ │
  │ │  admin_get_analytics(p_days) →                                      │ │
  │ │    user_growth [{date, new_users}],                                 │ │
  │ │    quiz_stats {attempts, avg_score, daily_attempts},                │ │
  │ │    top_modules [{title, emoji, attempts, users}]                    │ │
  │ └─────────────────────────────────────────────────────────────────────┘ │
  │                                                                         │
  │ ┌─────────────────────────────────────────────────────────────────────┐ │
  │ │ CONTENT MANAGEMENT (Modules → Chapters → Quizzes → Questions)      │ │
  │ │                                                                     │ │
  │ │  LIST:                                                              │ │
  │ │    admin_list_modules(status, grade_id, search, limit, offset)      │ │
  │ │    admin_list_chapters(module_id, status, limit, offset)            │ │
  │ │    admin_list_quizzes(chapter_id, status, search, limit, offset)    │ │
  │ │    admin_list_questions(quiz_id, type, status, limit, offset)       │ │
  │ │    admin_get_question_detail(question_id)                           │ │
  │ │                                                                     │ │
  │ │  CREATE / UPDATE:                                                   │ │
  │ │    admin_upsert_module(data)     ─┐                                 │ │
  │ │    admin_upsert_chapter(data)     ├── All log to audit_logs         │ │
  │ │    admin_upsert_quiz(data)        │                                 │ │
  │ │    admin_upsert_question(data)   ─┘   (with options + match_pairs) │ │
  │ │                                                                     │ │
  │ │  LIFECYCLE:                                                         │ │
  │ │    admin_publish_content(type, id)  →  status='published'           │ │
  │ │    admin_archive_content(type, id)  →  status='archived'            │ │
  │ │    admin_delete_content(table, id)  →  hard delete                  │ │
  │ │    admin_reorder(table, [{id,order},...])                            │ │
  │ └─────────────────────────────────────────────────────────────────────┘ │
  │                                                                         │
  │ ┌─────────────────────────────────────────────────────────────────────┐ │
  │ │ CONTENT STATUS LIFECYCLE                                            │ │
  │ │                                                                     │ │
  │ │  ┌───────┐  publish  ┌───────────┐  archive  ┌──────────┐          │ │
  │ │  │ draft │──────────►│ published │──────────►│ archived │          │ │
  │ │  └───┬───┘           └─────┬─────┘           └──────────┘          │ │
  │ │      │                     │                                        │ │
  │ │      │    ┌────────┐       │                                        │ │
  │ │      └───►│ review │───────┘  (optional review step)               │ │
  │ │           └────────┘                                                │ │
  │ │                                                                     │ │
  │ │  Every transition logs to: content_status_history                   │ │
  │ └─────────────────────────────────────────────────────────────────────┘ │
  │                                                                         │
  │ ┌─────────────────────────────────────────────────────────────────────┐ │
  │ │ TOURNAMENT MANAGEMENT                                               │ │
  │ │                                                                     │ │
  │ │  admin_list_tournaments(status, grade_id, limit, offset)            │ │
  │ │  admin_upsert_tournament(data)                                      │ │
  │ │  admin_set_tournament_questions(tournament_id, question_ids)         │ │
  │ │                                                                     │ │
  │ │  LIFECYCLE: admin_tournament_lifecycle(tournament_id, action)        │ │
  │ │                                                                     │ │
  │ │  ┌───────┐ schedule ┌───────────┐ go_live ┌──────┐ close ┌────────┐│ │
  │ │  │ draft │─────────►│ scheduled │────────►│ live │──────►│ closed ││ │
  │ │  └───────┘          └───────────┘         └──────┘       └───┬────┘│ │
  │ │                                                              │      │ │
  │ │                                              finalize        │      │ │
  │ │                                  (auto-ranks all entries)    ▼      │ │
  │ │                                                        ┌───────────┐│ │
  │ │                                                        │ finalized ││ │
  │ │                                                        └───────────┘│ │
  │ └─────────────────────────────────────────────────────────────────────┘ │
  │                                                                         │
  │ ┌─────────────────────────────────────────────────────────────────────┐ │
  │ │ USER MANAGEMENT                                                     │ │
  │ │                                                                     │ │
  │ │  admin_get_users(search, grade_id, country_id, include_banned,      │ │
  │ │                  limit, offset)                                      │ │
  │ │  admin_ban_user(user_id, reason)   → is_banned=true + audit_log     │ │
  │ │  admin_unban_user(user_id)         → is_banned=false + audit_log    │ │
  │ └─────────────────────────────────────────────────────────────────────┘ │
  │                                                                         │
  │ ┌─────────────────────────────────────────────────────────────────────┐ │
  │ │ REFERENCE DATA MANAGEMENT                                           │ │
  │ │                                                                     │ │
  │ │  admin_upsert_grade(code, label, sort_order)                        │ │
  │ │  admin_upsert_country(name, code, is_active)                        │ │
  │ │  admin_upsert_city(country_id, name)                                │ │
  │ └─────────────────────────────────────────────────────────────────────┘ │
  │                                                                         │
  │ ┌─────────────────────────────────────────────────────────────────────┐ │
  │ │ DAILY CHALLENGES                                                    │ │
  │ │                                                                     │ │
  │ │  admin_list_daily_challenges(date, grade_id)                        │ │
  │ │  admin_set_daily_challenge(quiz_id, grade_id, date)                 │ │
  │ └─────────────────────────────────────────────────────────────────────┘ │
  │                                                                         │
  │ ┌─────────────────────────────────────────────────────────────────────┐ │
  │ │ MEDIA & AUDIT                                                       │ │
  │ │                                                                     │ │
  │ │  admin_register_media(file_name, size, mime, path, cdn_url, ...)    │ │
  │ │  admin_list_media(mime_filter, limit, offset)                       │ │
  │ │  admin_get_audit_logs(table_name, action, admin_id, limit, offset)  │ │
  │ └─────────────────────────────────────────────────────────────────────┘ │
  └─────────────────────────────────────────────────────────────────────────┘
```

### 2G. XP & Level Progression System

```
  ┌──────────────────────────────────────────────────────────────────┐
  │ XP EARNING EVENTS                                                │
  │                                                                  │
  │  Quiz Completion:                                                │
  │    XP = max_xp × (score / total_questions)                      │
  │    Replay = 0 XP (idempotency check)                            │
  │                                                                  │
  │  Tournament Completion:                                          │
  │    XP = score × 10                                              │
  │                                                                  │
  │  Level Thresholds:                                               │
  │  ┌──────┬───────┬───────┬───────┬───────┬───────┬───────┐       │
  │  │ L1   │ L2    │ L3    │ L4    │ L5    │ L6    │ L7    │       │
  │  │ 0    │ 300   │ 600   │ 1000  │ 1400  │ 1900  │ 2500  │       │
  │  ├──────┴───────┴───────┼───────┴───────┴───────┴───────┤       │
  │  │ L8    │ L9    │ L10  │                               │       │
  │  │ 3200  │ 4000  │ 5000 │                               │       │
  │  └───────┴───────┴──────┘                               │       │
  │                                                          │       │
  │  On level change: level_changed = true in response       │       │
  │  → App shows level-up animation                          │       │
  └──────────────────────────────────────────────────────────────────┘
```

### 2H. RLS (Row Level Security) Access Matrix

```
  ┌────────────────────┬──────────────────┬──────────────────┬────────┐
  │ Table              │ App User         │ Admin            │ Anon   │
  ├────────────────────┼──────────────────┼──────────────────┼────────┤
  │ grades             │ SELECT           │ ALL              │ SELECT │
  │ countries          │ SELECT           │ ALL              │ SELECT │
  │ cities             │ SELECT           │ ALL              │ SELECT │
  │ users              │ own row SIUD     │ SELECT + UPDATE  │ —      │
  │ admin_users        │ own row SELECT   │ SELECT + MODIFY  │ —      │
  │ modules            │ SELECT           │ ALL (CRUD)       │ —      │
  │ chapters           │ SELECT           │ ALL (CRUD)       │ —      │
  │ quizzes            │ SELECT           │ ALL (CRUD)       │ —      │
  │ questions          │ SELECT           │ ALL (CRUD)       │ —      │
  │ question_options   │ SELECT           │ ALL (CRUD)       │ —      │
  │ match_pairs        │ SELECT           │ ALL (CRUD)       │ —      │
  │ quiz_attempts      │ own rows SI      │ SELECT all       │ —      │
  │ daily_challenges   │ SELECT           │ ALL (CRUD)       │ —      │
  │ tournaments        │ SELECT           │ ALL (CRUD)       │ —      │
  │ tournament_q's     │ SELECT           │ ALL (CRUD)       │ —      │
  │ tournament_entries │ SELECT all + own │ UPDATE all       │ —      │
  │ audit_logs         │ —                │ SELECT + INSERT  │ —      │
  │ media_uploads      │ —                │ SELECT+INS+DEL   │ —      │
  │ content_history    │ —                │ SELECT + INSERT  │ —      │
  └────────────────────┴──────────────────┴──────────────────┴────────┘

  Legend: S=SELECT  I=INSERT  U=UPDATE  D=DELETE
          "own row" = WHERE auth.uid() = id/user_id
          "ALL" = full CRUD via is_admin() check
```

---

## 3. COMPLETE RPC FUNCTION MAP

```
  ┌──────────────────────────────────────────────────────────────────────┐
  │                    45 RPC FUNCTIONS                                  │
  ├──────────────────────────────────────────────────────────────────────┤
  │                                                                      │
  │  APP RPCs (15) — called by mobile app                               │
  │  ─────────────────────────────────────                               │
  │  get_user_dashboard(user_id)                                         │
  │  get_daily_challenges(user_id)                                       │
  │  get_module_full(module_id, user_id)                                 │
  │  get_chapter_quizzes(chapter_id, user_id)                            │
  │  submit_quiz_attempt(payload)                                        │
  │  get_active_tournament(user_id, grade_id)                            │
  │  start_tournament(user_id, tournament_id)                            │
  │  pause_tournament(entry_id)                                          │
  │  resume_tournament(entry_id)                                         │
  │  submit_tournament(entry_id, answers, time_taken)                    │
  │  submit_tournament_answer(entry_id, answer)                          │
  │  get_leaderboard(user_id, filter, filter_id, limit, offset)          │
  │  get_user_stats(user_id, period)                                     │
  │  get_profile(user_id)                                                │
  │  update_profile(user_id, fields)                                     │
  │                                                                      │
  │  CMS RPCs (30) — called by admin panel                              │
  │  ────────────────────────────────────                                │
  │  DASHBOARD:                                                          │
  │    admin_dashboard_stats()                                           │
  │    admin_get_analytics(days)                                         │
  │                                                                      │
  │  USER MANAGEMENT:                                                    │
  │    admin_get_users(search, grade_id, country_id, banned, lim, off)   │
  │    admin_ban_user(user_id, reason)                                   │
  │    admin_unban_user(user_id)                                         │
  │                                                                      │
  │  CONTENT CRUD:                                                       │
  │    admin_upsert_module(data)       admin_list_modules(...)           │
  │    admin_upsert_chapter(data)      admin_list_chapters(...)          │
  │    admin_upsert_quiz(data)         admin_list_quizzes(...)           │
  │    admin_upsert_question(data)     admin_list_questions(...)         │
  │    admin_delete_content(table, id) admin_get_question_detail(id)     │
  │    admin_publish_content(type, id)                                   │
  │    admin_archive_content(type, id)                                   │
  │    admin_reorder(table, order)                                       │
  │                                                                      │
  │  REFERENCE DATA:                                                     │
  │    admin_upsert_grade(code, label, sort_order)                       │
  │    admin_upsert_country(name, code, is_active)                       │
  │    admin_upsert_city(country_id, name)                               │
  │                                                                      │
  │  TOURNAMENTS:                                                        │
  │    admin_upsert_tournament(data)                                     │
  │    admin_set_tournament_questions(tournament_id, question_ids)        │
  │    admin_list_tournaments(status, grade_id, limit, offset)           │
  │    admin_tournament_lifecycle(tournament_id, action)                  │
  │                                                                      │
  │  DAILY CHALLENGES:                                                   │
  │    admin_set_daily_challenge(quiz_id, grade_id, date)                │
  │    admin_list_daily_challenges(date, grade_id)                       │
  │                                                                      │
  │  MEDIA & AUDIT:                                                      │
  │    admin_register_media(file, size, mime, path, cdn, alt, w, h)      │
  │    admin_list_media(mime_filter, limit, offset)                      │
  │    admin_get_audit_logs(table, action, admin_id, limit, offset)      │
  │                                                                      │
  │  HELPER:                                                             │
  │    is_admin() — checks admin_users table                             │
  └──────────────────────────────────────────────────────────────────────┘
```
