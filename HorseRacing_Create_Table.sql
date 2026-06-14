-- ============================================================
-- HORSE RACING TOURNAMENT MANAGEMENT SYSTEM - MVP
-- SQL Server DDL Script - Create Database and Tables
-- Version: 1.0
-- Database: HorseRacingMVP
-- Platform: SQL Server 2016+
-- Date: 2026-06-14
-- ============================================================

-- Create Database
CREATE DATABASE HorseRacingMVP
    COLLATE SQL_Latin1_General_CP1_CI_AS;

GO

USE HorseRacingMVP;

GO

-- ============================================================
-- Drop tables if exist (careful in production!)
-- ============================================================
/*
IF OBJECT_ID('dbo.referee_report', 'U') IS NOT NULL
    DROP TABLE dbo.referee_report;
IF OBJECT_ID('dbo.race_result', 'U') IS NOT NULL
    DROP TABLE dbo.race_result;
IF OBJECT_ID('dbo.race_entry', 'U') IS NOT NULL
    DROP TABLE dbo.race_entry;
IF OBJECT_ID('dbo.race_invitation', 'U') IS NOT NULL
    DROP TABLE dbo.race_invitation;
IF OBJECT_ID('dbo.race_registration', 'U') IS NOT NULL
    DROP TABLE dbo.race_registration;
IF OBJECT_ID('dbo.prize_structures', 'U') IS NOT NULL
    DROP TABLE dbo.prize_structures;
IF OBJECT_ID('dbo.race', 'U') IS NOT NULL
    DROP TABLE dbo.race;
IF OBJECT_ID('dbo.race_condition', 'U') IS NOT NULL
    DROP TABLE dbo.race_condition;
IF OBJECT_ID('dbo.race_meeting', 'U') IS NOT NULL
    DROP TABLE dbo.race_meeting;
IF OBJECT_ID('dbo.racecourse', 'U') IS NOT NULL
    DROP TABLE dbo.racecourse;
IF OBJECT_ID('dbo.season', 'U') IS NOT NULL
    DROP TABLE dbo.season;
IF OBJECT_ID('dbo.horse', 'U') IS NOT NULL
    DROP TABLE dbo.horse;
IF OBJECT_ID('dbo.jockey', 'U') IS NOT NULL
    DROP TABLE dbo.jockey;
IF OBJECT_ID('dbo.referee', 'U') IS NOT NULL
    DROP TABLE dbo.referee;
IF OBJECT_ID('dbo.staff', 'U') IS NOT NULL
    DROP TABLE dbo.staff;
IF OBJECT_ID('dbo.[user]', 'U') IS NOT NULL
    DROP TABLE dbo.[user];
*/

GO

-- ============================================================
-- 1. USER - System User Account
-- ============================================================
CREATE TABLE dbo.[user] (
    user_id BIGINT PRIMARY KEY IDENTITY(1,1),
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT CK_USER_ROLE CHECK (role IN ('admin', 'owner', 'spectator', 'staff', 'referee', 'jockey')),
    CONSTRAINT CK_USER_STATUS CHECK (status IN ('active', 'inactive', 'suspended'))
);

CREATE INDEX idx_user_email ON dbo.[user](email);
CREATE INDEX idx_user_role ON dbo.[user](role);

GO

-- ============================================================
-- 2. STAFF - Staff Profile
-- ============================================================
CREATE TABLE dbo.staff (
    staff_id BIGINT PRIMARY KEY IDENTITY(1,1),
    user_id BIGINT NOT NULL UNIQUE,
    staff_code VARCHAR(30) NOT NULL UNIQUE,
    department VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT FK_STAFF_USER FOREIGN KEY (user_id) REFERENCES dbo.[user](user_id) ON DELETE NO ACTION,
    CONSTRAINT CK_STAFF_STATUS CHECK (status IN ('active', 'inactive'))
);

CREATE INDEX idx_staff_code ON dbo.staff(staff_code);

GO

-- ============================================================
-- 3. REFEREE - Referee Profile
-- ============================================================
CREATE TABLE dbo.referee (
    referee_id BIGINT PRIMARY KEY IDENTITY(1,1),
    user_id BIGINT NOT NULL UNIQUE,
    license_no VARCHAR(40) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT FK_REFEREE_USER FOREIGN KEY (user_id) REFERENCES dbo.[user](user_id) ON DELETE NO ACTION,
    CONSTRAINT CK_REFEREE_STATUS CHECK (status IN ('active', 'inactive', 'suspended'))
);

