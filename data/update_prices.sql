



UPDATE books SET price = 12.00 WHERE (category LIKE '%Fiction%' OR category LIKE '%Novel%') AND (price = 1.00 OR price IS NULL);


UPDATE books SET price = 34.99 WHERE (category LIKE '%Science%' OR category LIKE '%Programming%' OR category LIKE '%Computer%') AND (price = 1.00 OR price IS NULL);


UPDATE books SET price = 52.97 WHERE (category LIKE '%History%' OR category LIKE '%Biography%') AND (price = 1.00 OR price IS NULL);


UPDATE books SET price = 353.67 WHERE (category LIKE '%Business%' OR category LIKE '%Self-Help%') AND (price = 1.00 OR price IS NULL);


UPDATE books SET price = 36.63 WHERE (category LIKE '%Art%' OR category LIKE '%Philosophy%') AND (price = 1.00 OR price IS NULL);


UPDATE books SET price = 76.49 WHERE (category LIKE '%Children%' OR category LIKE '%Young Adult%') AND (price = 1.00 OR price IS NULL);


UPDATE books SET price = 26.99 WHERE (category LIKE '%Poetry%' OR category LIKE '%Drama%') AND (price = 1.00 OR price IS NULL);


UPDATE books SET price = 25.00 WHERE price = 1.00 OR price IS NULL;

