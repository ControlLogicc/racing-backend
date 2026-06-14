-- ============================================================
-- HORSE RACING TOURNAMENT MANAGEMENT SYSTEM - MVP
-- Sample Data Script - SQL Server
-- Database: HorseRacingMVP
-- Date: 2026-06-14
-- ============================================================

USE HorseRacingMVP;

GO

-- ============================================================
-- 1. USER - Sample Users
-- ============================================================
INSERT INTO dbo.[user] (full_name, email, password_hash, phone, role, status)
VALUES
    ('Admin User', 'admin@horseracing.com', 'hashed_password_admin_123', '0901234567', 'admin', 'active'),
    ('John Smith', 'john.smith@horseracing.com', 'hashed_password_john_456', '0902345678', 'owner', 'active'),
    ('Sarah Johnson', 'sarah.johnson@horseracing.com', 'hashed_password_sarah_789', '0903456789', 'owner', 'active'),
    ('Michael Brown', 'michael.brown@horseracing.com', 'hashed_password_michael_101', '0904567890', 'staff', 'active'),
    ('Emily Davis', 'emily.davis@horseracing.com', 'hashed_password_emily_202', '0905678901', 'staff', 'active'),
    ('Robert Wilson', 'robert.wilson@horseracing.com', 'hashed_password_robert_303', '0906789012', 'referee', 'active'),
    ('James Martinez', 'james.martinez@horseracing.com', 'hashed_password_james_404', '0907890123', 'referee', 'active'),
    ('David Lee', 'david.lee@horseracing.com', 'hashed_password_david_505', '0908901234', 'jockey', 'active'),
    ('Christopher Garcia', 'chris.garcia@horseracing.com', 'hashed_password_chris_606', '0909012345', 'jockey', 'active'),
    ('Daniel Rodriguez', 'daniel.rodriguez@horseracing.com', 'hashed_password_daniel_707', '0910123456', 'jockey', 'active'),
    ('Lisa Anderson', 'lisa.anderson@horseracing.com', 'hashed_password_lisa_808', '0911234567', 'spectator', 'active');

GO

-- ============================================================
-- 2. STAFF - Staff Profiles
-- ============================================================
INSERT INTO dbo.staff (user_id, staff_code, department, status)
VALUES
    (4, 'STAFF001', 'Race Management', 'active'),
    (5, 'STAFF002', 'Operations', 'active');

GO

-- ============================================================
-- 3. REFEREE - Referee Profiles
-- ============================================================
INSERT INTO dbo.referee (user_id, license_no, status)
VALUES
    (6, 'REF-LICENSE-001', 'active'),
    (7, 'REF-LICENSE-002', 'active');

GO

-- ============================================================
-- 4. JOCKEY - Jockey Profiles
-- ============================================================
INSERT INTO dbo.jockey (user_id, weight, experience_years, status)
VALUES
    (8, 55.50, 8, 'available'),
    (9, 54.75, 12, 'available'),
    (10, 56.25, 5, 'available');

GO

-- ============================================================
-- 5. HORSE - Horse Profiles
-- ============================================================
INSERT INTO dbo.horse (owner_id, horse_name, age, gender, breed, current_score, horse_class, status)
VALUES
    (2, 'Thunder Storm', 5, 'M', 'Thoroughbred', 450.00, 2, 'active'),
    (2, 'Lightning Queen', 4, 'F', 'Thoroughbred', 380.00, 3, 'active'),
    (3, 'Royal Prince', 6, 'M', 'Arabian', 520.00, 1, 'active'),
    (3, 'Golden Dawn', 3, 'F', 'Thoroughbred', 290.00, 4, 'active'),
    (2, 'Wind Dancer', 7, 'M', 'Standardbred', 410.00, 2, 'active');

GO

-- ============================================================
-- 6. SEASON - Racing Seasons
-- ============================================================
INSERT INTO dbo.season (season_name, start_date, end_date, status)
VALUES
    ('Spring Season 2026', '2026-03-01', '2026-05-31', 'active'),
    ('Summer Season 2026', '2026-06-01', '2026-08-31', 'active'),
    ('Fall Season 2026', '2026-09-01', '2026-11-30', 'draft');

GO