CREATE INDEX idx_referee_license ON dbo.referee(license_no);

GO

-- ============================================================
-- 4. JOCKEY - Jockey Profile
-- ============================================================
CREATE TABLE dbo.jockey (
    jockey_id BIGINT PRIMARY KEY IDENTITY(1,1),
    user_id BIGINT NOT NULL UNIQUE,
    weight DECIMAL(5,2),
    experience_years SMALLINT,
    status VARCHAR(20) NOT NULL DEFAULT 'available',
    created_at DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT FK_JOCKEY_USER FOREIGN KEY (user_id) REFERENCES dbo.[user](user_id) ON DELETE NO ACTION,
    CONSTRAINT CK_JOCKEY_STATUS CHECK (status IN ('available', 'unavailable', 'suspended'))
);

CREATE INDEX idx_jockey_status ON dbo.jockey(status);

GO

-- ============================================================
-- 5. HORSE - Horse Profile
-- ============================================================
CREATE TABLE dbo.horse (
    horse_id BIGINT PRIMARY KEY IDENTITY(1,1),
    owner_id BIGINT NOT NULL,
    horse_name VARCHAR(100) NOT NULL,
    age SMALLINT,
    gender VARCHAR(10),
    breed VARCHAR(50),
    current_score DECIMAL(8,2) DEFAULT 0,
    horse_class SMALLINT DEFAULT 5,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT FK_HORSE_OWNER FOREIGN KEY (owner_id) REFERENCES dbo.[user](user_id) ON DELETE NO ACTION,
    CONSTRAINT CK_HORSE_GENDER CHECK (gender IN ('M', 'F')),
    CONSTRAINT CK_HORSE_CLASS CHECK (horse_class BETWEEN 1 AND 5),
    CONSTRAINT CK_HORSE_STATUS CHECK (status IN ('active', 'injured', 'retired', 'suspended'))
);

CREATE INDEX idx_horse_owner ON dbo.horse(owner_id);
CREATE INDEX idx_horse_status ON dbo.horse(status);

GO

-- ============================================================
-- 6. SEASON - Racing Season
-- ============================================================
CREATE TABLE dbo.season (
    season_id BIGINT PRIMARY KEY IDENTITY(1,1),
    season_name VARCHAR(120) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'draft',
    created_at DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT CK_SEASON_STATUS CHECK (status IN ('draft', 'active', 'completed', 'cancelled')),
    CONSTRAINT CK_SEASON_DATES CHECK (start_date <= end_date)
);

CREATE INDEX idx_season_status ON dbo.season(status);

GO

-- ============================================================
-- 7. RACECOURSE - Racecourse
-- ============================================================
CREATE TABLE dbo.racecourse (
    racecourse_id BIGINT PRIMARY KEY IDENTITY(1,1),
    racecourse_name VARCHAR(120) NOT NULL,
    location VARCHAR(255),
    surface_type VARCHAR(20),
    capacity INT,
    created_at DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT CK_RACECOURSE_SURFACE_TYPE CHECK (surface_type IN ('turf', 'dirt', 'synthetic'))
);

CREATE INDEX idx_racecourse_name ON dbo.racecourse(racecourse_name);

GO

-- ============================================================
-- 8. RACE_MEETING - Racing Meeting/Event Day
-- ============================================================
CREATE TABLE dbo.race_meeting (
    meeting_id BIGINT PRIMARY KEY IDENTITY(1,1),
    season_id BIGINT NOT NULL,
    racecourse_id BIGINT NOT NULL,
    meeting_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'scheduled',
    created_at DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT FK_RACE_MEETING_SEASON FOREIGN KEY (season_id) REFERENCES dbo.season(season_id) ON DELETE NO ACTION,
    CONSTRAINT FK_RACE_MEETING_RACECOURSE FOREIGN KEY (racecourse_id) REFERENCES dbo.racecourse(racecourse_id) ON DELETE NO ACTION,
    CONSTRAINT CK_RACE_MEETING_STATUS CHECK (status IN ('scheduled', 'open', 'completed', 'cancelled'))
);

CREATE INDEX idx_race_meeting_season ON dbo.race_meeting(season_id);
CREATE INDEX idx_race_meeting_racecourse ON dbo.race_meeting(racecourse_id);
CREATE INDEX idx_race_meeting_date ON dbo.race_meeting(meeting_date);

