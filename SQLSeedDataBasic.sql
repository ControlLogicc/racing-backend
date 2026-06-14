-- ============================================================
-- SAMPLE DATA CORE
-- ============================================================

INSERT INTO USERS (FullName, Email, PasswordHash, Role, Status) VALUES
    (N'Admin HRS',      'admin@hrs.com',        'hashed_pw', 'ADMIN',      'ACTIVE'),
    (N'Nguyễn Văn A',   'owner1@hrs.com',       'hashed_pw', 'OWNER',      'ACTIVE'),
    (N'Trần Thị B',     'owner2@hrs.com',       'hashed_pw', 'OWNER',      'ACTIVE'),
    (N'Lê Văn C',       'jockey1@hrs.com',      'hashed_pw', 'JOCKEY',     'ACTIVE'),
    (N'Phạm Thị D',     'jockey2@hrs.com',      'hashed_pw', 'JOCKEY',     'ACTIVE'),
    (N'Võ Văn E',       'referee1@hrs.com',     'hashed_pw', 'REFEREE',    'ACTIVE'),
    (N'Hoàng Văn G',    'spectator1@hrs.com',   'hashed_pw', 'SPECTATOR',  'ACTIVE');

INSERT INTO RACECOURSES (CourseName, Location, TrackType, Capacity) VALUES
    (N'Phú Thọ Track',     N'TP. Hồ Chí Minh', 'TURF',        50000),
    (N'Đại Nam Circuit',   N'Bình Dương',      'DIRT',        30000),
    (N'Hanoi F1 Circuit',  N'Hà Nội',          'ALL_WEATHER', 40000);

INSERT INTO SEASONS (SeasonName, StartDate, EndDate, Status) VALUES
    (N'2025/2026 Racing Season', '2025-09-01', '2026-07-31', 'ACTIVE');

INSERT INTO JOCKEYS (UserID, LicenseNo, Weight, Status) VALUES
    (4, 'JK-2024-001', 54.5, 'ACTIVE'),
    (5, 'JK-2024-002', 55.0, 'ACTIVE');

INSERT INTO REFEREES (UserID, Level, Status) VALUES
    (6, 'Senior Steward', 'ACTIVE');

INSERT INTO HORSES (OwnerID, HorseName, BirthDate, Sex, Color, Status, CurrentRating) VALUES
    (2, N'Thanh Giong',  '2020-03-15', 'MALE',    'BAY',      'ACTIVE', 85),
    (2, N'Phong Ma',     '2021-06-20', 'MALE',    'GREY',     'ACTIVE', 72),
    (3, N'Bach Ma',      '2019-11-10', 'FEMALE',  'WHITE',    'ACTIVE', 90),
    (3, N'Kim Ngua',     '2022-01-05', 'GELDING', 'CHESTNUT', 'ACTIVE', 65);

INSERT INTO RACE_MEETINGS (SeasonID, CourseID, MeetingName, RaceDate, Status) VALUES
    (1, 1, N'Vietnam National Championship 2025', '2025-11-15', 'OPEN_FOR_ENTRY');

INSERT INTO RACES (MeetingID, RaceNo, RaceName, Distance, RaceClass, PrizePool, MaxRunners, StartTime, Status) VALUES
    (1, 1, N'VNE-SPRINT', 1200, 'Class 3', 80000000, 12, '2025-11-15 14:30:00', 'OPEN_FOR_ENTRY'),
    (1, 2, N'VNE-MILE',   1600, 'Class 2', 120000000, 10, '2025-11-15 15:30:00', 'OPEN_FOR_ENTRY'),
    (1, 3, N'VNE-CUP',    2000, 'Class 1', 200000000, 8,  '2025-11-15 16:30:00', 'OPEN_FOR_ENTRY');
GO