-- ============================================================
-- 7. RACECOURSE - Racecourses
-- ============================================================
INSERT INTO dbo.racecourse (racecourse_name, location, surface_type, capacity)
VALUES
    ('Royal Racecourse', 'Downtown District', 'turf', 15000),
    ('Meadow Valley Track', 'Suburban Area', 'dirt', 10000),
    ('Modern Racing Arena', 'Industrial Zone', 'synthetic', 12000);

GO

-- ============================================================
-- 8. RACE_MEETING - Racing Event Days
-- ============================================================
INSERT INTO dbo.race_meeting (season_id, racecourse_id, meeting_date, status)
VALUES
    (1, 1, '2026-06-14', 'open'),
    (1, 2, '2026-06-21', 'scheduled'),
    (2, 3, '2026-07-04', 'scheduled'),
    (2, 1, '2026-07-18', 'scheduled');

GO

-- ============================================================
-- 9. RACE_CONDITION - Race Types/Conditions
-- ============================================================
INSERT INTO dbo.race_condition (condition_name, distance, track_type, min_entries, max_entries, class_requirement)
VALUES
    ('1800m Turf Class 1', 1800, 'turf', 6, 12, 'Class 1-2'),
    ('1600m Dirt Class 2', 1600, 'dirt', 6, 12, 'Class 2-3'),
    ('2000m Turf Class 3', 2000, 'turf', 8, 14, 'Class 3-4'),
    ('1400m Synthetic Class 4', 1400, 'synthetic', 6, 12, 'Class 4-5');

GO

-- ============================================================
-- 10. RACE - Individual Races
-- ============================================================
INSERT INTO dbo.race (meeting_id, condition_id, staff_id, referee_id, race_name, race_no, scheduled_time, status)
VALUES
    (1, 1, 4, 6, 'Premium Cup - Race 1', 1, '2026-06-14 14:00:00', 'registration_open'),
    (1, 2, 5, 7, 'Standard Cup - Race 2', 2, '2026-06-14 15:30:00', 'draft'),
    (2, 3, 4, 6, 'Champion Race - Race 1', 1, '2026-06-21 13:00:00', 'scheduled'),
    (3, 4, 5, 7, 'Summer Grand Prix - Race 1', 1, '2026-07-04 14:30:00', 'scheduled');

GO

-- ============================================================
-- 11. PRIZE_STRUCTURES - Prize and Score for Each Race
-- ============================================================
INSERT INTO dbo.prize_structures (race_id, position, amount, score)
VALUES
    -- Race 1 prizes
    (1, 1, 10000000.00, 100),
    (1, 2, 5000000.00, 50),
    (1, 3, 2000000.00, 20),
    (1, 4, 1000000.00, 10),
    -- Race 2 prizes
    (2, 1, 8000000.00, 80),
    (2, 2, 4000000.00, 40),
    (2, 3, 1500000.00, 15),
    (2, 4, 800000.00, 8),
    -- Race 3 prizes
    (3, 1, 12000000.00, 120),
    (3, 2, 6000000.00, 60),
    (3, 3, 3000000.00, 30),
    (3, 4, 1500000.00, 15),
    -- Race 4 prizes
    (4, 1, 7000000.00, 70),
    (4, 2, 3500000.00, 35),
    (4, 3, 1400000.00, 14);

GO

-- ============================================================
-- 12. RACE_REGISTRATION - Horse Registrations
-- ============================================================
INSERT INTO dbo.race_registration (race_id, horse_id, submitted_by_user_id, approved_by_staff_id, registration_status, submitted_at, reviewed_at)
VALUES
    (1, 1, 2, 4, 'approved', '2026-06-10 10:00:00', '2026-06-11 14:00:00'),
    (1, 3, 3, 4, 'approved', '2026-06-10 11:00:00', '2026-06-11 14:00:00'),
    (1, 5, 2, 4, 'approved', '2026-06-10 12:00:00', '2026-06-11 14:00:00'),
    (2, 2, 2, 5, 'approved', '2026-06-10 13:00:00', '2026-06-11 15:00:00'),
    (2, 4, 3, 5, 'approved', '2026-06-10 14:00:00', '2026-06-11 15:00:00'),
    (3, 1, 2, 4, 'submitted', '2026-06-17 09:00:00', NULL),
    (3, 3, 3, 4, 'submitted', '2026-06-17 10:00:00', NULL);

GO

