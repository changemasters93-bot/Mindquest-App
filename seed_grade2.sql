-- ============================================================
-- SEED DATA: Grade 2 — 5 Modules, 6 Chapters each,
--            6 Quizzes per chapter, 2-3 Questions per quiz
-- Run this in the Supabase SQL Editor
-- ============================================================

-- ── Helper function (dropped at end) ────────────────────────
CREATE OR REPLACE FUNCTION _sq(
  p_quiz UUID, p_title TEXT, p_expl TEXT,
  p_o1 TEXT, p_o2 TEXT, p_o3 TEXT, p_o4 TEXT,
  p_correct INT, p_sort INT DEFAULT 1
) RETURNS VOID AS $fn$
DECLARE v UUID := gen_random_uuid();
BEGIN
  INSERT INTO questions (id,quiz_id,question_type,title,explanation,sort_order)
  VALUES (v, p_quiz,'multiple_choice',p_title,p_expl,p_sort);
  INSERT INTO question_options (id,question_id,label,is_correct,sort_order) VALUES
    (gen_random_uuid(),v,p_o1,(p_correct=1),1),
    (gen_random_uuid(),v,p_o2,(p_correct=2),2),
    (gen_random_uuid(),v,p_o3,(p_correct=3),3),
    (gen_random_uuid(),v,p_o4,(p_correct=4),4);
END; $fn$ LANGUAGE plpgsql;

-- ── Clean up any previous Grade 2 seed data ────────────────
DELETE FROM question_options WHERE question_id IN (
  SELECT q.id FROM questions q
  JOIN quizzes qz ON q.quiz_id = qz.id
  JOIN chapters ch ON qz.chapter_id = ch.id
  JOIN modules mo ON ch.module_id = mo.id
  WHERE mo.grade_id = 'e857cd23-06b1-410d-ba2e-3b8939ecd79b'
);
DELETE FROM questions WHERE quiz_id IN (
  SELECT qz.id FROM quizzes qz
  JOIN chapters ch ON qz.chapter_id = ch.id
  JOIN modules mo ON ch.module_id = mo.id
  WHERE mo.grade_id = 'e857cd23-06b1-410d-ba2e-3b8939ecd79b'
);
DELETE FROM quizzes WHERE chapter_id IN (
  SELECT ch.id FROM chapters ch
  JOIN modules mo ON ch.module_id = mo.id
  WHERE mo.grade_id = 'e857cd23-06b1-410d-ba2e-3b8939ecd79b'
);
DELETE FROM chapters WHERE module_id IN (
  SELECT id FROM modules WHERE grade_id = 'e857cd23-06b1-410d-ba2e-3b8939ecd79b'
);
DELETE FROM modules WHERE grade_id = 'e857cd23-06b1-410d-ba2e-3b8939ecd79b';

-- ── Main seed block ─────────────────────────────────────────
DO $$
DECLARE
  g  UUID := 'e857cd23-06b1-410d-ba2e-3b8939ecd79b'; -- Grade 2 (hardcoded)
  m  UUID; -- module
  c  UUID; -- chapter
  q  UUID; -- quiz
BEGIN

RAISE NOTICE 'Using grade_id: %', g;

-- ═══════════════════════════════════════════════════════════
-- MODULE 1: Mathematics 🔢
-- ═══════════════════════════════════════════════════════════
m := gen_random_uuid();
INSERT INTO modules (id,title,subtitle,emoji,accent_color,display_order,grade_id,is_active,status)
VALUES (m,'Mathematics','Numbers, shapes & fun','🔢','#4F46E5',1,g,true,'published');

