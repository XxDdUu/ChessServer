-- Flyway migration: Add ban_reason, ban_description, and banned_by_user columns to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS ban_reason VARCHAR(255) DEFAULT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS ban_description VARCHAR(1000) DEFAULT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS banned_by_user VARCHAR(100) DEFAULT NULL;

