# API Inventory Report

Project: `D:\FPTU\Ki5\SWP\FinalSWP`  
Audit time: 2026-06-22  
Scope: inspect controllers, security rules, DTOs, Swagger/OpenAPI. No source code or database schema changes were made.

## Git Snapshot

| Check | Result |
|---|---|
| Branch | `main` |
| Initial git status | Dirty before this audit: existing modified source/test files plus new Race Result files from prior work. This audit only adds report markdown files. |
| Last commits | `86e6ea2 Merge pull request #13 from ControlLogicc/Feature/PrizeAndScore`; `ff0161f prizeandscore`; `a7ce9a6 Merge pull request #12 from ControlLogicc/feature/referee-report-api`; `3f74fa9 feat: add referee report API`; `00c6c1f fix` |

## Build Baseline

| Command | Result |
|---|---|
| `./mvnw.cmd -DskipTests compile` | BUILD SUCCESS |
| `./mvnw.cmd test` | BUILD SUCCESS, Tests run: 130, Failures: 0, Errors: 0, Skipped: 0 |

## Security Summary

| Route pattern | Access rule observed |
|---|---|
| `/api/auth/**` | Public |
| `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` | Public |
| `/api/owner/**` | OWNER |
| `/api/jockey/**` | JOCKEY |
| `GET /api/staff` | STAFF |
| `POST /api/staff`, `PUT /api/staff/**` | ADMIN |
| `/api/admin/**` | ADMIN |
| `/api/race-management/**` | ADMIN or STAFF |
| `/api/referees/**`, `/api/registrations/**`, `/api/invitations/**` | Authenticated, with method-level role checks in controllers/services |
| Other `/api/**` endpoints such as `/api/health`, `/api/db-test`, `/api/races/open` | Authenticated by fallback rule unless explicitly permitted elsewhere |

## Controller Inventory

| Area | Controller |
|---|---|
| Auth | `AuthController` |
| Admin Users | `AdminUserController` |
| Admin Seasons | `AdminSeasonController` |
| Admin Racecourses | `AdminRacecourseController` |
| Admin Race Meetings | `AdminRaceMeetingController` |
| Admin Race Conditions | `AdminRaceConditionController` |
| Admin Races | `AdminRaceController` |
| Admin Prize Structures | `AdminPrizeStructureController` |
| Admin Horses | `AdminHorseController` |
| Admin Jockeys | `AdminJockeyController` |
| Owner Horse | `OwnerHorseController` |
| Staff | `StaffController` |
| Jockey Profile/List | `JockeyProfileController`, `JockeyController` |
| Referee Profile | `RefereeController` |
| Race Registration | `RaceRegistrationController` |
| Race Invitation | `RaceInvitationController` |
| Race Management Status | `RaceManagementController` |
| Referee Report | `RefereeReportController` |
| Race Result | `RaceResultController` |
| Public/Open Race and Health | `RaceController` |

## Swagger/OpenAPI Endpoints

Swagger `/v3/api-docs` returned 58 operations.

