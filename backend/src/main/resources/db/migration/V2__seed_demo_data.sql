-- V2__seed_demo_data.sql
-- Seed essential admin and initial discovery profiles
-- Default password for demo users is: StrongPassword123!
-- BCrypt hash: $2a$10$wO3l2F0Ww6sF3e0k1gZcSeX9O5O9uT.RjR5/r0E3F3B1Y3c6M7F.K

-- 1. Main User / Administrator: Aditi Rao
INSERT INTO users (id, email, password_hash, display_name, role, account_locked, failed_login_attempts, mfa_enabled, mfa_secret, created_at, updated_at)
VALUES (
    'user-me-001',
    'aditi.rao@example.com',
    '$2a$10$f9b3K.dK1fU2v6k5L.tILe3w4K4Z9.y4uO3J5R8L2v1M0o9p8Q.2m',
    'Aditi Rao',
    'ADMIN',
    FALSE,
    0,
    FALSE,
    'JBSWY3DPEHPK3PXP',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;

INSERT INTO profiles (
    id, user_id, display_name, date_of_birth, gender, country, state_province, city, bio,
    phone_number, photo_available, profession, employer, salary_range, education, skills,
    linkedin_url, instagram_url, facebook_url, website_url, profile_visibility,
    show_phone, show_salary, show_social, background_check_status, verification_status,
    verified_at, profile_locked, created_at, updated_at
) VALUES (
    'profile-me-001',
    'user-me-001',
    'Aditi Rao',
    '1998-05-15',
    'Female',
    'India',
    'Karnataka',
    'Bengaluru',
    'Software architect with a love for classical Carnatic music, filter coffee, and mountain trails. Looking for someone grounded, ambitious, and with a kind sense of humor.',
    '+91 98765 43210',
    FALSE,
    'Senior Software Architect',
    'Tech Innovations Lab',
    '₹35-50 LPA',
    'M.Tech Computer Science, IISc Bangalore',
    'Cloud Architecture, Distributed Systems, Classical Vocal',
    'https://linkedin.com/in/aditirao-example',
    'https://instagram.com/aditi_trails',
    NULL,
    'https://aditirao.dev',
    'PUBLIC',
    TRUE,
    TRUE,
    TRUE,
    'VERIFIED',
    'VERIFIED',
    CURRENT_TIMESTAMP,
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- 2. Discovery Sample User 1: Priya Sharma
INSERT INTO users (id, email, password_hash, display_name, role, created_at, updated_at)
VALUES (
    'user-001',
    'priya.sharma@example.com',
    '$2a$10$f9b3K.dK1fU2v6k5L.tILe3w4K4Z9.y4uO3J5R8L2v1M0o9p8Q.2m',
    'Priya Sharma',
    'MEMBER',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;

INSERT INTO profiles (
    id, user_id, display_name, date_of_birth, gender, country, state_province, city, bio,
    profession, employer, salary_range, education, skills, linkedin_url, instagram_url,
    website_url, profile_visibility, show_phone, show_salary, show_social,
    background_check_status, verification_status, verified_at, created_at, updated_at
) VALUES (
    'prof-001',
    'user-001',
    'Priya Sharma',
    '1999-08-20',
    'Female',
    'India',
    'Karnataka',
    'Bengaluru',
    'Lead product designer crafting digital experiences. Weekend ceramist and coffee connoisseur.',
    'Lead Product Designer',
    'Studio Indigo',
    '₹25-35 LPA',
    'B.Des, NID Ahmedabad',
    'Design Systems, User Research, Pottery',
    'https://linkedin.com/in/priyasharma',
    'https://instagram.com/priya_designs',
    'https://priyasharma.design',
    'PUBLIC',
    FALSE,
    TRUE,
    TRUE,
    'VERIFIED',
    'VERIFIED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- 3. Discovery Sample User 2: Rahul Verma
INSERT INTO users (id, email, password_hash, display_name, role, created_at, updated_at)
VALUES (
    'user-002',
    'rahul.verma@example.com',
    '$2a$10$f9b3K.dK1fU2v6k5L.tILe3w4K4Z9.y4uO3J5R8L2v1M0o9p8Q.2m',
    'Rahul Verma',
    'MEMBER',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;

INSERT INTO profiles (
    id, user_id, display_name, date_of_birth, gender, country, state_province, city, bio,
    profession, employer, salary_range, education, skills, linkedin_url, instagram_url,
    profile_visibility, show_phone, show_salary, show_social,
    background_check_status, verification_status, verified_at, created_at, updated_at
) VALUES (
    'prof-002',
    'user-002',
    'Rahul Verma',
    '1996-03-12',
    'Male',
    'India',
    'Maharashtra',
    'Mumbai',
    'Fintech professional passionate about venture capital, long-distance running, and landscape photography.',
    'Vice President, Fintech',
    'Apex Capital',
    '₹45-60 LPA',
    'MBA Finance, IIM Calcutta',
    'Investment Strategy, Marathon Running, Photography',
    'https://linkedin.com/in/rahulverma',
    'https://instagram.com/rahul_runs',
    'PUBLIC',
    FALSE,
    TRUE,
    TRUE,
    'VERIFIED',
    'VERIFIED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- 4. Discovery Sample User 3: Ananya Patel
INSERT INTO users (id, email, password_hash, display_name, role, created_at, updated_at)
VALUES (
    'user-003',
    'ananya.patel@example.com',
    '$2a$10$f9b3K.dK1fU2v6k5L.tILe3w4K4Z9.y4uO3J5R8L2v1M0o9p8Q.2m',
    'Ananya Patel',
    'MEMBER',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;

INSERT INTO profiles (
    id, user_id, display_name, date_of_birth, gender, country, state_province, city, bio,
    profession, employer, salary_range, education, skills, linkedin_url, website_url,
    profile_visibility, show_phone, show_salary, show_social,
    background_check_status, verification_status, verified_at, created_at, updated_at
) VALUES (
    'prof-003',
    'user-003',
    'Ananya Patel',
    '1998-11-04',
    'Female',
    'India',
    'Gujarat',
    'Ahmedabad',
    'AI researcher exploring foundation models. Trained in classical dance, stargazing on weekends.',
    'AI Research Scientist',
    'DeepTech Research Labs',
    '₹30-40 LPA',
    'Ph.D. Computer Science, IIT Bombay',
    'Machine Learning, Astronomy, Indian Classical Dance',
    'https://linkedin.com/in/ananyapatel',
    'https://ananya-ai.org',
    'PUBLIC',
    FALSE,
    TRUE,
    TRUE,
    'VERIFIED',
    'VERIFIED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- 5. Discovery Sample User 4: Vikram Singhania
INSERT INTO users (id, email, password_hash, display_name, role, created_at, updated_at)
VALUES (
    'user-004',
    'vikram.singhania@example.com',
    '$2a$10$f9b3K.dK1fU2v6k5L.tILe3w4K4Z9.y4uO3J5R8L2v1M0o9p8Q.2m',
    'Vikram Singhania',
    'MEMBER',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;

INSERT INTO profiles (
    id, user_id, display_name, date_of_birth, gender, country, state_province, city, bio,
    profession, employer, salary_range, education, skills, linkedin_url,
    profile_visibility, show_phone, show_salary, show_social,
    background_check_status, verification_status, verified_at, created_at, updated_at
) VALUES (
    'prof-004',
    'user-004',
    'Vikram Singhania',
    '1994-07-28',
    'Male',
    'India',
    'Delhi',
    'New Delhi',
    'Corporate lawyer advising high-growth startups and cross-border M&A transactions. Book collector and squash player.',
    'Partner, Corporate Law',
    'Singhania & Partners',
    '₹50-70 LPA',
    'LL.M., National Law School Bangalore',
    'Commercial Arbitration, Venture Deals, Squash',
    'https://linkedin.com/in/vikramsinghania',
    'PUBLIC',
    FALSE,
    TRUE,
    TRUE,
    'VERIFIED',
    'VERIFIED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- 6. Sample Initial Notification
INSERT INTO notifications (id, user_id, type, title, body, read_at, created_at)
VALUES (
    'notif-001',
    'user-me-001',
    'SECURITY_ALERT',
    'Profile Verified Successfully',
    'Your identity and background check have been verified with high trust status.',
    NULL,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- 7. Sample Initial Moderation Report
INSERT INTO reports (id, reporter_user_id, reported_user_id, reason, details, status, created_at, updated_at)
VALUES (
    'rep-001',
    'user-001',
    'user-002',
    'INAPPROPRIATE_BEHAVIOR',
    'Repeated unwanted messaging after unmatching.',
    'OPEN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;
