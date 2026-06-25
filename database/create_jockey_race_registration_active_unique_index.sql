IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'UX_JOCKEY_RACE_REGISTRATION_ACTIVE'
      AND object_id = OBJECT_ID(N'dbo.jockey_race_registration')
)
BEGIN
    CREATE UNIQUE INDEX UX_JOCKEY_RACE_REGISTRATION_ACTIVE
        ON dbo.jockey_race_registration(race_id, jockey_id)
        WHERE status = N'registered';
END;
