-- Migration: Resize horse.health_Note to store PREVIOUSLY_REGISTERED rating proof links.
-- Run this script manually against HorseRacingMVP.

ALTER TABLE dbo.horse ALTER COLUMN health_Note NVARCHAR(2048) NULL;

PRINT 'Migration complete: horse.health_Note resized to NVARCHAR(2048).';