| Method | URL | Main role/access | Notes |
|---|---|---|---|
| POST | `/api/auth/login` | Public | Login |
| POST | `/api/auth/register` | Public | Register external user |
| GET | `/api/health` | Authenticated by current security fallback | Returned 401 without token in HTTP audit |
| GET | `/api/db-test` | Authenticated by current security fallback | Returned 401 without token in HTTP audit |
| POST | `/api/admin/users` | ADMIN | Create internal user |
| GET | `/api/admin/seasons` | ADMIN | List seasons |
| POST | `/api/admin/seasons` | ADMIN | Create season |
| GET | `/api/admin/seasons/{id}` | ADMIN | Get season |
| PUT | `/api/admin/seasons/{id}` | ADMIN | Update season |
| DELETE | `/api/admin/seasons/{id}` | ADMIN | Delete season |
| GET | `/api/admin/racecourses` | ADMIN | List racecourses |
| POST | `/api/admin/racecourses` | ADMIN | Create racecourse |
| GET | `/api/admin/racecourses/{id}` | ADMIN | Get racecourse |
| PUT | `/api/admin/racecourses/{id}` | ADMIN | Update racecourse |
| DELETE | `/api/admin/racecourses/{id}` | ADMIN | Delete racecourse |
| GET | `/api/admin/race-meetings` | ADMIN | List meetings |
| POST | `/api/admin/race-meetings` | ADMIN | Create meeting |
| GET | `/api/admin/race-meetings/{id}` | ADMIN | Get meeting |
| PUT | `/api/admin/race-meetings/{id}` | ADMIN | Update meeting |
| DELETE | `/api/admin/race-meetings/{id}` | ADMIN | Delete meeting |
| GET | `/api/admin/race-conditions` | ADMIN | List race conditions |
| POST | `/api/admin/race-conditions` | ADMIN | Create race condition |
| GET | `/api/admin/race-conditions/{id}` | ADMIN | Get race condition |
| PUT | `/api/admin/race-conditions/{id}` | ADMIN | Update race condition |
| DELETE | `/api/admin/race-conditions/{id}` | ADMIN | Delete race condition |
| GET | `/api/admin/races` | ADMIN | List races |
| POST | `/api/admin/races` | ADMIN | Create race |
| GET | `/api/admin/races/open` | ADMIN | Admin open races |
| GET | `/api/admin/races/{id}` | ADMIN | Get race |
| PUT | `/api/admin/races/{id}` | ADMIN | Update race |
| DELETE | `/api/admin/races/{id}` | ADMIN | Delete race |
| PUT | `/api/admin/races/{id}/assign-staff` | ADMIN | Assign staff |
| POST | `/api/admin/races/{raceId}/recalculate-prizes` | ADMIN | Recalculate prize/score |
| PATCH | `/api/race-management/races/{raceId}/status` | ADMIN or STAFF | Race lifecycle status transition |
| GET | `/api/races/open` | Authenticated by current security fallback | Expected public in task, actual 401 without token |
| GET | `/api/admin/prize-structures` | ADMIN | List prize structures |
| POST | `/api/admin/prize-structures` | ADMIN | Create prize structure |
| GET | `/api/admin/prize-structures/{id}` | ADMIN | Get prize structure |
| PUT | `/api/admin/prize-structures/{id}` | ADMIN | Update prize structure |
| DELETE | `/api/admin/prize-structures/{id}` | ADMIN | Delete prize structure |
| GET | `/api/owner/horses` | OWNER | Owner list horses |
| POST | `/api/owner/horses` | OWNER | Owner create horse |
| GET | `/api/owner/horses/{id}` | OWNER | Owner get horse |
| PUT | `/api/owner/horses/{id}` | OWNER | Owner update horse |
| DELETE | `/api/owner/horses/{id}` | OWNER | Owner delete horse |
| GET | `/api/admin/horses` | ADMIN | Admin list horses |
| GET | `/api/admin/horses/{id}` | ADMIN | Admin get horse |
| PUT | `/api/admin/horses/{id}` | ADMIN | Admin update horse |
| DELETE | `/api/admin/horses/{id}` | ADMIN | Admin delete horse |
| GET | `/api/admin/jockeys` | ADMIN | Admin list jockeys |
| POST | `/api/admin/jockeys` | ADMIN | Admin create jockey profile |
| GET | `/api/admin/jockeys/{id}` | ADMIN | Admin get jockey |
| PUT | `/api/admin/jockeys/{id}` | ADMIN | Admin update jockey |
| DELETE | `/api/admin/jockeys/{id}` | ADMIN | Admin delete jockey |
| GET | `/api/jockey/profile` | JOCKEY | Current jockey profile |
| PUT | `/api/jockey/profile` | JOCKEY | Update current jockey profile |
| GET | `/api/jockeys` | Authenticated | List jockeys |
| PUT | `/api/jockeys/{id}/weight` | JOCKEY or role-checked service | Update jockey weight |
| GET | `/api/jockeys/available-races` | JOCKEY | Available races |
| GET | `/api/staff` | STAFF | Current staff profile |
| POST | `/api/staff` | ADMIN | Create staff profile |
| PUT | `/api/staff/{id}` | ADMIN | Update staff profile |
| GET | `/api/staff/races` | STAFF | Staff assigned races |
| GET | `/api/staff/registrations` | STAFF | Staff registration queue |
| GET | `/api/referees` | Authenticated/Admin intended | List referees |
| POST | `/api/referees` | ADMIN intended | Create referee profile |
| PUT | `/api/referees/{id}` | ADMIN intended | Update referee profile |
| POST | `/api/registrations` | OWNER | Owner submits race registration |
| GET | `/api/registrations/{raceId}` | ADMIN or STAFF | List registrations for race |
| PUT | `/api/registrations/{registrationId}/approve` | ADMIN or STAFF | Approve registration |
| PUT | `/api/registrations/{registrationId}/reject` | ADMIN or STAFF | Reject registration |
| GET | `/api/invitations` | OWNER or JOCKEY | List invitations |
| POST | `/api/invitations` | OWNER | Create invitation |
| PUT | `/api/invitations/{invitationId}/accept` | JOCKEY | Accept invitation |
| PUT | `/api/invitations/{invitationId}/decline` | JOCKEY | Decline invitation |
| GET | `/api/reports` | ADMIN | List referee reports |
| POST | `/api/reports` | REFEREE | Submit referee report |
| GET | `/api/reports/{raceId}` | ADMIN/assigned REFEREE/assigned STAFF intended | Reports by race |
| PUT | `/api/reports/{id}` | REFEREE owner or ADMIN intended | Update report |
| POST | `/api/results` | ADMIN or STAFF | Create race result from entryId |
| GET | `/api/results/{raceId}` | Authenticated | Results by race |
| GET | `/api/results/horse/{horseId}` | Authenticated | Results by horse |

## Missing/Important Finding

No `RaceEntryController` or `/api/...entry...` endpoint was found in controller scan or Swagger. Current code has `RaceEntry` model and `RaceEntryRepository`, and Race Result needs `entryId`, but the audited HTTP API surface does not expose a way to create/list a race entry. `PUT /api/invitations/{id}/accept` currently returns an accepted invitation response; it did not expose or create an entry through the API response during audit.
