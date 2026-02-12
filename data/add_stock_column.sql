
ALTER TABLE books ADD COLUMN IF NOT EXISTS stock INT DEFAULT 1;






UPDATE books SET stock = 12 WHERE category LIKE '%Fiction%' OR category LIKE '%Novel%';
UPDATE books SET stock = 5 WHERE category LIKE '%Science%' OR category LIKE '%Programming%' OR category LIKE '%Computer%';
UPDATE books SET stock = 8 WHERE category LIKE '%History%' OR category LIKE '%Biography%';
UPDATE books SET stock = 10 WHERE category LIKE '%Business%' OR category LIKE '%Self-Help%';
UPDATE books SET stock = 7 WHERE category LIKE '%Art%' OR category LIKE '%Philosophy%';
UPDATE books SET stock = 15 WHERE category LIKE '%Children%' OR category LIKE '%Young Adult%';
UPDATE books SET stock = 6 WHERE category LIKE '%Poetry%' OR category LIKE '%Drama%';


UPDATE books SET stock = 8 WHERE stock = 1;
