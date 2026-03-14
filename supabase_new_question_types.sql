-- ============================================================
-- NEW QUESTION TYPES: grid_cell_select, grid_pattern_boolean, sequence_tap
-- Run this in Supabase SQL Editor
-- ============================================================

-- ── 1. Update CHECK constraint to allow 15 question types ───

ALTER TABLE public.questions
  DROP CONSTRAINT IF EXISTS questions_question_type_check;

ALTER TABLE public.questions
  ADD CONSTRAINT questions_question_type_check
  CHECK (question_type IN (
    'multiple_choice','true_false','ordering','match',
    'fill_blank','select_word','matrix','grid_pattern',
    'statement_reason','table_data','memory','visual_single_choice',
    'grid_cell_select','grid_pattern_boolean','sequence_tap'
  ));


-- ============================================================
-- 2. GRID_CELL_SELECT — 5 questions
-- ============================================================

-- Q61: Row sum grid
INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, prompt_config, sort_order, explanation)
VALUES (
  'c1000000-0000-0000-0000-000000000061',
  'c1000000-0000-0000-0000-000000000099',
  'grid_cell_select',
  'Which number completes the grid so each row sums to 60?',
  'Find the missing value',
  '{"grid":[["15","20","25"],["10","30","?"],["25","15","20"]]}',
  61,
  'Row 2 needs 60 - 10 - 30 = 20'
);
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
  ('c2000000-0000-0000-0061-000000000001', 'c1000000-0000-0000-0000-000000000061', '20', true, 1),
  ('c2000000-0000-0000-0061-000000000002', 'c1000000-0000-0000-0000-000000000061', '15', false, 2),
  ('c2000000-0000-0000-0061-000000000003', 'c1000000-0000-0000-0000-000000000061', '25', false, 3),
  ('c2000000-0000-0000-0061-000000000004', 'c1000000-0000-0000-0000-000000000061', '10', false, 4);

-- Q62: Column product grid
INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, prompt_config, sort_order, explanation)
VALUES (
  'c1000000-0000-0000-0000-000000000062',
  'c1000000-0000-0000-0000-000000000099',
  'grid_cell_select',
  'Each column multiplies to 24. What is the missing number?',
  'Find the missing value',
  '{"grid":[["2","3","4"],["3","?","2"],["4","2","3"]]}',
  62,
  'Column 2: 3 x ? x 2 = 24, so ? = 4'
);
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
  ('c2000000-0000-0000-0062-000000000001', 'c1000000-0000-0000-0000-000000000062', '4', true, 1),
  ('c2000000-0000-0000-0062-000000000002', 'c1000000-0000-0000-0000-000000000062', '6', false, 2),
  ('c2000000-0000-0000-0062-000000000003', 'c1000000-0000-0000-0000-000000000062', '3', false, 3),
  ('c2000000-0000-0000-0062-000000000004', 'c1000000-0000-0000-0000-000000000062', '8', false, 4);

-- Q63: Diagonal sum
INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, prompt_config, sort_order, explanation)
VALUES (
  'c1000000-0000-0000-0000-000000000063',
  'c1000000-0000-0000-0000-000000000099',
  'grid_cell_select',
  'Both diagonals sum to 15. What value goes in the center?',
  'Find the center value',
  '{"grid":[["2","7","6"],["9","?","1"],["4","3","8"]]}',
  63,
  'Diagonal: 2 + ? + 8 = 15, so ? = 5'
);
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
  ('c2000000-0000-0000-0063-000000000001', 'c1000000-0000-0000-0000-000000000063', '5', true, 1),
  ('c2000000-0000-0000-0063-000000000002', 'c1000000-0000-0000-0000-000000000063', '4', false, 2),
  ('c2000000-0000-0000-0063-000000000003', 'c1000000-0000-0000-0000-000000000063', '6', false, 3),
  ('c2000000-0000-0000-0063-000000000004', 'c1000000-0000-0000-0000-000000000063', '7', false, 4);

