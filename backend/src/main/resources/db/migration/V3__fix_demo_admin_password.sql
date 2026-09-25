-- V3__fix_demo_admin_password.sql
-- Correct the seeded demo administrator password hash.
-- Password: StrongPassword123!

UPDATE users
SET password_hash = '$2a$10$nuEXP.QO7RT0A8Z65e3onOzWuJhRyLraKmmDa41Bq6Z8oEVKchKtm',
    failed_login_attempts = 0,
    account_locked = FALSE,
    lockout_until = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'aditi.rao@example.com';
