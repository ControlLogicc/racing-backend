-- ============================================================
-- Migration: Update Check Constraint on race_registration.registration_status
-- Reason: Schema drift - database was using outdated constraint restricted to ('pending', 'approved', 'rejected')
-- Date: 2026-06-23
-- ============================================================

USE HorseRacingMVP;
GO

ALTER TABLE dbo.race_registration DROP CONSTRAINT CK_RACE_REGISTRATION_STATUS;
GO

ALTER TABLE dbo.race_registration ADD CONSTRAINT CK_RACE_REGISTRATION_STATUS CHECK (registration_status IN (
    'draft', 'submitted', 'pending_review', 'approved', 'rejected', 'withdrawn', 'converted_to_entry'
));
GO