-- Q64: Fibonacci grid
INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, prompt_config, sort_order, explanation)
VALUES (
  'c1000000-0000-0000-0000-000000000064',
  'c1000000-0000-0000-0000-000000000099',
  'grid_cell_select',
  'Each cell equals the sum of the two cells above it. Find the missing value.',
  'Follow the pattern',
  '{"grid":[["1","1","1","1"],["1","2","2","1"],["1","3","?","1"]]}',
  64,
  'Third row: 2 + 2 = 4, but the pattern gives 3. Actually 1+2=3, 2+2=4. The cell is the sum of the two above: 2+2=4.'
);
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
  ('c2000000-0000-0000-0064-000000000001', 'c1000000-0000-0000-0000-000000000064', '4', true, 1),
  ('c2000000-0000-0000-0064-000000000002', 'c1000000-0000-0000-0000-000000000064', '3', false, 2),
  ('c2000000-0000-0000-0064-000000000003', 'c1000000-0000-0000-0000-000000000064', '5', false, 3),
  ('c2000000-0000-0000-0064-000000000004', 'c1000000-0000-0000-0000-000000000064', '2', false, 4);

-- Q65: Alternating pattern grid
INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, prompt_config, sort_order, explanation)
VALUES (
  'c1000000-0000-0000-0000-000000000065',
  'c1000000-0000-0000-0000-000000000099',
  'grid_cell_select',
  'Each row follows +3 pattern. What is the missing number?',
  'Complete the sequence',
  '{"grid":[["1","4","7","10"],["2","5","8","11"],["3","6","?","12"]]}',
  65,
  'Row 3: 3, 6, ?, 12. Pattern is +3, so ? = 9'
);
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
  ('c2000000-0000-0000-0065-000000000001', 'c1000000-0000-0000-0000-000000000065', '9', true, 1),
  ('c2000000-0000-0000-0065-000000000002', 'c1000000-0000-0000-0000-000000000065', '8', false, 2),
  ('c2000000-0000-0000-0065-000000000003', 'c1000000-0000-0000-0000-000000000065', '10', false, 3),
  ('c2000000-0000-0000-0065-000000000004', 'c1000000-0000-0000-0000-000000000065', '7', false, 4);


-- ============================================================
-- 3. GRID_PATTERN_BOOLEAN — 5 questions
-- ============================================================

-- Q66: Vertical symmetry check (symmetric)
INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, prompt_config, metadata, sort_order, explanation)
VALUES (
  'c1000000-0000-0000-0000-000000000066',
  'c1000000-0000-0000-0000-000000000099',
  'grid_pattern_boolean',
  'Is this pattern symmetric along the vertical axis?',
  'Check symmetry',
  '{"grid":[["red","blue","red"],["blue","red","blue"],["red","blue","red"]]}',
  '{"property":"symmetry","axis":"vertical"}',
  66,
  'Each row reads the same forwards and backwards, so it is vertically symmetric.'
);
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
  ('c2000000-0000-0000-0066-000000000001', 'c1000000-0000-0000-0000-000000000066', 'True', true, 1),
  ('c2000000-0000-0000-0066-000000000002', 'c1000000-0000-0000-0000-000000000066', 'False', false, 2);

-- Q67: Horizontal symmetry check (not symmetric)
INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, prompt_config, metadata, sort_order, explanation)
VALUES (
  'c1000000-0000-0000-0000-000000000067',
  'c1000000-0000-0000-0000-000000000099',
  'grid_pattern_boolean',
  'Is this pattern symmetric along the horizontal axis?',
  'Check symmetry',
  '{"grid":[["star","star","star"],["circle","circle","circle"],["triangle","triangle","star"]]}',
  '{"property":"symmetry","axis":"horizontal"}',
  67,
  'Row 1 and Row 3 are different (star vs triangle in last cell), so not horizontally symmetric.'
);
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
  ('c2000000-0000-0000-0067-000000000001', 'c1000000-0000-0000-0000-000000000067', 'True', false, 1),
  ('c2000000-0000-0000-0067-000000000002', 'c1000000-0000-0000-0000-000000000067', 'False', true, 2);

-- Q68: Repeating pattern check
INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, prompt_config, metadata, sort_order, explanation)
VALUES (
  'c1000000-0000-0000-0000-000000000068',
  'c1000000-0000-0000-0000-000000000099',
  'grid_pattern_boolean',
  'Does every row in this grid follow the same repeating pattern?',
  'Check pattern repetition',
  '{"grid":[["A","B","A","B"],["A","B","A","B"],["A","B","A","B"]]}',
  '{"property":"repetition","axis":"rows"}',
  68,
  'All rows follow A-B-A-B pattern, so yes it repeats.'
);
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
  ('c2000000-0000-0000-0068-000000000001', 'c1000000-0000-0000-0000-000000000068', 'True', true, 1),
  ('c2000000-0000-0000-0068-000000000002', 'c1000000-0000-0000-0000-000000000068', 'False', false, 2);

