-- Drop the unique constraint on (race_id, horse_id) in the race_registration table
DECLARE @ConstraintName NVARCHAR(256);

SELECT @ConstraintName = dc.name
FROM sys.key_constraints dc
INNER JOIN sys.index_columns ic ON dc.parent_object_id = ic.object_id AND dc.unique_index_id = ic.index_id
INNER JOIN sys.columns c ON ic.object_id = c.object_id AND ic.column_id = c.column_id
WHERE dc.parent_object_id = OBJECT_ID('dbo.race_registration')
  AND dc.type = 'UQ'
GROUP BY dc.name
HAVING COUNT(DISTINCT c.name) = 2
   AND SUM(CASE WHEN c.name IN ('race_id', 'horse_id') THEN 1 ELSE 0 END) = 2;

IF @ConstraintName IS NOT NULL
BEGIN
    DECLARE @SQL NVARCHAR(MAX) = 'ALTER TABLE dbo.race_registration DROP CONSTRAINT ' + QUOTENAME(@ConstraintName);
    EXEC sp_executesql @SQL;
    PRINT 'Dropped unique constraint: ' + @ConstraintName;
END
ELSE
BEGIN
    PRINT 'No matching unique constraint found';
END
GO
