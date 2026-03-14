# Mindquest V2 - Content Creator Guide

## Table of Contents
1. [Content Hierarchy](#content-hierarchy)
2. [All 15 Question Types](#all-15-question-types)
3. [Module Quiz vs IQ Quiz](#module-quiz-vs-iq-quiz)
4. [Step-by-Step Guides](#step-by-step-guides)
5. [Important Rules & Constraints](#important-rules--constraints)
6. [Common Pitfalls](#common-pitfalls)

---

## Content Hierarchy

```
Module (e.g., "Mathematics")
  └── Chapter (e.g., "Chapter 1: Addition")
        └── Quiz (e.g., "Basic Addition Quiz")
              └── Question (e.g., "What is 2 + 2?")
                    ├── Options (for most types)
                    └── Match Pairs (for match type only)
```

Each level has:
- `status`: draft → review → published → archived
- `sort_order`: controls display order
- `is_active`: master visibility toggle

**Content is only visible to users when `status = 'published'` AND `is_active = true`, AND all parent items are also published.**

---

## All 15 Question Types

### 1. MULTIPLE_CHOICE

Standard multiple-choice with one correct answer.

**Database fields:**
- `question_type`: `'multiple_choice'`
- `prompt_config`: Not required
- `metadata`: Not required

**Options:** 2-6 options, exactly ONE with `is_correct = true`

```sql
-- Question
INSERT INTO questions (quiz_id, question_type, title, explanation) VALUES
('quiz-uuid', 'multiple_choice', 'What is the capital of France?', 'Paris is the capital of France.');

-- Options
INSERT INTO question_options (question_id, label, is_correct, sort_order) VALUES
('q-uuid', 'London', false, 1),
('q-uuid', 'Paris', true, 2),
('q-uuid', 'Berlin', false, 3),
('q-uuid', 'Rome', false, 4);
```

---

### 2. TRUE_FALSE

Binary true/false choice.

**Database fields:**
- `question_type`: `'true_false'`
- `prompt_config`: Not required
- `metadata`: Not required

**Options:** Exactly 2 options with labels "True" and "False"

```sql
INSERT INTO questions (quiz_id, question_type, title, explanation) VALUES
('quiz-uuid', 'true_false', 'The sun rises in the east.', 'The sun rises in the east and sets in the west.');

INSERT INTO question_options (question_id, label, is_correct, sort_order) VALUES
('q-uuid', 'True', true, 1),
('q-uuid', 'False', false, 2);
```

---

### 3. ORDERING

User arranges items in the correct sequence.

**Database fields:**
- `question_type`: `'ordering'`
- `prompt_config`: Not required
- `metadata`: Not required

**Options:** Each option needs `correct_position` (1-based). `sort_order` is the shuffled display order.

```sql
INSERT INTO questions (quiz_id, question_type, title, explanation) VALUES
('quiz-uuid', 'ordering', 'Arrange these planets from closest to farthest from the Sun.', 'Mercury, Venus, Earth, Mars');

INSERT INTO question_options (question_id, label, is_correct, sort_order, correct_position) VALUES
('q-uuid', 'Earth', true, 3, 3),
('q-uuid', 'Mercury', true, 1, 1),
('q-uuid', 'Mars', true, 2, 4),
('q-uuid', 'Venus', true, 4, 2);
```

**Important:** All options should have `is_correct = true` (they're all part of the sequence). `correct_position` determines the right order. `sort_order` is the initial shuffled display.

---

### 4. MATCH

User matches left items to right items.

**Database fields:**
- `question_type`: `'match'`
- `prompt_config`: Not required
- `metadata`: Not required

**Options:** NOT USED. Uses `match_pairs` table instead.

```sql
INSERT INTO questions (quiz_id, question_type, title, explanation) VALUES
('quiz-uuid', 'match', 'Match each country with its capital.', 'France-Paris, Japan-Tokyo, India-Delhi, Brazil-Brasilia');

INSERT INTO match_pairs (question_id, left_text, right_text, sort_order) VALUES
('q-uuid', 'France', 'Paris', 1),
('q-uuid', 'Japan', 'Tokyo', 2),
('q-uuid', 'India', 'New Delhi', 3),
('q-uuid', 'Brazil', 'Brasilia', 4);
```

**Important:** Do NOT add `question_options` for match questions. Only use `match_pairs`.

---

### 5. FILL_BLANK

User types the answer.

**Database fields:**
- `question_type`: `'fill_blank'`
- `prompt_config`: Not required
- `metadata`: Not required

**Options:** Single option with `is_correct = true` containing the exact answer.

```sql
INSERT INTO questions (quiz_id, question_type, title, explanation) VALUES
('quiz-uuid', 'fill_blank', 'The largest planet in our solar system is _____.', 'Jupiter is the largest planet.');

INSERT INTO question_options (question_id, label, is_correct, sort_order) VALUES
('q-uuid', 'Jupiter', true, 1);
```

**Important:** Answer comparison is **case-insensitive** and **trimmed**. "jupiter", "JUPITER", " Jupiter " will all match.

---

### 6. SELECT_WORD

User selects multiple correct words from a set.

**Database fields:**
- `question_type`: `'select_word'`
- `prompt_config`: Not required
- `metadata`: Not required

**Options:** Multiple options, several with `is_correct = true`.

```sql
INSERT INTO questions (quiz_id, question_type, title, explanation) VALUES
('quiz-uuid', 'select_word', 'Select all the prime numbers:', 'Prime numbers: 2, 3, 5, 7');

INSERT INTO question_options (question_id, label, is_correct, sort_order) VALUES
('q-uuid', '2', true, 1),
('q-uuid', '4', false, 2),
('q-uuid', '5', true, 3),
('q-uuid', '6', false, 4),
('q-uuid', '7', true, 5),
('q-uuid', '9', false, 6);
```

**Important:** User must select ALL correct words and NO incorrect words to get it right.

---

### 7. MATRIX

Grid/matrix with a missing value. User picks the answer from MCQ options.

**Database fields:**
- `question_type`: `'matrix'`
- `prompt_config`: **REQUIRED** - JSON with `rows` key (2D array)
- `metadata`: Optional (e.g., matrix type)

**Options:** MCQ-style options for the missing value.

```sql
INSERT INTO questions (quiz_id, question_type, title, explanation,
  prompt_config) VALUES
('quiz-uuid', 'matrix', 'Find the missing number in the magic square.',
 'Each row sums to 15.',
 '{"rows": [["2","7","6"],["9","5","1"],["4","3","?"]]}');

INSERT INTO question_options (question_id, label, is_correct, sort_order) VALUES
('q-uuid', '6', false, 1),
('q-uuid', '8', true, 2),
('q-uuid', '7', false, 3),
('q-uuid', '9', false, 4);
```

**`prompt_config` schema:**
```json
{
  "rows": [
    ["cell1", "cell2", "cell3"],
    ["cell4", "?", "cell6"],
    ["cell7", "cell8", "cell9"]
  ]
}
```

Use `"?"` for the missing cell. The app highlights it with an accent border.

---

### 8. GRID_PATTERN

Pattern-based grid with a missing element. Similar to MATRIX but uses `grid` key.

**Database fields:**
- `question_type`: `'grid_pattern'`
- `prompt_config`: **REQUIRED** - JSON with `grid` key (2D array)
- `metadata`: Optional

**Options:** MCQ-style options.

```sql
INSERT INTO questions (quiz_id, question_type, title, explanation,
  prompt_config) VALUES
('quiz-uuid', 'grid_pattern', 'Complete the letter pattern.',
 'Letters follow alphabetical order.',
 '{"grid": [["A","B","C"],["D","E","F"],["G","H","?"]]}');

INSERT INTO question_options (question_id, label, is_correct, sort_order) VALUES
('q-uuid', 'I', true, 1),
('q-uuid', 'J', false, 2),
('q-uuid', 'K', false, 3),
('q-uuid', 'G', false, 4);
```

---

### 9. GRID_CELL_SELECT

Grid where user selects the correct value for a highlighted cell. UI renders grid + MCQ options.

**Database fields:**
- `question_type`: `'grid_cell_select'`
- `prompt_config`: **REQUIRED** - JSON with `grid` key (2D array with "?" cell)
- `metadata`: Optional (e.g., `{"constraint": "row_sum=60"}`)

**Options:** MCQ-style options.

```sql
INSERT INTO questions (quiz_id, question_type, title, explanation,
  prompt_config, metadata) VALUES
('quiz-uuid', 'grid_cell_select',
 'Each row sums to 60. Find the missing value.',
 'Row 2: 10 + 25 + ? = 60, so ? = 25.',
 '{"grid": [["20","15","25"],["10","25","?"],["30","10","20"]]}',
 '{"constraint": "row_sum=60"}');

INSERT INTO question_options (question_id, label, is_correct, sort_order) VALUES
('q-uuid', '20', false, 1),
('q-uuid', '25', true, 2),
('q-uuid', '30', false, 3),
('q-uuid', '15', false, 4);
```

---

### 10. GRID_PATTERN_BOOLEAN

Grid displayed with a True/False question about a property of the grid.

**Database fields:**
- `question_type`: `'grid_pattern_boolean'`
- `prompt_config`: **REQUIRED** - JSON with `grid` key
- `metadata`: **REQUIRED** - `{"property": "...", "axis": "..."}`

**Options:** Exactly 2 options: "True" and "False".

**Metadata `property` values:** `symmetry`, `repetition`, `checkerboard`
**Metadata `axis` values:** `vertical`, `horizontal`, `diagonal`, `both`, `rows`

```sql
INSERT INTO questions (quiz_id, question_type, title, explanation,
  prompt_config, metadata) VALUES
('quiz-uuid', 'grid_pattern_boolean',
 'Does this grid have vertical symmetry?',
 'The grid is symmetric along the vertical axis.',
 '{"grid": [["A","B","A"],["C","D","C"],["E","F","E"]]}',
 '{"property": "symmetry", "axis": "vertical"}');

INSERT INTO question_options (question_id, label, is_correct, sort_order) VALUES
('q-uuid', 'True', true, 1),
('q-uuid', 'False', false, 2);
```

The app displays the grid with a property badge (e.g., "Symmetry: vertical") below it.

---

### 11. STATEMENT_REASON

Two statements presented (Statement A and Reason R), user evaluates their relationship.

**Database fields:**
- `question_type`: `'statement_reason'`
- `prompt_config`: Not required
- `metadata`: **Recommended** - `{"statement": "...", "reason": "..."}`

**Options:** MCQ-style options (typically 4):

```sql
INSERT INTO questions (quiz_id, question_type, title, explanation,
  metadata) VALUES
('quiz-uuid', 'statement_reason',
 'Statement: Water boils at 100C at sea level.\nReason: Atmospheric pressure affects boiling point.',
 'Both the statement and reason are correct, and the reason explains the statement.',
 '{"statement": "Water boils at 100C at sea level.", "reason": "Atmospheric pressure affects boiling point."}');

INSERT INTO question_options (question_id, label, is_correct, sort_order) VALUES
('q-uuid', 'Both A and R are correct, and R explains A', true, 1),
('q-uuid', 'Both A and R are correct, but R does not explain A', false, 2),
('q-uuid', 'A is correct, but R is incorrect', false, 3),
('q-uuid', 'A is incorrect, but R is correct', false, 4);
```

**Important:** If `metadata.statement` and `metadata.reason` are provided, the app uses them. Otherwise, it parses from `title` (split by `\n` or `. Reason:`). **Always use metadata** for reliable display.

---

### 12. TABLE_DATA

Table/data presented with an MCQ question about the data.

**Database fields:**
- `question_type`: `'table_data'`
- `prompt_config`: **REQUIRED** - 2D array representing the table
- `metadata`: Optional

**Options:** MCQ-style options.

```sql
INSERT INTO questions (quiz_id, question_type, title, explanation,
  prompt_config) VALUES
('quiz-uuid', 'table_data',
 'Based on the data, which student scored the highest?',
 'Alice scored 95, the highest score.',
 '{"rows": [["Name","Math","Science"],["Alice","95","88"],["Bob","82","91"],["Carol","78","85"]]}');

INSERT INTO question_options (question_id, label, is_correct, sort_order) VALUES
('q-uuid', 'Alice', true, 1),
('q-uuid', 'Bob', false, 2),
('q-uuid', 'Carol', false, 3);
```

---

### 13. MEMORY

Grid shown briefly, then hidden. User answers MCQ based on what they remember.

**Database fields:**
- `question_type`: `'memory'`
- `prompt_config`: **REQUIRED** - `{"grid": [[...]], "reveal_duration": 2000}`
- `metadata`: Optional

**Options:** MCQ-style options.

```sql
INSERT INTO questions (quiz_id, question_type, title, explanation,
  prompt_config) VALUES
('quiz-uuid', 'memory',
 'Memorize the grid, then answer: What was in the center?',
 'The center cell contained a star.',
 '{"grid": [["🔴","🟢","🔵"],["🟡","⭐","🟣"],["🟠","⚪","🟤"]], "reveal_duration": 3000}');

INSERT INTO question_options (question_id, label, is_correct, sort_order) VALUES
('q-uuid', '⭐ Star', true, 1),
('q-uuid', '🟡 Yellow', false, 2),
('q-uuid', '🟣 Purple', false, 3),
('q-uuid', '🔴 Red', false, 4);
```

**Visual Token Rendering:**

Memory grid cells and options can use **visual token prefixes** for richer display. Instead of plain text, use:
- `shape:circle`, `shape:star`, `shape:heart`, `shape:diamond`, etc. → renders as Canvas-drawn colored shapes
- `color:red`, `color:blue`, `color:green`, etc. → renders as filled color swatches
- `shape:heart color:red` → renders as a red heart (combo token)

The app's `VisualTokenFromText` composable automatically detects these prefixes and renders visual elements instead of plain text. Plain text/emoji values are still supported as fallbacks.

---

### 14. VISUAL_SINGLE_CHOICE

MCQ with visual elements (images, icons, emojis) on each option.

**Database fields:**
- `question_type`: `'visual_single_choice'`
- `prompt_config`: Not required
- `metadata`: Not required

**Options:** Use `media_url` for images or `visual_label` for emoji/icon display.

```sql
INSERT INTO questions (quiz_id, question_type, title, explanation) VALUES
('quiz-uuid', 'visual_single_choice', 'Which flag belongs to Japan?', 'Japan flag is white with a red circle.');

INSERT INTO question_options (question_id, label, is_correct, sort_order, visual_label) VALUES
('q-uuid', 'Japan', true, 1, '🇯🇵'),
('q-uuid', 'China', false, 2, '🇨🇳'),
('q-uuid', 'South Korea', false, 3, '🇰🇷'),
('q-uuid', 'India', false, 4, '🇮🇳');
```

For image-based options, use `media_url` instead of `visual_label`:
```sql
INSERT INTO question_options (question_id, label, is_correct, sort_order, media_url) VALUES
('q-uuid', 'Cat', true, 1, 'https://cdn.example.com/cat.png'),
('q-uuid', 'Dog', false, 2, 'https://cdn.example.com/dog.png');
```

---

### 15. SEQUENCE_TAP

Two-phase memory game: memorize a sequence, then tap items in the correct order.

**Database fields:**
- `question_type`: `'sequence_tap'`
- `prompt_config`: **REQUIRED** - `{"show_duration_ms": 3000}`
- `metadata`: Optional (e.g., `{"type": "fruits"}`)

**Options:** ALL options have `is_correct = true`. Use `correct_position` for sequence order. Use `visual_label` for display.

```sql
INSERT INTO questions (quiz_id, question_type, title, explanation,
  prompt_config) VALUES
('quiz-uuid', 'sequence_tap',
 'Memorize the sequence, then tap in the correct order.',
 'The correct order is: Apple, Banana, Cherry, Date.',
 '{"show_duration_ms": 3000}');

INSERT INTO question_options (question_id, label, is_correct, sort_order, correct_position, visual_label) VALUES
('q-uuid', 'Apple', true, 1, 1, '🍎'),
('q-uuid', 'Banana', true, 2, 2, '🍌'),
('q-uuid', 'Cherry', true, 3, 3, '🍒'),
('q-uuid', 'Date', true, 4, 4, '🌴');
```

**How it works in the app:**
1. **Memorize Phase** (duration from `show_duration_ms`): Shows items in `correct_position` order with numbered badges and a countdown bar
2. **Recall Phase**: Items are shuffled. User taps them in the memorized order. Can undo by tapping again.

**Important:**
- ALL options must have `is_correct = true`
- `correct_position` determines the sequence order (1, 2, 3, ...)
- `visual_label` is shown in the UI (emoji, symbol, or short text)
- `show_duration_ms` controls how long the sequence is shown (default: 3000ms)

---

## Module Quiz vs IQ Quiz

| Feature | Module Quiz | IQ Quiz |
|---------|------------|---------|
| **quiz_type** | `'practice'` | `'iq'` |
| **Feedback** | Shows green/red + explanation | No feedback shown |
| **Cooldown** | None | 168 hours (7 days) |
| **XP** | Full XP on first attempt, 0 on replay | Full XP on first attempt, 0 on replay |
| **IQ Score** | Not tracked | Updates `iq_best_score` on users table |
| **Auto-advance** | User clicks "Next" | Auto-advances after 300ms |
| **Review** | Can review answers after | No review |
| **Retry** | Can retry immediately | Must wait 7 days |
| **Nudges** | Encouragement messages shown | No nudges |

**Data structure is identical** - both use the same `questions`, `question_options`, and `match_pairs` tables with the same schema. The behavioral differences are controlled entirely by `quiz_type` in the `quizzes` table and `QuizConfig` in the app.

---

## Step-by-Step Guides

### How to Add a New Module

1. Insert into `modules`:
   ```sql
   INSERT INTO modules (title, subtitle, emoji, accent_color, grade_id, display_order, status)
   VALUES ('Science', 'Physical Sciences', '🔬', '#10B981', 'grade-uuid', 2, 'draft');
   ```

2. Add chapters to the module
3. Add quizzes to each chapter
4. Add questions to each quiz
5. Publish: change status to `'published'` for module, chapters, quizzes, and questions

### How to Add a New Quiz

1. Get the `chapter_id` for the target chapter
2. Insert into `quizzes`:
   ```sql
   INSERT INTO quizzes (chapter_id, title, quiz_type, question_count, time_limit_secs, max_xp, difficulty, status)
   VALUES ('chapter-uuid', 'Forces Quiz', 'practice', 10, 300, 100, 'medium', 'draft');
   ```
3. Add questions (see question type examples above)
4. Update `question_count` to match actual number of questions
5. Publish when ready

### How to Add a New Question

1. Get the `quiz_id`
2. Choose the `question_type` from the 15 supported types
3. Insert the question with appropriate `prompt_config` and `metadata`
4. Insert options into `question_options` (or `match_pairs` for match type)
5. Set `sort_order` to control question display order

### How to Create an IQ Quiz

1. Create a chapter specifically for IQ tests (e.g., under a "Mental Aptitude" module)
2. Create quiz with `quiz_type = 'iq'` and `cooldown_hours = 168`
3. Add diverse question types (matrix, grid_pattern, sequence_tap, ordering, etc.)
4. These questions test cognitive ability rather than subject knowledge
5. The app will automatically handle no-feedback mode and cooldown enforcement

---

## Important Rules & Constraints

### Required Fields

| Table | Required | Notes |
|-------|----------|-------|
| questions | quiz_id, question_type, title | explanation defaults to '' |
| question_options | question_id, label | is_correct defaults to false |
| match_pairs | question_id, left_text, right_text | |

### Status Workflow

All content must follow: `draft → review → published → archived`

Content in `draft` or `review` status is **invisible** to app users.

### sort_order Conventions

- Start from 1 and increment by 1
- No gaps (1, 2, 3, ... not 1, 5, 10)
- Controls display order in the app

### UUID Generation

All `id` columns use `uuid_generate_v4()` by default. You can:
- Let Supabase auto-generate them (recommended)
- Provide your own UUIDs (useful for seed data and cross-references)

### Question Type CHECK Constraint

The `question_type` column only accepts these 15 values:
```
multiple_choice, true_false, ordering, match, fill_blank,
select_word, matrix, grid_pattern, statement_reason,
table_data, memory, visual_single_choice,
grid_cell_select, grid_pattern_boolean, sequence_tap
```

Any other value will be rejected by the database.

---

## Common Pitfalls

### 1. Missing prompt_config Causes Fallback UI

For `matrix`, `grid_pattern`, `grid_cell_select`, `grid_pattern_boolean`, `table_data`, `memory`, and `sequence_tap`:
- If `prompt_config` is NULL or missing the expected key (`grid`, `rows`, `show_duration_ms`), the app shows a blank area above the options
- **Always verify** your prompt_config JSON is valid

### 2. Wrong question_type String

The CHECK constraint is **case-sensitive**. Must be lowercase:
- `'multiple_choice'` (correct)
- `'Multiple_Choice'` (rejected)
- `'MULTIPLE_CHOICE'` (rejected)

### 3. Options Without is_correct Flag

If no option has `is_correct = true`, the question has no correct answer and will always be scored as incorrect.

### 4. ORDERING Without correct_position

If `correct_position` is NULL on ordering options, the app falls back to `display_order`. Always set `correct_position` explicitly for ordering questions.

### 5. MATCH Without match_pairs Data

Match questions use `match_pairs` table, NOT `question_options`. If you accidentally add options instead of match pairs, the UI will show an empty match screen.

### 6. SEQUENCE_TAP Options Not All Marked Correct

All options in a sequence_tap question must have `is_correct = true`. Unlike MCQ where only one is correct, sequence_tap uses ALL items in the sequence.

### 7. GRID_PATTERN_BOOLEAN Missing Metadata

This type requires `metadata` with `property` and `axis` keys. Without it, the property badge won't display.

### 8. STATEMENT_REASON Without Metadata

While the app can parse statement/reason from the `title` field (split by `\n`), this is fragile. Always use `metadata: {"statement": "...", "reason": "..."}` for reliable display.

### 9. Forgetting to Update question_count

The `question_count` on the `quizzes` table is NOT auto-calculated. After adding/removing questions, update it manually:
```sql
UPDATE quizzes SET question_count = (
    SELECT COUNT(*) FROM questions WHERE quiz_id = 'quiz-uuid' AND status = 'published'
) WHERE id = 'quiz-uuid';
```

### 10. Publishing Child Without Publishing Parent

A published quiz inside a draft chapter is effectively invisible. Ensure the entire hierarchy is published:
Module (published) → Chapter (published) → Quiz (published) → Questions (published)