-- ============================================================
-- 13. RACE_INVITATION - Jockey Invitations
-- ============================================================
INSERT INTO dbo.race_invitation (registration_id, jockey_id, invitation_status, sent_at, responded_at)
VALUES
    (1, 8, 'accepted', '2026-06-11 09:00:00', '2026-06-11 10:00:00'),
    (2, 9, 'accepted', '2026-06-11 09:30:00', '2026-06-11 10:30:00'),
    (3, 10, 'accepted', '2026-06-11 10:00:00', '2026-06-11 11:00:00'),
    (4, 8, 'accepted', '2026-06-11 14:00:00', '2026-06-11 15:00:00'),
    (5, 9, 'accepted', '2026-06-11 14:30:00', '2026-06-11 15:30:00'),
    (6, 10, 'sent', '2026-06-18 09:00:00', NULL),
    (7, 8, 'pending_response', '2026-06-18 10:00:00', NULL);

GO

-- ============================================================
-- 14. RACE_ENTRY - Official Race Entries
-- ============================================================
INSERT INTO dbo.race_entry (race_id, registration_id, invitation_id, horse_id, jockey_id, confirmed_by_staff_id, gate_number, draw_number, handicap_weight, actual_weight, weight_check_status, entry_status)
VALUES
    (1, 1, 1, 1, 8, 4, 1, 3, 60.00, 59.80, 'passed', 'ready'),
    (1, 2, 2, 3, 9, 4, 3, 1, 61.00, 60.95, 'passed', 'ready'),
    (1, 3, 3, 5, 10, 4, 5, 2, 60.50, 60.40, 'passed', 'ready'),
    (2, 4, 4, 2, 8, 5, 2, 4, 59.50, 59.45, 'passed', 'ready'),
    (2, 5, 5, 4, 9, 5, 4, 2, 58.50, 58.60, 'failed', 'declared');

GO

-- ============================================================
-- 15. RACE_RESULT - Race Results (only for Race 1)
-- ============================================================
INSERT INTO dbo.race_result (entry_id, race_id, position, finish_time, result_status, score_awarded, prize_amount)
VALUES
    (1, 1, 1, '00:28:45.500', 'official', 100, 10000000.00),
    (2, 1, 2, '00:29:12.300', 'official', 50, 5000000.00),
    (3, 1, 3, '00:29:45.800', 'official', 20, 2000000.00);

GO

-- ============================================================
-- 16. REFEREE_REPORT - Referee Reports
-- ============================================================
INSERT INTO dbo.referee_report (race_id, entry_id, referee_id, report_type, description, decision, report_status)
VALUES
    (1, 1, 6, 'pre_race_check', 'Horse Thunder Storm passed all pre-race checks. Equipment verified. Weight OK.', 'no_issue', 'submitted'),
    (1, 2, 6, 'pre_race_check', 'Horse Royal Prince passed weight check and equipment inspection.', 'no_issue', 'submitted'),
    (1, 3, 7, 'race_review', 'Race ran smoothly. No violations observed during the race.', 'no_issue', 'submitted'),
    (1, 1, 6, 'result_confirmation', 'Result official: Thunder Storm crossed finish line first at 28:45.5', 'result_confirmed', 'submitted');

GO

-- ============================================================
-- Display Summary
-- ============================================================
PRINT '================================';
PRINT 'Sample Data Insertion Complete!';
PRINT '================================';
PRINT '';
PRINT 'Users created: 11';
PRINT 'Staff: 2';
PRINT 'Referees: 2';
PRINT 'Jockeys: 3';
PRINT 'Horses: 5';
PRINT 'Seasons: 3';
PRINT 'Racecourses: 3';
PRINT 'Race Meetings: 4';
PRINT 'Race Conditions: 4';
PRINT 'Races: 4';
PRINT 'Prize Structures: 15';
PRINT 'Race Registrations: 7';
PRINT 'Race Invitations: 7';
PRINT 'Race Entries: 5';
PRINT 'Race Results: 3';
PRINT 'Referee Reports: 4';

GO

-- ============================================================
-- Verify Data
-- ============================================================
-- SELECT COUNT(*) as 'User Count' FROM dbo.[user];
-- SELECT COUNT(*) as 'Horse Count' FROM dbo.horse;
-- SELECT COUNT(*) as 'Race Count' FROM dbo.race;
-- SELECT COUNT(*) as 'Race Entry Count' FROM dbo.race_entry;
-- SELECT COUNT(*) as 'Race Result Count' FROM dbo.race_result;

-- ============================================================
-- End of Sample Data Script
-- ============================================================