GO

-- ============================================================
-- 9. RACE_CONDITION - Race Condition/Type
-- ============================================================
CREATE TABLE dbo.race_condition (
    condition_id BIGINT PRIMARY KEY IDENTITY(1,1),
    condition_name VARCHAR(80) NOT NULL,
    distance INT NOT NULL,
    track_type VARCHAR(20),
    min_entries SMALLINT DEFAULT 3,
    max_entries SMALLINT DEFAULT 14,
    class_requirement VARCHAR(30),
    created_at DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT CK_RACE_CONDITION_TRACK_TYPE CHECK (track_type IN ('turf', 'dirt', 'synthetic')),
    CONSTRAINT CK_RACE_CONDITION_ENTRIES CHECK (min_entries <= max_entries)
);

CREATE INDEX idx_race_condition_name ON dbo.race_condition(condition_name);

GO

-- ============================================================
-- 10. RACE - Race
-- ============================================================
CREATE TABLE dbo.race (
    race_id BIGINT PRIMARY KEY IDENTITY(1,1),
    meeting_id BIGINT NOT NULL,
    condition_id BIGINT NOT NULL,
    staff_id BIGINT,
    referee_id BIGINT,
    race_name VARCHAR(120) NOT NULL,
    race_no SMALLINT,
    scheduled_time DATETIME,
    status VARCHAR(25) NOT NULL DEFAULT 'draft',
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT FK_RACE_MEETING FOREIGN KEY (meeting_id) REFERENCES dbo.race_meeting(meeting_id) ON DELETE NO ACTION,
    CONSTRAINT FK_RACE_CONDITION FOREIGN KEY (condition_id) REFERENCES dbo.race_condition(condition_id) ON DELETE NO ACTION,
    CONSTRAINT FK_RACE_STAFF FOREIGN KEY (staff_id) REFERENCES dbo.staff(staff_id) ON DELETE SET NULL,
    CONSTRAINT FK_RACE_REFEREE FOREIGN KEY (referee_id) REFERENCES dbo.referee(referee_id) ON DELETE SET NULL,
    CONSTRAINT CK_RACE_STATUS CHECK (status IN (
        'draft', 'scheduled', 'registration_open', 'registration_closed', 
        'entries_finalized', 'racecard_published', 'running', 'provisional_result', 
        'under_review', 'official_result', 'closed', 'cancelled'
    ))
);

CREATE INDEX idx_race_meeting ON dbo.race(meeting_id);
CREATE INDEX idx_race_status ON dbo.race(status);
CREATE INDEX idx_race_time ON dbo.race(scheduled_time);

GO

-- ============================================================
-- 11. PRIZE_STRUCTURES - Prize and Score Structure
-- ============================================================
CREATE TABLE dbo.prize_structures (
    prize_id BIGINT PRIMARY KEY IDENTITY(1,1),
    race_id BIGINT NOT NULL,
    position SMALLINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    score DECIMAL(6,2) NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT FK_PRIZE_STRUCTURES_RACE FOREIGN KEY (race_id) REFERENCES dbo.race(race_id) ON DELETE CASCADE,
    CONSTRAINT CK_PRIZE_STRUCTURES_POSITION CHECK (position > 0),
    CONSTRAINT CK_PRIZE_STRUCTURES_AMOUNT CHECK (amount >= 0),
    CONSTRAINT CK_PRIZE_STRUCTURES_SCORE CHECK (score >= 0),
    UNIQUE (race_id, position)
);

CREATE INDEX idx_prize_structures_race ON dbo.prize_structures(race_id);

GO

-- ============================================================
-- 12. RACE_REGISTRATION - Race Entry Registration
-- ============================================================
CREATE TABLE dbo.race_registration (
    registration_id BIGINT PRIMARY KEY IDENTITY(1,1),
    race_id BIGINT NOT NULL,
    horse_id BIGINT NOT NULL,
    submitted_by_user_id BIGINT NOT NULL,
    approved_by_staff_id BIGINT,
    registration_status VARCHAR(25) NOT NULL DEFAULT 'draft',
    submitted_at DATETIME,
    reviewed_at DATETIME,
    created_at DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT FK_RACE_REGISTRATION_RACE FOREIGN KEY (race_id) REFERENCES dbo.race(race_id) ON DELETE NO ACTION,
    CONSTRAINT FK_RACE_REGISTRATION_HORSE FOREIGN KEY (horse_id) REFERENCES dbo.horse(horse_id) ON DELETE NO ACTION,
    CONSTRAINT FK_RACE_REGISTRATION_SUBMITTER FOREIGN KEY (submitted_by_user_id) REFERENCES dbo.[user](user_id) ON DELETE NO ACTION,
    CONSTRAINT FK_RACE_REGISTRATION_APPROVER FOREIGN KEY (approved_by_staff_id) REFERENCES dbo.staff(staff_id) ON DELETE SET NULL,
    CONSTRAINT CK_RACE_REGISTRATION_STATUS CHECK (registration_status IN (
        'draft', 'submitted', 'pending_review', 'approved', 'rejected', 'withdrawn', 'converted_to_entry'
    )),
    UNIQUE (race_id, horse_id)
);

