


CREATE DATABASE IF NOT EXISTS libraryplus DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE libraryplus;


CREATE TABLE IF NOT EXISTS roles (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;


CREATE TABLE IF NOT EXISTS users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  salt VARCHAR(255),
  role_id INT NOT NULL,
  full_name VARCHAR(255),
  phone VARCHAR(50),
  date_of_birth DATE,
  card_number VARCHAR(512),
  card_balance DECIMAL(10,2) DEFAULT 0.00,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB;


CREATE TABLE IF NOT EXISTS clients (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  phone VARCHAR(50) NOT NULL UNIQUE,
  first_name VARCHAR(100),
  last_name VARCHAR(100),
  date_of_birth DATE,
  membership_type VARCHAR(20) DEFAULT 'STANDARD',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;


CREATE TABLE IF NOT EXISTS books (
  isbn VARCHAR(20) PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  author VARCHAR(255),
  category VARCHAR(50),
  availability_status VARCHAR(50) DEFAULT 'AVAILABLE',
  cover_image_path VARCHAR(1024),
  description TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;


CREATE TABLE IF NOT EXISTS ratings (
  id INT AUTO_INCREMENT PRIMARY KEY,
  book_isbn VARCHAR(20) NOT NULL,
  client_id INT NOT NULL,
  rating TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (book_isbn, client_id),
  FOREIGN KEY (book_isbn) REFERENCES books(isbn) ON DELETE CASCADE,
  FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
) ENGINE=InnoDB;


CREATE TABLE IF NOT EXISTS comments (
  id INT AUTO_INCREMENT PRIMARY KEY,
  book_isbn VARCHAR(20) NOT NULL,
  client_id INT NOT NULL,
  comment TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (book_isbn) REFERENCES books(isbn) ON DELETE CASCADE,
  FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
) ENGINE=InnoDB;


CREATE TABLE IF NOT EXISTS book_waitlist (
  id INT AUTO_INCREMENT PRIMARY KEY,
  book_isbn VARCHAR(20) NOT NULL,
  client_id INT NOT NULL,
  position INT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (book_isbn) REFERENCES books(isbn) ON DELETE CASCADE,
  FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
  UNIQUE (book_isbn, client_id)
) ENGINE=InnoDB;


CREATE TABLE IF NOT EXISTS subscriptions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  client_id INT NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
) ENGINE=InnoDB;


CREATE TABLE IF NOT EXISTS purchases (
  id INT AUTO_INCREMENT PRIMARY KEY,
  client_id INT NOT NULL,
  book_isbn VARCHAR(20) NOT NULL,
  quantity INT DEFAULT 1,
  unit_price DECIMAL(10,2) NOT NULL,
  purchase_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
  FOREIGN KEY (book_isbn) REFERENCES books(isbn) ON DELETE CASCADE
) ENGINE=InnoDB;


CREATE TABLE IF NOT EXISTS loans (
  id INT AUTO_INCREMENT PRIMARY KEY,
  book_isbn VARCHAR(20) NOT NULL,
  client_id INT NOT NULL,
  borrow_date TIMESTAMP NOT NULL,
  expected_return_date TIMESTAMP NOT NULL,
  actual_return_date TIMESTAMP NULL,
  fine_amount DECIMAL(10,2) DEFAULT 0.00,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (book_isbn) REFERENCES books(isbn) ON DELETE CASCADE,
  FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
  INDEX idx_loans_not_returned (actual_return_date)
) ENGINE=InnoDB;


CREATE TABLE IF NOT EXISTS transactions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  client_id INT NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  reason VARCHAR(255),
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  resulting_balance DECIMAL(10,2),
  FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
) ENGINE=InnoDB;


CREATE TABLE IF NOT EXISTS admin_messages (
  id INT AUTO_INCREMENT PRIMARY KEY,
  client_id INT,
  sender_email VARCHAR(255),
  subject VARCHAR(255),
  content TEXT,
  status VARCHAR(20) DEFAULT 'UNREAD',
  admin_reply TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE SET NULL
) ENGINE=InnoDB;


CREATE TABLE IF NOT EXISTS login_events (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ip_address VARCHAR(45),
  meta TEXT,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

INSERT IGNORE INTO roles (name) VALUES ('ADMIN'), ('CLIENT');


SET @dbname = DATABASE();
SET @tablename = 'books';
SET @columnname = 'stock';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' INT DEFAULT 1')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

UPDATE books SET stock = 12 WHERE category LIKE '%Fiction%' OR category LIKE '%Novel%';
UPDATE books SET stock = 5 WHERE category LIKE '%Science%' OR category LIKE '%Programming%' OR category LIKE '%Computer%';
UPDATE books SET stock = 8 WHERE category LIKE '%History%' OR category LIKE '%Biography%';
UPDATE books SET stock = 10 WHERE category LIKE '%Business%' OR category LIKE '%Self-Help%';
UPDATE books SET stock = 7 WHERE category LIKE '%Art%' OR category LIKE '%Philosophy%';
UPDATE books SET stock = 15 WHERE category LIKE '%Children%' OR category LIKE '%Young Adult%';
UPDATE books SET stock = 6 WHERE category LIKE '%Poetry%' OR category LIKE '%Drama%';


UPDATE books SET stock = 8 WHERE stock = 1;


SET @columnname = 'price';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' DECIMAL(10,2) DEFAULT 1.00')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

UPDATE books SET price = 12.00 WHERE (category LIKE '%Fiction%' OR category LIKE '%Novel%') AND (price = 1.00 OR price IS NULL);
UPDATE books SET price = 34.99 WHERE (category LIKE '%Science%' OR category LIKE '%Programming%' OR category LIKE '%Computer%') AND (price = 1.00 OR price IS NULL);
UPDATE books SET price = 52.97 WHERE (category LIKE '%History%' OR category LIKE '%Biography%') AND (price = 1.00 OR price IS NULL);
UPDATE books SET price = 353.67 WHERE (category LIKE '%Business%' OR category LIKE '%Self-Help%') AND (price = 1.00 OR price IS NULL);
UPDATE books SET price = 36.63 WHERE (category LIKE '%Art%' OR category LIKE '%Philosophy%') AND (price = 1.00 OR price IS NULL);
UPDATE books SET price = 76.49 WHERE (category LIKE '%Children%' OR category LIKE '%Young Adult%') AND (price = 1.00 OR price IS NULL);
UPDATE books SET price = 26.99 WHERE (category LIKE '%Poetry%' OR category LIKE '%Drama%') AND (price = 1.00 OR price IS NULL);


UPDATE books SET price = 25.00 WHERE price = 1.00 OR price IS NULL;
