USE HorseRacingMVP;
GO

IF COL_LENGTH('dbo.race_entry', 'pre_check_note') IS NULL
BEGIN
    ALTER TABLE dbo.race_entry ADD pre_check_note NVARCHAR(500) NULL;
END
GO

DECLARE @constraintName SYSNAME;
DECLARE @sql NVARCHAR(MAX);

SELECT @constraintName = cc.name
FROM sys.check_constraints cc
WHERE cc.parent_object_id = OBJECT_ID('dbo.race_entry')
  AND cc.definition LIKE '%entry_status%';

IF @constraintName IS NOT NULL
BEGIN
    SET @sql = N'ALTER TABLE dbo.race_entry DROP CONSTRAINT ' + QUOTENAME(@constraintName);
    EXEC sp_executesql @sql;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID('dbo.race_entry')
      AND name = 'CK_RACE_ENTRY_STATUS'
)
BEGIN
    ALTER TABLE dbo.race_entry ADD CONSTRAINT CK_RACE_ENTRY_STATUS CHECK (entry_status IN (
        'DECLARED', 'PASSED', 'FAILED', 'WITHDRAWN',
        'declared', 'passed', 'failed', 'withdrawn',
        'ready', 'scratched', 'checked_in', 'running', 'finished', 'dnf', 'disqualified'
    ));
END
GO
