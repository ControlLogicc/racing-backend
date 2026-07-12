-- Run manually against the local/test database.
-- Adds columns for user ban features.

IF COL_LENGTH('dbo.[user]', 'account_status') IS NULL
BEGIN
    ALTER TABLE dbo.[user] ADD account_status NVARCHAR(50) NOT NULL
        CONSTRAINT DF_USER_ACCOUNT_STATUS DEFAULT 'active';
END;

IF COL_LENGTH('dbo.[user]', 'banned_reason') IS NULL
BEGIN
    ALTER TABLE dbo.[user] ADD banned_reason NVARCHAR(500) NULL;
END;

IF COL_LENGTH('dbo.[user]', 'banned_at') IS NULL
BEGIN
    ALTER TABLE dbo.[user] ADD banned_at DATETIME2 NULL;
END;

IF COL_LENGTH('dbo.[user]', 'banned_by') IS NULL
BEGIN
    ALTER TABLE dbo.[user] ADD banned_by BIGINT NULL;
END;
