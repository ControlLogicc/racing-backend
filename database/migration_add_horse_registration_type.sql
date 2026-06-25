-- Migration: Add horse registration type and rating verification fields
-- Run this script against HorseRacingMVP database

ALTER TABLE dbo.horse
    ADD registration_type    VARCHAR(25)   NOT NULL DEFAULT 'NEW',
        claimed_score        DECIMAL(8,2)  NULL,
        claimed_class        SMALLINT      NULL,
        rating_verified      BIT           NOT NULL DEFAULT 1,
        rating_verified_by   BIGINT        NULL,
        rating_verified_at   DATETIME      NULL;

-- rating_verified defaults to 1 (true) for all existing horses (treat as NEW)
-- rating_verified_by is FK to staff table (not enforced at DB level to keep it flexible)

PRINT 'Migration complete: horse registration type fields added.';
