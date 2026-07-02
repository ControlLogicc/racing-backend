-- Run manually against the intended local/test database after reviewing backups.
-- This script is idempotent for newly introduced columns.

IF COL_LENGTH('dbo.jockey', 'height') IS NULL
    ALTER TABLE dbo.jockey ADD height DECIMAL(5,2) NULL;
IF COL_LENGTH('dbo.jockey', 'nationality') IS NULL
    ALTER TABLE dbo.jockey ADD nationality NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.jockey', 'license_number') IS NULL
    ALTER TABLE dbo.jockey ADD license_number NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.jockey', 'achievements') IS NULL
    ALTER TABLE dbo.jockey ADD achievements NVARCHAR(MAX) NULL;
IF COL_LENGTH('dbo.jockey', 'image_url') IS NULL
    ALTER TABLE dbo.jockey ADD image_url NVARCHAR(2048) NULL;
IF COL_LENGTH('dbo.jockey', 'date_of_birth') IS NULL
    ALTER TABLE dbo.jockey ADD date_of_birth DATE NULL;

IF COL_LENGTH('dbo.horse', 'breed') IS NULL
    ALTER TABLE dbo.horse ADD breed NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.horse', 'pedigree') IS NULL
    ALTER TABLE dbo.horse ADD pedigree NVARCHAR(1000) NULL;
IF COL_LENGTH('dbo.horse', 'trainer_name') IS NULL
    ALTER TABLE dbo.horse ADD trainer_name NVARCHAR(150) NULL;
IF COL_LENGTH('dbo.horse', 'stable_name') IS NULL
    ALTER TABLE dbo.horse ADD stable_name NVARCHAR(150) NULL;
IF COL_LENGTH('dbo.horse', 'image_url') IS NULL
    ALTER TABLE dbo.horse ADD image_url NVARCHAR(2048) NULL;
IF COL_LENGTH('dbo.horse', 'date_of_birth') IS NULL
    ALTER TABLE dbo.horse ADD date_of_birth DATE NULL;
IF COL_LENGTH('dbo.horse', 'health_Note') IS NOT NULL
    ALTER TABLE dbo.horse ALTER COLUMN health_Note NVARCHAR(2048) NULL;
IF COL_LENGTH('dbo.horse', 'evidence_link') IS NOT NULL
    ALTER TABLE dbo.horse ALTER COLUMN evidence_link NVARCHAR(2048) NULL;

IF COL_LENGTH('dbo.referee_report', 'entry_id') IS NULL
BEGIN
    ALTER TABLE dbo.referee_report ADD entry_id BIGINT NULL;
    ALTER TABLE dbo.referee_report
        ADD CONSTRAINT FK_REFEREE_REPORT_ENTRY
        FOREIGN KEY (entry_id) REFERENCES dbo.race_entry(entry_id);
END;