-- Q69: Diagonal symmetry
INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, prompt_config, metadata, sort_order, explanation)
VALUES (
  'c1000000-0000-0000-0000-000000000069',
  'c1000000-0000-0000-0000-000000000099',
  'grid_pattern_boolean',
  'Is this grid symmetric along the main diagonal (top-left to bottom-right)?',
  'Check diagonal symmetry',
  '{"grid":[["1","2","3"],["2","4","5"],["3","5","6"]]}',
  '{"property":"symmetry","axis":"diagonal"}',
  69,
  'Element (i,j) equals element (j,i) for all positions, so it is diagonally symmetric.'
);
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
  ('c2000000-0000-0000-0069-000000000001', 'c1000000-0000-0000-0000-000000000069', 'True', true, 1),
  ('c2000000-0000-0000-0069-000000000002', 'c1000000-0000-0000-0000-000000000069', 'False', false, 2);

-- Q70: Checkerboard pattern check
INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, prompt_config, metadata, sort_order, explanation)
VALUES (
  'c1000000-0000-0000-0000-000000000070',
  'c1000000-0000-0000-0000-000000000099',
  'grid_pattern_boolean',
  'Does this grid form a perfect checkerboard pattern?',
  'Check checkerboard',
  '{"grid":[["black","white","black","white"],["white","black","white","black"],["black","white","black","black"]]}',
  '{"property":"checkerboard","axis":"both"}',
  70,
  'The last cell in the bottom row breaks the alternating pattern, so it is not a perfect checkerboard.'
);
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order) VALUES
  ('c2000000-0000-0000-0070-000000000001', 'c1000000-0000-0000-0000-000000000070', 'True', false, 1),
  ('c2000000-0000-0000-0070-000000000002', 'c1000000-0000-0000-0000-000000000070', 'False', true, 2);


-- ============================================================
-- 4. SEQUENCE_TAP — 5 questions
-- ============================================================

-- Q71: Fruit sequence
INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, prompt_config, sort_order, explanation)
VALUES (
  'c1000000-0000-0000-0000-000000000071',
  'c1000000-0000-0000-0000-000000000099',
  'sequence_tap',
  'Memorize the sequence, then tap in the same order',
  'Watch carefully!',
  '{"show_duration_ms":3000}',
  71,
  'The correct order was: apple, star, target, cat'
);
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order, correct_position, visual_label) VALUES
  ('c2000000-0000-0000-0071-000000000001', 'c1000000-0000-0000-0000-000000000071', 'Apple', true, 1, 1, 'apple'),
  ('c2000000-0000-0000-0071-000000000002', 'c1000000-0000-0000-0000-000000000071', 'Star', true, 2, 2, 'star'),
  ('c2000000-0000-0000-0071-000000000003', 'c1000000-0000-0000-0000-000000000071', 'Target', true, 3, 3, 'target'),
  ('c2000000-0000-0000-0071-000000000004', 'c1000000-0000-0000-0000-000000000071', 'Cat', true, 4, 4, 'cat');

-- Q72: Number sequence
INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, prompt_config, sort_order, explanation)
VALUES (
  'c1000000-0000-0000-0000-000000000072',
  'c1000000-0000-0000-0000-000000000099',
  'sequence_tap',
  'Remember the order of these numbers, then tap them back',
  'Focus and remember!',
  '{"show_duration_ms":4000}',
  72,
  'The correct order was: 7, 3, 9, 1, 5'
);
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order, correct_position, visual_label) VALUES
  ('c2000000-0000-0000-0072-000000000001', 'c1000000-0000-0000-0000-000000000072', 'Seven', true, 1, 1, '7'),
  ('c2000000-0000-0000-0072-000000000002', 'c1000000-0000-0000-0000-000000000072', 'Three', true, 2, 2, '3'),
  ('c2000000-0000-0000-0072-000000000003', 'c1000000-0000-0000-0000-000000000072', 'Nine', true, 3, 3, '9'),
  ('c2000000-0000-0000-0072-000000000004', 'c1000000-0000-0000-0000-000000000072', 'One', true, 4, 4, '1'),
  ('c2000000-0000-0000-0072-000000000005', 'c1000000-0000-0000-0000-000000000072', 'Five', true, 5, 5, '5');

