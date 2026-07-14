-- Drop the check constraint CK_PRIZE_STRUCTURES_SCORE in table prize_structures to allow negative scores
IF EXISTS (SELECT * FROM sys.check_constraints WHERE object_id = OBJECT_ID(N'[dbo].[CK_PRIZE_STRUCTURES_SCORE]') AND parent_object_id = OBJECT_ID(N'[dbo].[prize_structures]'))
BEGIN
    ALTER TABLE dbo.prize_structures DROP CONSTRAINT CK_PRIZE_STRUCTURES_SCORE;
    PRINT 'Dropped CHECK constraint CK_PRIZE_STRUCTURES_SCORE';
END
ELSE
BEGIN
    PRINT 'CHECK constraint CK_PRIZE_STRUCTURES_SCORE not found';
END
GO