CREATE INDEX idx_race_registration_race ON dbo.race_registration(race_id);
CREATE INDEX idx_race_registration_horse ON dbo.race_registration(horse_id);
CREATE INDEX idx_race_registration_status ON dbo.race_registration(registration_status);

GO

-- ============================================================
-- 13. RACE_INVITATION - Jockey Invitation
-- ============================================================
CREATE TABLE dbo.race_invitation (
    invitation_id BIGINT PRIMARY KEY IDENTITY(1,1),
    registration_id BIGINT NOT NULL,
    jockey_id BIGINT NOT NULL,
    invitation_status VARCHAR(20) NOT NULL DEFAULT 'draft',
    sent_at DATETIME,
    responded_at DATETIME,
    message VARCHAR(500),
    created_at DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT FK_RACE_INVITATION_REGISTRATION FOREIGN KEY (registration_id) REFERENCES dbo.race_registration(registration_id) ON DELETE NO ACTION,
    CONSTRAINT FK_RACE_INVITATION_JOCKEY FOREIGN KEY (jockey_id) REFERENCES dbo.jockey(jockey_id) ON DELETE NO ACTION,
    CONSTRAINT CK_RACE_INVITATION_STATUS CHECK (invitation_status IN (
        'draft', 'sent', 'pending_response', 'accepted', 'declined', 'cancelled', 'expired', 'used'
    ))
);

CREATE INDEX idx_race_invitation_registration ON dbo.race_invitation(registration_id);
CREATE INDEX idx_race_invitation_jockey ON dbo.race_invitation(jockey_id);
CREATE INDEX idx_race_invitation_status ON dbo.race_invitation(invitation_status);

GO

-- ============================================================
-- 14. RACE_ENTRY - Official Race Entry
-- ============================================================
CREATE TABLE dbo.race_entry (
    entry_id BIGINT PRIMARY KEY IDENTITY(1,1),
    race_id BIGINT NOT NULL,
    registration_id BIGINT NOT NULL,
    invitation_id BIGINT,
    horse_id BIGINT NOT NULL,
    jockey_id BIGINT NOT NULL,
    confirmed_by_staff_id BIGINT,
    gate_number SMALLINT,
    draw_number SMALLINT,
    handicap_weight DECIMAL(5,2),
    actual_weight DECIMAL(5,2),
    weight_check_status VARCHAR(25),
    entry_status VARCHAR(20) NOT NULL DEFAULT 'declared',
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT FK_RACE_ENTRY_RACE FOREIGN KEY (race_id) REFERENCES dbo.race(race_id) ON DELETE NO ACTION,
    CONSTRAINT FK_RACE_ENTRY_REGISTRATION FOREIGN KEY (registration_id) REFERENCES dbo.race_registration(registration_id) ON DELETE NO ACTION,
    CONSTRAINT FK_RACE_ENTRY_INVITATION FOREIGN KEY (invitation_id) REFERENCES dbo.race_invitation(invitation_id) ON DELETE SET NULL,
    CONSTRAINT FK_RACE_ENTRY_HORSE FOREIGN KEY (horse_id) REFERENCES dbo.horse(horse_id) ON DELETE NO ACTION,
    CONSTRAINT FK_RACE_ENTRY_JOCKEY FOREIGN KEY (jockey_id) REFERENCES dbo.jockey(jockey_id) ON DELETE NO ACTION,
    CONSTRAINT FK_RACE_ENTRY_CONFIRMER FOREIGN KEY (confirmed_by_staff_id) REFERENCES dbo.staff(staff_id) ON DELETE SET NULL,
    CONSTRAINT CK_RACE_ENTRY_STATUS CHECK (entry_status IN (
        'declared', 'checked_in', 'ready', 'running', 'finished', 'scratched', 'dnf', 'disqualified'
    )),
    CONSTRAINT CK_RACE_ENTRY_WEIGHT_CHECK CHECK (weight_check_status IN ('passed', 'failed', 'overweight_accepted')),
    UNIQUE (registration_id),
    UNIQUE (race_id, gate_number),
    UNIQUE (race_id, horse_id)
);