-- Q73: Color sequence
INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, prompt_config, sort_order, explanation)
VALUES (
  'c1000000-0000-0000-0000-000000000073',
  'c1000000-0000-0000-0000-000000000099',
  'sequence_tap',
  'Watch the color sequence, then recreate it',
  'Remember the colors!',
  '{"show_duration_ms":3500}',
  73,
  'The correct order was: Red, Blue, Green, Yellow'
);
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order, correct_position, visual_label) VALUES
  ('c2000000-0000-0000-0073-000000000001', 'c1000000-0000-0000-0000-000000000073', 'Red', true, 1, 1, 'color:red'),
  ('c2000000-0000-0000-0073-000000000002', 'c1000000-0000-0000-0000-000000000073', 'Blue', true, 2, 2, 'color:blue'),
  ('c2000000-0000-0000-0073-000000000003', 'c1000000-0000-0000-0000-000000000073', 'Green', true, 3, 3, 'color:green'),
  ('c2000000-0000-0000-0073-000000000004', 'c1000000-0000-0000-0000-000000000073', 'Yellow', true, 4, 4, 'color:yellow');

-- Q74: Shape sequence
INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, prompt_config, sort_order, explanation)
VALUES (
  'c1000000-0000-0000-0000-000000000074',
  'c1000000-0000-0000-0000-000000000099',
  'sequence_tap',
  'Memorize the order of shapes, then tap them correctly',
  'Study the pattern!',
  '{"show_duration_ms":3000}',
  74,
  'The correct order was: Triangle, Circle, Square, Diamond, Star'
);
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order, correct_position, visual_label) VALUES
  ('c2000000-0000-0000-0074-000000000001', 'c1000000-0000-0000-0000-000000000074', 'Triangle', true, 1, 1, 'shape:triangle'),
  ('c2000000-0000-0000-0074-000000000002', 'c1000000-0000-0000-0000-000000000074', 'Circle', true, 2, 2, 'shape:circle'),
  ('c2000000-0000-0000-0074-000000000003', 'c1000000-0000-0000-0000-000000000074', 'Square', true, 3, 3, 'shape:square'),
  ('c2000000-0000-0000-0074-000000000004', 'c1000000-0000-0000-0000-000000000074', 'Diamond', true, 4, 4, 'shape:diamond'),
  ('c2000000-0000-0000-0074-000000000005', 'c1000000-0000-0000-0000-000000000074', 'Star', true, 5, 5, 'shape:star');

-- Q75: Letter sequence
INSERT INTO public.questions (id, quiz_id, question_type, title, prompt, prompt_config, sort_order, explanation)
VALUES (
  'c1000000-0000-0000-0000-000000000075',
  'c1000000-0000-0000-0000-000000000099',
  'sequence_tap',
  'Remember and tap the letters in the shown order',
  'Quick memory test!',
  '{"show_duration_ms":2500}',
  75,
  'The correct order was: M, Q, Z, A, K, P'
);
INSERT INTO public.question_options (id, question_id, label, is_correct, sort_order, correct_position, visual_label) VALUES
  ('c2000000-0000-0000-0075-000000000001', 'c1000000-0000-0000-0000-000000000075', 'M', true, 1, 1, 'M'),
  ('c2000000-0000-0000-0075-000000000002', 'c1000000-0000-0000-0000-000000000075', 'Q', true, 2, 2, 'Q'),
  ('c2000000-0000-0000-0075-000000000003', 'c1000000-0000-0000-0000-000000000075', 'Z', true, 3, 3, 'Z'),
  ('c2000000-0000-0000-0075-000000000004', 'c1000000-0000-0000-0000-000000000075', 'A', true, 4, 4, 'A'),
  ('c2000000-0000-0000-0075-000000000005', 'c1000000-0000-0000-0000-000000000075', 'K', true, 5, 5, 'K'),
  ('c2000000-0000-0000-0075-000000000006', 'c1000000-0000-0000-0000-000000000075', 'P', true, 6, 6, 'P');
