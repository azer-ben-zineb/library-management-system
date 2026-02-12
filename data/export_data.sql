



SCRIPT TO 'libraryplus_backup.sql';




SELECT id, email, full_name, phone, date_of_birth, role_id, card_number, card_balance
FROM users;


SELECT id, user_id, phone, first_name, last_name, date_of_birth, membership_type
FROM clients;


SELECT isbn, title, author, category, availability_status, cover_image_path, description, 
       avg_rating, ratings_count, purchases_count, loans_count
FROM books;


SELECT id, book_isbn, client_id, borrow_date, expected_return_date, actual_return_date, fine_amount
FROM loans;


SELECT id, client_id, book_isbn, quantity, unit_price, purchase_date
FROM purchases;


SELECT id, book_isbn, client_id, comment, created_at
FROM comments;


SELECT id, client_id, start_date, end_date, is_active
FROM subscriptions;


SELECT id, book_isbn, client_id, position, created_at
FROM book_waitlist;


SELECT id, client_id, sender_email, subject, content, status, admin_reply, created_at
FROM admin_messages;