CREATE INDEX idx_race_entry_race ON dbo.race_entry(race_id);
CREATE INDEX idx_race_entry_horse ON dbo.race_entry(horse_id);
CREATE INDEX idx_race_entry_jockey ON dbo.race_entry(jockey_id);
CREATE INDEX idx_race_entry_status ON dbo.race_entry(entry_status);

GO

-- ============================================================
-- 15. RACE_RESULT - Race Result
-- ============================================================
CREATE TABLE dbo.race_result (
    result_id BIGINT PRIMARY KEY IDENTITY(1,1),
    entry_id BIGINT NOT NULL,
    race_id BIGINT NOT NULL,
    position SMALLINT NOT NULL,
    finish_time TIME(3),
    result_status VARCHAR(20) NOT NULL DEFAULT 'provisional',
    score_awarded DECIMAL(6,2),
    prize_amount DECIMAL(12,2),
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT FK_RACE_RESULT_ENTRY FOREIGN KEY (entry_id) REFERENCES dbo.race_entry(entry_id) ON DELETE NO ACTION,
    CONSTRAINT FK_RACE_RESULT_RACE FOREIGN KEY (race_id) REFERENCES dbo.race(race_id) ON DELETE NO ACTION,
    CONSTRAINT CK_RACE_RESULT_STATUS CHECK (result_status IN ('provisional', 'official', 'amended', 'disqualified')),
    CONSTRAINT CK_RACE_RESULT_POSITION CHECK (position > 0),
    UNIQUE (entry_id),
    UNIQUE (race_id, position)
);

CREATE INDEX idx_race_result_race ON dbo.race_result(race_id);
CREATE INDEX idx_race_result_status ON dbo.race_result(result_status);

GO

-- ============================================================
-- 16. REFEREE_REPORT - Referee Report
-- ============================================================
CREATE TABLE dbo.referee_report (
    report_id BIGINT PRIMARY KEY IDENTITY(1,1),
    race_id BIGINT NOT NULL,
    entry_id BIGINT,
    referee_id BIGINT NOT NULL,
    report_type VARCHAR(25) NOT NULL,
    description TEXT,
    decision VARCHAR(25),
    penalty VARCHAR(255),
    report_status VARCHAR(20) NOT NULL DEFAULT 'draft',
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT FK_REFEREE_REPORT_RACE FOREIGN KEY (race_id) REFERENCES dbo.race(race_id) ON DELETE NO ACTION,
    CONSTRAINT FK_REFEREE_REPORT_ENTRY FOREIGN KEY (entry_id) REFERENCES dbo.race_entry(entry_id) ON DELETE SET NULL,
    CONSTRAINT FK_REFEREE_REPORT_REFEREE FOREIGN KEY (referee_id) REFERENCES dbo.referee(referee_id) ON DELETE NO ACTION,
    CONSTRAINT CK_REFEREE_REPORT_TYPE CHECK (report_type IN (
        'pre_race_check', 'race_review', 'violation', 'result_confirmation'
    )),
    CONSTRAINT CK_REFEREE_REPORT_DECISION CHECK (decision IN (
        'no_issue', 'warning', 'penalized', 'disqualified', 'result_confirmed'
    )),
    CONSTRAINT CK_REFEREE_REPORT_STATUS CHECK (report_status IN ('draft', 'submitted', 'closed'))
);

CREATE INDEX idx_referee_report_race ON dbo.referee_report(race_id);
CREATE INDEX idx_referee_report_entry ON dbo.referee_report(entry_id);
CREATE INDEX idx_referee_report_referee ON dbo.referee_report(referee_id);

GO

-- ============================================================
-- Verify Tables Created
-- ============================================================
-- SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES 
-- WHERE TABLE_SCHEMA = 'dbo' AND TABLE_CATALOG = 'HorseRacingMVP'
-- ORDER BY TABLE_NAME;

-- ============================================================
-- End of DDL Script
-- ============================================================
