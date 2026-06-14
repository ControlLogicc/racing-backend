-- ============================================================
-- HORSE RACING MANAGEMENT SYSTEM
-- CORE DATABASE FOR CODING FIRST
-- Scope: Auth + Role + Admin Race Setup + Owner Horse Flow
-- ============================================================

USE HorsesRacing;
GO

-- ============================================================
-- 1. USERS
-- ============================================================
CREATE TABLE USERS (
    UserID          INT             IDENTITY(1,1)   PRIMARY KEY,
    FullName        NVARCHAR(100)   NOT NULL,
    Email           NVARCHAR(150)   NOT NULL        UNIQUE,
    PasswordHash    NVARCHAR(255)   NOT NULL,
    Role            NVARCHAR(20)    NOT NULL
        CONSTRAINT CHK_USERS_Role CHECK (Role IN (
            'ADMIN','OWNER','JOCKEY','REFEREE','HANDICAPPER','SPECTATOR'
        )),
    Status          NVARCHAR(20)    NOT NULL        DEFAULT 'ACTIVE'
        CONSTRAINT CHK_USERS_Status CHECK (Status IN (
            'ACTIVE','SUSPENDED','INACTIVE'
        )),
    CreatedAt       DATETIME        NOT NULL        DEFAULT GETDATE()
);
GO

-- ============================================================
-- 2. RACECOURSES
-- ============================================================
CREATE TABLE RACECOURSES (
    CourseID        INT             IDENTITY(1,1)   PRIMARY KEY,
    CourseName      NVARCHAR(100)   NOT NULL,
    Location        NVARCHAR(255)   NULL,
    TrackType       NVARCHAR(20)    NULL
        CONSTRAINT CHK_RACECOURSES_TrackType CHECK (TrackType IN (
            'TURF','DIRT','ALL_WEATHER'
        )),
    Capacity        INT             NULL
);
GO

-- ============================================================
-- 3. SEASONS
-- ============================================================
CREATE TABLE SEASONS (
    SeasonID        INT             IDENTITY(1,1)   PRIMARY KEY,
    SeasonName      NVARCHAR(100)   NOT NULL,
    StartDate       DATE            NOT NULL,
    EndDate         DATE            NOT NULL,
    Status          NVARCHAR(20)    NOT NULL        DEFAULT 'DRAFT'
        CONSTRAINT CHK_SEASONS_Status CHECK (Status IN (
            'DRAFT','ACTIVE','COMPLETED','CANCELLED'
        )),
    CONSTRAINT CHK_SEASONS_Dates CHECK (EndDate > StartDate)
);
GO

-- ============================================================
-- 4. HORSES
-- ============================================================
CREATE TABLE HORSES (
    HorseID         INT             IDENTITY(1,1)   PRIMARY KEY,
    OwnerID         INT             NOT NULL
        CONSTRAINT FK_HORSES_Owner FOREIGN KEY REFERENCES USERS(UserID),
    HorseName       NVARCHAR(100)   NOT NULL,
    BirthDate       DATE            NULL,
    Sex             NVARCHAR(10)    NULL
        CONSTRAINT CHK_HORSES_Sex CHECK (Sex IN ('MALE','FEMALE','GELDING')),
    Color           NVARCHAR(50)    NULL,
    Status          NVARCHAR(20)    NOT NULL        DEFAULT 'ACTIVE'
        CONSTRAINT CHK_HORSES_Status CHECK (Status IN (
            'ACTIVE','INJURED','RETIRED','SUSPENDED'
        )),
    CurrentRating   INT             NULL,
    HealthNote      NVARCHAR(MAX)   NULL
);
GO

-- ============================================================
-- 5. JOCKEYS
-- ============================================================
CREATE TABLE JOCKEYS (
    JockeyID        INT             IDENTITY(1,1)   PRIMARY KEY,
    UserID          INT             NOT NULL        UNIQUE
        CONSTRAINT FK_JOCKEYS_User FOREIGN KEY REFERENCES USERS(UserID),
    LicenseNo       NVARCHAR(50)    NULL,
    Weight          DECIMAL(5,2)    NULL,
    Status          NVARCHAR(20)    NOT NULL        DEFAULT 'ACTIVE'
        CONSTRAINT CHK_JOCKEYS_Status CHECK (Status IN (
            'ACTIVE','SUSPENDED','INACTIVE'
        ))
);
GO

-- ============================================================
-- 6. REFEREES
-- ============================================================
CREATE TABLE REFEREES (
    RefereeID       INT             IDENTITY(1,1)   PRIMARY KEY,
    UserID          INT             NOT NULL        UNIQUE
        CONSTRAINT FK_REFEREES_User FOREIGN KEY REFERENCES USERS(UserID),
    Level           NVARCHAR(50)    NULL,
    Status          NVARCHAR(20)    NOT NULL        DEFAULT 'ACTIVE'
        CONSTRAINT CHK_REFEREES_Status CHECK (Status IN ('ACTIVE','INACTIVE'))
);
GO