-- ── Ch 1.1: Counting to 100 ────────────────────────────────
c := gen_random_uuid();
INSERT INTO chapters (id,module_id,title,chapter_number,sort_order) VALUES (c,m,'Counting to 100',1,1);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Count Forward',2,120,50,1);
PERFORM _sq(q,'What number comes after 7?','7 + 1 = 8','6','8','9','10',2,1);
PERFORM _sq(q,'What number comes after 19?','19 + 1 = 20','18','21','20','22',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Count Backward',2,120,50,2);
PERFORM _sq(q,'What number comes before 10?','10 - 1 = 9','8','11','9','7',3,1);
PERFORM _sq(q,'What number comes before 50?','50 - 1 = 49','48','51','49','47',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Skip Counting by 2',2,120,50,3);
PERFORM _sq(q,'Count by 2: 2, 4, 6, __?','Next even number is 8','7','8','9','10',2,1);
PERFORM _sq(q,'Which is an even number?','Even: 2,4,6,8,10...','3','5','8','7',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Skip Counting by 5',2,120,50,4);
PERFORM _sq(q,'Count by 5: 5, 10, 15, __?','Next is 20','18','25','20','22',3,1);
PERFORM _sq(q,'Count by 5: 25, 30, 35, __?','Next is 40','38','45','42','40',4,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Skip Counting by 10',2,120,50,5);
PERFORM _sq(q,'Count by 10: 10, 20, 30, __?','Next is 40','35','50','45','40',4,1);
PERFORM _sq(q,'Count by 10: 60, 70, 80, __?','Next is 90','85','100','90','95',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Number Order',3,180,75,6);
PERFORM _sq(q,'Which is the smallest: 45, 12, 67, 33?','12 is smallest','45','12','67','33',2,1);
PERFORM _sq(q,'Which is the biggest: 21, 89, 54, 76?','89 is biggest','21','89','54','76',2,2);
PERFORM _sq(q,'Put in order: 5, 2, 8. Which comes first?','2 is smallest','5','2','8','3',2,3);

-- ── Ch 1.2: Addition Fun ────────────────────────────────────
c := gen_random_uuid();
INSERT INTO chapters (id,module_id,title,chapter_number,sort_order) VALUES (c,m,'Addition Fun',2,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Add Single Digits',2,120,50,1);
PERFORM _sq(q,'What is 3 + 4?','3 + 4 = 7','5','6','7','8',3,1);
PERFORM _sq(q,'What is 6 + 2?','6 + 2 = 8','7','9','6','8',4,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Add to 10',2,120,50,2);
PERFORM _sq(q,'What is 5 + 5?','5 + 5 = 10','8','9','10','11',3,1);
PERFORM _sq(q,'What is 7 + 3?','7 + 3 = 10','9','10','11','8',2,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Add with Carry',2,120,50,3);
PERFORM _sq(q,'What is 8 + 5?','8 + 5 = 13','12','13','14','11',2,1);
PERFORM _sq(q,'What is 9 + 7?','9 + 7 = 16','15','17','16','14',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Double Numbers',2,120,50,4);
PERFORM _sq(q,'What is 6 + 6?','6 + 6 = 12','10','11','12','14',3,1);
PERFORM _sq(q,'What is 8 + 8?','8 + 8 = 16','14','15','16','18',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Add Two-Digit Numbers',2,120,50,5);
PERFORM _sq(q,'What is 12 + 5?','12 + 5 = 17','15','16','17','18',3,1);
PERFORM _sq(q,'What is 20 + 13?','20 + 13 = 33','31','32','33','35',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Addition Word Problems',3,180,75,6);
PERFORM _sq(q,'Sam has 4 apples. He gets 3 more. How many?','4 + 3 = 7','5','6','7','8',3,1);
PERFORM _sq(q,'There are 6 birds. 5 more come. How many?','6 + 5 = 11','10','11','12','9',2,2);
PERFORM _sq(q,'Mia has 8 crayons. She gets 4. Total?','8 + 4 = 12','10','11','12','13',3,3);

-- ── Ch 1.3: Subtraction Fun ─────────────────────────────────
c := gen_random_uuid();
INSERT INTO chapters (id,module_id,title,chapter_number,sort_order) VALUES (c,m,'Subtraction Fun',3,3);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Subtract Single Digits',2,120,50,1);
PERFORM _sq(q,'What is 9 - 3?','9 - 3 = 6','5','6','7','8',2,1);
PERFORM _sq(q,'What is 7 - 4?','7 - 4 = 3','2','4','3','5',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Subtract from 10',2,120,50,2);
PERFORM _sq(q,'What is 10 - 6?','10 - 6 = 4','3','5','4','6',3,1);
PERFORM _sq(q,'What is 10 - 2?','10 - 2 = 8','6','7','8','9',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Subtract Two-Digit',2,120,50,3);
PERFORM _sq(q,'What is 15 - 7?','15 - 7 = 8','6','7','8','9',3,1);
PERFORM _sq(q,'What is 18 - 9?','18 - 9 = 9','7','8','9','10',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Subtract from 20',2,120,50,4);
PERFORM _sq(q,'What is 20 - 5?','20 - 5 = 15','14','15','16','13',2,1);
PERFORM _sq(q,'What is 20 - 12?','20 - 12 = 8','6','7','8','9',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Find the Difference',2,120,50,5);
PERFORM _sq(q,'What is 14 - 6?','14 - 6 = 8','7','8','9','6',2,1);
PERFORM _sq(q,'What is 16 - 9?','16 - 9 = 7','5','6','7','8',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Subtraction Word Problems',3,180,75,6);
PERFORM _sq(q,'Tom has 10 sweets. He eats 4. How many left?','10 - 4 = 6','5','6','7','4',2,1);
PERFORM _sq(q,'A tree has 12 mangoes. 5 fall. How many on tree?','12 - 5 = 7','6','7','8','5',2,2);
PERFORM _sq(q,'15 balloons, 6 burst. How many left?','15 - 6 = 9','7','8','9','10',3,3);

-- ── Ch 1.4: Shapes Around Us ────────────────────────────────
c := gen_random_uuid();
INSERT INTO chapters (id,module_id,title,chapter_number,sort_order) VALUES (c,m,'Shapes Around Us',4,4);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Basic Shapes',2,120,50,1);
PERFORM _sq(q,'How many sides does a triangle have?','Tri means 3','2','3','4','5',2,1);
PERFORM _sq(q,'How many sides does a square have?','Square has 4 equal sides','3','4','5','6',2,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Circle & Rectangle',2,120,50,2);
PERFORM _sq(q,'A circle has how many corners?','A circle is round, no corners','0','1','2','4',1,1);
PERFORM _sq(q,'How many sides does a rectangle have?','Rectangle has 4 sides','3','4','5','6',2,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Shapes in Real Life',2,120,50,3);
PERFORM _sq(q,'A coin is shaped like a __?','Coins are round','Square','Triangle','Circle','Rectangle',3,1);
PERFORM _sq(q,'A door is shaped like a __?','Doors are rectangular','Circle','Triangle','Square','Rectangle',4,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Corners & Sides',2,120,50,4);
PERFORM _sq(q,'How many corners does a rectangle have?','Rectangle has 4 corners','2','3','4','5',3,1);
PERFORM _sq(q,'Which shape has 3 corners?','Triangle has 3 corners','Square','Circle','Triangle','Rectangle',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'3D Shapes',2,120,50,5);
PERFORM _sq(q,'A ball is shaped like a __?','Balls are spheres','Cube','Sphere','Cone','Cylinder',2,1);
PERFORM _sq(q,'A dice is shaped like a __?','Dice is a cube','Sphere','Cone','Cylinder','Cube',4,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Shape Patterns',3,180,75,6);
PERFORM _sq(q,'Which shape has all equal sides?','Square has 4 equal sides','Rectangle','Triangle','Square','Circle',3,1);
PERFORM _sq(q,'Which shape has no straight sides?','Circle is curved','Square','Triangle','Rectangle','Circle',4,2);
PERFORM _sq(q,'A pizza slice looks like a __?','Pizza slices are triangular','Square','Circle','Triangle','Rectangle',3,3);

-- ── Ch 1.5: Measurement ─────────────────────────────────────
c := gen_random_uuid();
INSERT INTO chapters (id,module_id,title,chapter_number,sort_order) VALUES (c,m,'Measurement',5,5);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Long & Short',2,120,50,1);
PERFORM _sq(q,'Which is longer: a pencil or a bus?','A bus is much longer','Pencil','Bus','Both same','Cannot tell',2,1);
PERFORM _sq(q,'Which is shorter: an ant or a cat?','An ant is tiny','Cat','Ant','Both same','Cannot tell',2,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Heavy & Light',2,120,50,2);
PERFORM _sq(q,'Which is heavier: a feather or a rock?','Rocks are heavy','Feather','Rock','Both same','Cannot tell',2,1);
PERFORM _sq(q,'Which is lighter: a balloon or a book?','Balloons are very light','Book','Balloon','Both same','Cannot tell',2,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Tall & Short',2,120,50,3);
PERFORM _sq(q,'Which is taller: a tree or a flower?','Trees grow much taller','Flower','Tree','Both same','Cannot tell',2,1);
PERFORM _sq(q,'Which is taller: a giraffe or a dog?','Giraffes are the tallest animal','Dog','Giraffe','Both same','Cannot tell',2,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Measuring Length',2,120,50,4);
PERFORM _sq(q,'We measure length using a __?','Rulers measure length','Clock','Ruler','Weighing scale','Thermometer',2,1);
PERFORM _sq(q,'How many centimeters in 1 meter?','1 m = 100 cm','10','50','100','1000',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Capacity',2,120,50,5);
PERFORM _sq(q,'Which holds more water: a cup or a bucket?','Buckets are bigger','Cup','Bucket','Both same','Glass',2,1);
PERFORM _sq(q,'We measure liquids in __?','Litres measure liquids','Metres','Kilograms','Litres','Seconds',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Measurement Review',3,180,75,6);
PERFORM _sq(q,'We use a weighing scale to measure __?','Scales measure weight','Length','Weight','Time','Speed',2,1);
PERFORM _sq(q,'Which unit is for weight?','Kilograms measure weight','Metre','Litre','Kilogram','Second',3,2);
PERFORM _sq(q,'A thermometer measures __?','Thermometers measure temperature','Weight','Height','Speed','Temperature',4,3);

-- ── Ch 1.6: Telling Time ────────────────────────────────────
c := gen_random_uuid();
INSERT INTO chapters (id,module_id,title,chapter_number,sort_order) VALUES (c,m,'Telling Time',6,6);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Hours on a Clock',2,120,50,1);
PERFORM _sq(q,'How many numbers on a clock face?','Clocks show 1-12','6','10','12','24',3,1);
PERFORM _sq(q,'The short hand shows __?','Short hand = hours','Minutes','Seconds','Hours','Days',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'O''Clock Time',2,120,50,2);
PERFORM _sq(q,'When both hands point to 12, it is __?','12 o''clock','6 o''clock','3 o''clock','12 o''clock','9 o''clock',3,1);
PERFORM _sq(q,'How many hours in a day?','A day has 24 hours','12','24','30','60',2,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Half Past',2,120,50,3);
PERFORM _sq(q,'Half past 3 means the minute hand is on __?','Half past = minute hand on 6','12','3','6','9',3,1);
PERFORM _sq(q,'How many minutes in half an hour?','Half of 60 = 30','15','20','30','45',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Minutes',2,120,50,4);
PERFORM _sq(q,'How many minutes in 1 hour?','1 hour = 60 minutes','30','45','60','100',3,1);
PERFORM _sq(q,'The long hand shows __?','Long hand = minutes','Hours','Minutes','Seconds','Days',2,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Days of the Week',2,120,50,5);
PERFORM _sq(q,'How many days in a week?','A week has 7 days','5','6','7','10',3,1);
PERFORM _sq(q,'Which day comes after Monday?','Monday → Tuesday','Sunday','Wednesday','Tuesday','Friday',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Months & Calendar',3,180,75,6);
PERFORM _sq(q,'How many months in a year?','A year has 12 months','6','10','12','24',3,1);
PERFORM _sq(q,'Which month comes first?','January is month 1','February','March','January','April',3,2);
PERFORM _sq(q,'How many days in February (usually)?','February usually has 28 days','28','29','30','31',1,3);

-- ═══════════════════════════════════════════════════════════
-- MODULE 2: English 📖
-- ═══════════════════════════════════════════════════════════
m := gen_random_uuid();
INSERT INTO modules (id,title,subtitle,emoji,accent_color,display_order,grade_id,is_active,status)
VALUES (m,'English','Words, sentences & stories','📖','#059669',2,g,true,'published');

-- ── Ch 2.1: Vowels & Consonants ─────────────────────────────
c := gen_random_uuid();
INSERT INTO chapters (id,module_id,title,chapter_number,sort_order) VALUES (c,m,'Vowels & Consonants',1,1);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Know Your Vowels',2,120,50,1);
PERFORM _sq(q,'Which of these is a vowel?','A E I O U are vowels','B','E','D','G',2,1);
PERFORM _sq(q,'How many vowels are there in English?','A E I O U = 5','3','4','5','6',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Spot the Consonant',2,120,50,2);
PERFORM _sq(q,'Which is a consonant?','B is not a vowel','A','E','I','B',4,1);
PERFORM _sq(q,'How many consonants in English?','26 - 5 vowels = 21','15','20','21','24',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Vowel in Words',2,120,50,3);
PERFORM _sq(q,'Which word starts with a vowel?','Apple starts with A','Ball','Cat','Apple','Dog',3,1);
PERFORM _sq(q,'Find the vowel in "Sun"','U is the vowel in Sun','S','U','N','None',2,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Vowel or Consonant?',2,120,50,4);
PERFORM _sq(q,'Is "O" a vowel or consonant?','O is a vowel','Consonant','Vowel','Neither','Both',2,1);
PERFORM _sq(q,'Is "T" a vowel or consonant?','T is a consonant','Vowel','Consonant','Neither','Both',2,2);

-- ── Ch 2.2: Simple Sentences ────────────────────────────────
c := gen_random_uuid();
INSERT INTO chapters (id,module_id,title,chapter_number,sort_order) VALUES (c,m,'Simple Sentences',2,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Sentence Basics',2,120,50,1);
PERFORM _sq(q,'A sentence always starts with a __?','Sentences begin with capital letters','small letter','Capital letter','number','symbol',2,1);
PERFORM _sq(q,'A sentence ends with a __?','Sentences end with a full stop','comma','question mark','full stop','colon',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Fix the Sentence',2,120,50,2);
PERFORM _sq(q,'Which is a correct sentence?','Proper sentence has capital + full stop','the cat sat','The cat sat.','cat the sat.','THE CAT SAT',2,1);
PERFORM _sq(q,'Pick the question sentence.','Questions end with ?','I like dogs.','Dogs are nice.','Do you like dogs?','Dogs bark.',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Complete the Sentence',2,120,50,3);
PERFORM _sq(q,'The bird can __.','Birds fly','swim','fly','drive','cook',2,1);
PERFORM _sq(q,'The sun is __.','The sun gives brightness','cold','dark','bright','wet',3,2);

-- ── Ch 2.3: Nouns & Verbs ───────────────────────────────────
c := gen_random_uuid();
INSERT INTO chapters (id,module_id,title,chapter_number,sort_order) VALUES (c,m,'Nouns & Verbs',3,3);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'What is a Noun?',2,120,50,1);
PERFORM _sq(q,'Which of these is a noun?','Cat is a naming word (noun)','Run','Big','Cat','Quickly',3,1);
PERFORM _sq(q,'A noun is a __ word.','Nouns name people, places, things','doing','naming','describing','joining',2,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'What is a Verb?',2,120,50,2);
PERFORM _sq(q,'Which of these is a verb?','Jump is an action word','Table','Happy','Jump','Blue',3,1);
PERFORM _sq(q,'A verb is a __ word.','Verbs show action','naming','action','describing','number',2,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Noun or Verb?',2,120,50,3);
PERFORM _sq(q,'"Book" is a __?','Book is a thing = noun','Verb','Noun','Adjective','Adverb',2,1);
PERFORM _sq(q,'"Eat" is a __?','Eat is an action = verb','Noun','Adjective','Verb','Adverb',3,2);

-- ═══════════════════════════════════════════════════════════
-- MODULE 3: Science 🔬
-- ═══════════════════════════════════════════════════════════
m := gen_random_uuid();
INSERT INTO modules (id,title,subtitle,emoji,accent_color,display_order,grade_id,is_active,status)
VALUES (m,'Science','Explore the world around you','🔬','#DC2626',3,g,true,'published');

-- ── Ch 3.1: Plants ──────────────────────────────────────────
c := gen_random_uuid();
INSERT INTO chapters (id,module_id,title,chapter_number,sort_order) VALUES (c,m,'Plants',1,1);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Parts of a Plant',2,120,50,1);
PERFORM _sq(q,'Which part of a plant is underground?','Roots grow underground','Leaf','Stem','Flower','Root',4,1);
PERFORM _sq(q,'Plants make food in their __?','Leaves make food using sunlight','Roots','Leaves','Stem','Flowers',2,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'What Plants Need',2,120,50,2);
PERFORM _sq(q,'Plants need __ to grow.','Plants need water, sunlight, air','Toys','Water','Books','Pencils',2,1);
PERFORM _sq(q,'Plants get energy from __?','Plants use sunlight for photosynthesis','Moon','Stars','Sunlight','Wind',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Seeds & Fruits',2,120,50,3);
PERFORM _sq(q,'A new plant grows from a __?','Seeds grow into new plants','Leaf','Flower','Seed','Stem',3,1);
PERFORM _sq(q,'Which of these is a fruit?','Mango is a fruit','Carrot','Potato','Mango','Onion',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Types of Plants',2,120,50,4);
PERFORM _sq(q,'A very big plant is called a __?','Big plants are trees','Herb','Shrub','Grass','Tree',4,1);
PERFORM _sq(q,'Which plant gives us flowers?','Rose is a flowering plant','Cactus','Rose','Grass','Fern',2,2);

-- ── Ch 3.2: Animals ─────────────────────────────────────────
c := gen_random_uuid();
INSERT INTO chapters (id,module_id,title,chapter_number,sort_order) VALUES (c,m,'Animals',2,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Land Animals',2,120,50,1);
PERFORM _sq(q,'Which animal lives on land?','Lions live on land','Fish','Whale','Lion','Shark',3,1);
PERFORM _sq(q,'Which animal has a trunk?','Elephants have trunks','Dog','Cat','Horse','Elephant',4,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Water Animals',2,120,50,2);
PERFORM _sq(q,'Which animal lives in water?','Fish live in water','Dog','Cat','Fish','Cow',3,1);
PERFORM _sq(q,'A frog can live on land and __?','Frogs are amphibians','Air','Fire','Water','Space',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Birds & Insects',2,120,50,3);
PERFORM _sq(q,'How many legs does a bird have?','Birds have 2 legs','1','2','4','6',2,1);
PERFORM _sq(q,'How many legs does an insect have?','Insects have 6 legs','2','4','6','8',3,2);

-- ── Ch 3.3: Our Body ────────────────────────────────────────
c := gen_random_uuid();
INSERT INTO chapters (id,module_id,title,chapter_number,sort_order) VALUES (c,m,'Our Body',3,3);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Sense Organs',2,120,50,1);
PERFORM _sq(q,'We see with our __?','Eyes help us see','Ears','Nose','Eyes','Hands',3,1);
PERFORM _sq(q,'We hear with our __?','Ears help us hear','Eyes','Ears','Tongue','Skin',2,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Body Parts',2,120,50,2);
PERFORM _sq(q,'How many fingers on two hands?','5 + 5 = 10','8','10','12','20',2,1);
PERFORM _sq(q,'The heart is in our __?','The heart is in the chest','Head','Chest','Leg','Hand',2,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Staying Healthy',2,120,50,3);
PERFORM _sq(q,'We should brush our teeth __ times a day.','Brush twice daily','1','2','3','5',2,1);
PERFORM _sq(q,'Which food keeps bones strong?','Milk has calcium for strong bones','Chips','Candy','Milk','Soda',3,2);

-- ═══════════════════════════════════════════════════════════
-- MODULE 4: General Knowledge 🌍
-- ═══════════════════════════════════════════════════════════
m := gen_random_uuid();
INSERT INTO modules (id,title,subtitle,emoji,accent_color,display_order,grade_id,is_active,status)
VALUES (m,'General Knowledge','Know the world','🌍','#D97706',4,g,true,'published');

-- ── Ch 4.1: Our Community ───────────────────────────────────
c := gen_random_uuid();
INSERT INTO chapters (id,module_id,title,chapter_number,sort_order) VALUES (c,m,'Our Community',1,1);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'People Who Help Us',2,120,50,1);
PERFORM _sq(q,'Who puts out fires?','Firefighters fight fires','Doctor','Teacher','Firefighter','Baker',3,1);
PERFORM _sq(q,'Who treats sick people?','Doctors treat patients','Farmer','Chef','Pilot','Doctor',4,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Places Around Us',2,120,50,2);
PERFORM _sq(q,'Where do we go to study?','We study at school','Market','Hospital','School','Park',3,1);
PERFORM _sq(q,'Where do we buy medicines?','Medicines at pharmacy','Library','Pharmacy','Bank','Zoo',2,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Transport',2,120,50,3);
PERFORM _sq(q,'Which travels on rails?','Trains run on tracks','Bus','Car','Train','Boat',3,1);
PERFORM _sq(q,'Which flies in the sky?','Aeroplanes fly','Ship','Bicycle','Truck','Aeroplane',4,2);

-- ── Ch 4.2: Our Earth ───────────────────────────────────────
c := gen_random_uuid();
INSERT INTO chapters (id,module_id,title,chapter_number,sort_order) VALUES (c,m,'Our Earth',2,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Land & Water',2,120,50,1);
PERFORM _sq(q,'Most of the Earth is covered by __?','71% of Earth is water','Land','Water','Ice','Sand',2,1);
PERFORM _sq(q,'The biggest ocean is the __?','Pacific is largest','Atlantic','Indian','Pacific','Arctic',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Seasons',2,120,50,2);
PERFORM _sq(q,'How many seasons are there?','India has 3 main seasons','2','3','4','5',3,1);
PERFORM _sq(q,'In summer, the weather is __?','Summer is hot','Cold','Hot','Rainy','Snowy',2,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Day & Night',2,120,50,3);
PERFORM _sq(q,'We see the Sun during __?','Sun shines in the day','Night','Day','Both','Neither',2,1);
PERFORM _sq(q,'The Moon is seen at __?','Moon is visible at night','Morning','Afternoon','Night','Noon',3,2);

-- ═══════════════════════════════════════════════════════════
-- MODULE 5: Good Habits 🌱
-- ═══════════════════════════════════════════════════════════
m := gen_random_uuid();
INSERT INTO modules (id,title,subtitle,emoji,accent_color,display_order,grade_id,is_active,status)
VALUES (m,'Good Habits','Be the best you','🌱','#7C3AED',5,g,true,'published');

-- ── Ch 5.1: Hygiene & Health ────────────────────────────────
c := gen_random_uuid();
INSERT INTO chapters (id,module_id,title,chapter_number,sort_order) VALUES (c,m,'Hygiene & Health',1,1);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Clean Hands',2,120,50,1);
PERFORM _sq(q,'We should wash hands before __?','Wash hands before eating','Sleeping','Eating','Playing','Reading',2,1);
PERFORM _sq(q,'We use __ to wash our hands.','Soap cleans germs','Oil','Mud','Soap','Paint',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Daily Routine',2,120,50,2);
PERFORM _sq(q,'How many hours should children sleep?','Kids need 8-10 hours','4','6','8-10','12',3,1);
PERFORM _sq(q,'We should take a bath __?','Bathe daily for hygiene','Weekly','Every day','Monthly','Never',2,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Healthy Food',2,120,50,3);
PERFORM _sq(q,'Which is a healthy snack?','Fruits are nutritious','Chips','Candy','Fruit','Soda',3,1);
PERFORM _sq(q,'We should drink plenty of __?','Water keeps us hydrated','Juice','Soda','Cola','Water',4,2);

-- ── Ch 5.2: Safety & Manners ────────────────────────────────
c := gen_random_uuid();
INSERT INTO chapters (id,module_id,title,chapter_number,sort_order) VALUES (c,m,'Safety & Manners',2,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Road Safety',2,120,50,1);
PERFORM _sq(q,'We should cross the road at a __?','Zebra crossings are safe','Corner','Middle','Zebra crossing','Anywhere',3,1);
PERFORM _sq(q,'Red traffic light means __?','Red = Stop','Go','Slow down','Stop','Speed up',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Good Manners',2,120,50,2);
PERFORM _sq(q,'What do we say when we receive a gift?','Thank you shows gratitude','Sorry','Please','Thank you','Goodbye',3,1);
PERFORM _sq(q,'What do we say when we want something?','Please is polite','Sorry','Thank you','Please','Hello',3,2);

q := gen_random_uuid();
INSERT INTO quizzes (id,chapter_id,title,question_count,time_limit_secs,max_xp,sort_order) VALUES (q,c,'Being Kind',2,120,50,3);
PERFORM _sq(q,'Sharing with friends is __?','Sharing is caring','Bad','Rude','Kind','Silly',3,1);
PERFORM _sq(q,'If you hurt someone, you should say __?','Apologize when wrong','Thank you','Please','Sorry','Hello',3,2);

END $$;

-- ── Cleanup helper ──────────────────────────────────────────
DROP FUNCTION IF EXISTS _sq;

-- ── Verify: show inserted modules ─────────────────────────
SELECT id, title, emoji, grade_id, is_active, status
FROM modules
WHERE grade_id = 'e857cd23-06b1-410d-ba2e-3b8939ecd79b'
ORDER BY display_order;

