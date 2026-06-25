IF OBJECT_ID(N'dbo.jockey_race_registration', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.jockey_race_registration (
        jockey_race_registration_id BIGINT IDENTITY(1,1) NOT NULL
            CONSTRAINT PK_JOCKEY_RACE_REGISTRATION PRIMARY KEY,
        race_id BIGINT NOT NULL,
        jockey_id BIGINT NOT NULL,
        status NVARCHAR(30) NOT NULL,
        note NVARCHAR(500) NULL,
        registered_at DATETIME2 NOT NULL,
        cancelled_at DATETIME2 NULL,
        updated_at DATETIME2 NOT NULL,
        CONSTRAINT FK_JOCKEY_RACE_REGISTRATION_RACE
            FOREIGN KEY (race_id) REFERENCES dbo.race(race_id),
        CONSTRAINT FK_JOCKEY_RACE_REGISTRATION_JOCKEY
            FOREIGN KEY (jockey_id) REFERENCES dbo.jockey(jockey_id),
        CONSTRAINT CK_JOCKEY_RACE_REGISTRATION_STATUS
            CHECK (status IN ('registered', 'cancelled'))
    );
END;