-- ============================================================
-- 7. RACE_MEETINGS
-- ============================================================
CREATE TABLE RACE_MEETINGS (
    MeetingID       INT             IDENTITY(1,1)   PRIMARY KEY,
    SeasonID        INT             NOT NULL
        CONSTRAINT FK_MEETINGS_Season FOREIGN KEY REFERENCES SEASONS(SeasonID),
    CourseID        INT             NOT NULL
        CONSTRAINT FK_MEETINGS_Course FOREIGN KEY REFERENCES RACECOURSES(CourseID),
    MeetingName     NVARCHAR(150)   NOT NULL,
    RaceDate        DATE            NOT NULL,
    StartTime       DATETIME        NULL,
    EndTime         DATETIME        NULL,
    Weather         NVARCHAR(100)   NULL,
    TrackCondition  NVARCHAR(20)    NULL
        CONSTRAINT CHK_MEETINGS_TrackCondition CHECK (TrackCondition IN (
            'GOOD','YIELDING','WET','FAST','SLOW'
        )),
    Status          NVARCHAR(30)    NOT NULL        DEFAULT 'DRAFT'
        CONSTRAINT CHK_MEETINGS_Status CHECK (Status IN (
            'DRAFT','SCHEDULED','OPEN_FOR_ENTRY','DECLARED',
            'ONGOING','COMPLETED','CLOSED','CANCELLED'
        ))
);
GO

-- ============================================================
-- 8. RACES
-- ============================================================
CREATE TABLE RACES (
    RaceID              INT             IDENTITY(1,1)   PRIMARY KEY,
    MeetingID           INT             NOT NULL
        CONSTRAINT FK_RACES_Meeting FOREIGN KEY REFERENCES RACE_MEETINGS(MeetingID),
    RaceNo              INT             NOT NULL,
    RaceName            NVARCHAR(150)   NOT NULL,
    Distance            INT             NOT NULL,
    RaceClass           NVARCHAR(50)    NULL,
    PrizePool           DECIMAL(12,2)   NOT NULL        DEFAULT 0,
    MaxRunners          INT             NOT NULL        DEFAULT 14,
    EntryDeadline       DATETIME        NULL,
    DeclarationDeadline DATETIME        NULL,
    StartTime           DATETIME        NOT NULL,
    Status              NVARCHAR(30)    NOT NULL        DEFAULT 'DRAFT'
        CONSTRAINT CHK_RACES_Status CHECK (Status IN (
            'DRAFT','OPEN_FOR_ENTRY','ENTRY_CLOSED','DECLARED',
            'READY','RUNNING','PENDING_REVIEW','OFFICIAL','COMPLETED','CANCELLED'
        )),
    CONSTRAINT UQ_RACES_MeetingNo UNIQUE (MeetingID, RaceNo)
);
GO

-- ============================================================
-- 9. RACE_REGISTRATIONS
-- ============================================================
CREATE TABLE RACE_REGISTRATIONS (
    RegistrationID      INT             IDENTITY(1,1)   PRIMARY KEY,
    RaceID              INT             NOT NULL
        CONSTRAINT FK_REG_Race FOREIGN KEY REFERENCES RACES(RaceID),
    HorseID             INT             NOT NULL
        CONSTRAINT FK_REG_Horse FOREIGN KEY REFERENCES HORSES(HorseID),
    JockeyID            INT             NULL
        CONSTRAINT FK_REG_Jockey FOREIGN KEY REFERENCES JOCKEYS(JockeyID),
    RegisteredByOwnerID INT             NOT NULL
        CONSTRAINT FK_REG_Owner FOREIGN KEY REFERENCES USERS(UserID),
    Status              NVARCHAR(20)    NOT NULL        DEFAULT 'PENDING'
        CONSTRAINT CHK_REG_Status CHECK (Status IN (
            'PENDING','APPROVED','REJECTED','WITHDRAWN',
            'DECLARED','CHECKED_IN','NON_RUNNER','FINISHED'
        )),
    RegisteredAt        DATETIME        NOT NULL        DEFAULT GETDATE(),
    ApprovedBy          INT             NULL
        CONSTRAINT FK_REG_ApprovedBy FOREIGN KEY REFERENCES USERS(UserID),
    ApprovedAt          DATETIME        NULL,
    OwnerConfirmedAt    DATETIME        NULL,
    JockeyConfirmedAt   DATETIME        NULL,
    WithdrawReason      NVARCHAR(MAX)   NULL,
    OwnerReadinessNote  NVARCHAR(MAX)   NULL,
    CONSTRAINT UQ_REG_RaceHorse UNIQUE (RaceID, HorseID)
);
GO

-- ============================================================
-- INDEXES CORE
-- ============================================================
CREATE INDEX IDX_HORSES_OwnerID       ON HORSES(OwnerID);
CREATE INDEX IDX_RACES_MeetingID      ON RACES(MeetingID);
CREATE INDEX IDX_RACES_Status         ON RACES(Status);
CREATE INDEX IDX_REG_RaceID           ON RACE_REGISTRATIONS(RaceID);
CREATE INDEX IDX_REG_HorseID          ON RACE_REGISTRATIONS(HorseID);
CREATE INDEX IDX_REG_JockeyID         ON RACE_REGISTRATIONS(JockeyID);
CREATE INDEX IDX_REG_Status           ON RACE_REGISTRATIONS(Status);
GO