# API Test Report

Project: `D:\FPTU\Ki5\SWP\FinalSWP`  
Audit time: 2026-06-22  
Scope: compile, full test, Swagger check, HTTP API smoke/integration checks through localhost. No source code or database/schema changes were made.

## Build/Test Result

| Command | Result |
|---|---|
| `./mvnw.cmd -DskipTests compile` | BUILD SUCCESS |
| `./mvnw.cmd test` | BUILD SUCCESS. Tests run: 130, Failures: 0, Errors: 0, Skipped: 0 |

## HTTP Summary

| Metric | Count |
|---|---:|
| HTTP checks executed | 55 |
| PASS | 51 |
| FAIL/Needs review | 4 |
| BLOCKED/Partial workflow | Race Result happy path blocked because no Race Entry API/entryId is exposed |

## Created API Test Data

These records were created through API calls only. No direct database write was used.

| Data | ID / Value |
|---|---|
| Spectator user | `userId=4002`, `audit_spectator_20260622145033@example.com` |
| Referee user | `userId=4003`, `audit_referee_20260622145033@example.com` |
| Referee profile | `refereeId=720` |
| Season | `seasonId=774` |
| Racecourse | `racecourseId=774` |
| Race meeting | `meetingId=769` |
| Race condition | `conditionId=768` |
| Race | `raceId=768` |
| Prize structure | `prizeId=255` |
| Horse | `horseId=845` |
| Race registration | `registrationId=653` |
| Race invitation | `invitationId=185` |
| Referee report | `reportId=51` |

## Test Table

| Step | API | Token role | Expected | Actual | PASS/FAIL | Note |
|---|---|---|---|---|---|---|
| AUTH-01 | POST `/api/auth/login` | ADMIN | 200 | 200 | PASS | Admin login OK. |
| AUTH-02 | POST `/api/auth/login` | STAFF | 200 | 200 | PASS | Staff login OK. |
| AUTH-03 | POST `/api/auth/login` | OWNER | 200 | 200 | PASS | Owner login OK. |
| AUTH-04 | POST `/api/auth/login` | JOCKEY | 200 | 200 | PASS | Jockey login OK. |
| AUTH-05 | POST `/api/auth/login` | PUBLIC | 401 expected by audit | 400 | FAIL | Wrong password returns `400 {"error":"Invalid email or password"}` instead of 401. Not a crash, but different error contract. |
| AUTH-06 | POST `/api/auth/register` | PUBLIC | 200/201 | 200 | PASS | Created spectator test user. |
| AUTH-07 | POST `/api/auth/login` | REFEREE | 200 | 200 | PASS | Newly created referee login OK. |
| DOC-01 | GET `/v3/api-docs` | PUBLIC | 200 | 200 | PASS | OpenAPI JSON available. |
| DOC-02 | GET `/swagger-ui/index.html` | PUBLIC | 200 | 200 | PASS | Swagger UI available. |
| HEALTH-01 | GET `/api/health` | PUBLIC | 200 | 401 | FAIL | Current SecurityConfig fallback requires auth. If health should be public, add permit rule later. |
| HEALTH-02 | GET `/api/db-test` | PUBLIC | 200 | 401 | FAIL | Current SecurityConfig fallback requires auth. |
| SEC-01 | GET `/api/admin/seasons` | No token | 401 | 401 | PASS | Admin endpoint protected. |
| SEC-02 | GET `/api/admin/seasons` | OWNER | 403 | 403 | PASS | Wrong role forbidden. |
| DATA-01 | POST `/api/admin/users` | ADMIN | 200/201 | 200 | PASS | Created REFEREE user by API. |
| DATA-02 | POST `/api/referees` | ADMIN | 200/201 | 200 | PASS | Created referee profile by API. |
| STAFF-01 | GET `/api/staff` | STAFF | 200 | 200 | PASS | Current staff profile returned `staffId=93`. |
| JOCKEY-01 | GET `/api/jockey/profile` | JOCKEY | 200 | 200 | PASS | Current jockey profile returned `jockeyId=4`. |
| ADMIN-LIST-01 | GET `/api/admin/seasons` | ADMIN | 200 | 200 | PASS | Season list OK. |
| ADMIN-LIST-02 | GET `/api/admin/racecourses` | ADMIN | 200 | 200 | PASS | Racecourse list OK. |
| ADMIN-LIST-03 | GET `/api/admin/race-meetings` | ADMIN | 200 | 200 | PASS | Race meeting list OK. |
| ADMIN-LIST-04 | GET `/api/admin/race-conditions` | ADMIN | 200 | 200 | PASS | Race condition list OK. |
| ADMIN-LIST-05 | GET `/api/admin/prize-structures` | ADMIN | 200 | 200 | PASS | Prize list OK. |
| ADMIN-LIST-06 | GET `/api/admin/races` | ADMIN | 200 | 200 | PASS | Race list OK. |
| ADMIN-LIST-07 | GET `/api/admin/races/open` | ADMIN | 200 | 200 | PASS | Admin open races OK. |
| PUBLIC-01 | GET `/api/races/open` | PUBLIC | 200 | 401 | FAIL | Endpoint exists but is not public under current SecurityConfig fallback. |
| NEG-01 | GET `/api/admin/seasons/999999999` | ADMIN | 404 | 404 | PASS | Not found behavior OK. |
| NEG-02 | POST `/api/admin/seasons` | ADMIN | 400 | 400 | PASS | Validation error OK. |
| FLOW-01 | POST `/api/admin/seasons` | ADMIN | 200/201 | 200 | PASS | Created season `774`. |
| FLOW-02 | POST `/api/admin/racecourses` | ADMIN | 200/201 | 200 | PASS | Created racecourse `774`. |
| FLOW-03 | POST `/api/admin/race-meetings` | ADMIN | 200/201 | 200 | PASS | Created meeting `769`. |
| FLOW-04 | POST `/api/admin/race-conditions` | ADMIN | 200/201 | 200 | PASS | Created condition `768`. |
| FLOW-05 | POST `/api/admin/races` | ADMIN | 200/201 | 200 | PASS | Created race `768`, assigned `staffId=93`, `refereeId=720`. |
| FLOW-06 | PATCH `/api/race-management/races/768/status` | ADMIN | 200 | 200 | PASS | `DRAFT -> SCHEDULED` OK. |
| FLOW-07 | PATCH `/api/race-management/races/768/status` | ADMIN | 200 | 200 | PASS | `SCHEDULED -> OPEN_FOR_ENTRY` OK. |
| FLOW-08 | POST `/api/admin/prize-structures` | ADMIN | 200/201 | 200 | PASS | Created position 1 prize. |
| FLOW-09 | POST `/api/owner/horses` | OWNER | 200/201 | 200 | PASS | Created horse `845`. |
| OWNER-01 | GET `/api/owner/horses` | OWNER | 200 | 200 | PASS | Owner horse list OK. |
| FLOW-10 | POST `/api/registrations` | OWNER | 201 | 201 | PASS | Created registration `653`. |
| FLOW-11 | GET `/api/registrations/768` | STAFF | 200 | 200 | PASS | Assigned staff can view registration. |
| FLOW-12 | PUT `/api/registrations/653/approve` | STAFF | 200 | 200 | PASS | Staff approved registration. |
| FLOW-13 | POST `/api/invitations` | OWNER | 200/201 | 200 | PASS | Created invitation `185`. |
| INV-01 | GET `/api/invitations` | JOCKEY | 200 | 200 | PASS | Jockey invitation list OK. |
| FLOW-14 | PUT `/api/invitations/185/accept` | JOCKEY | 200 | 200 | PASS | Jockey accepted invitation. Response did not expose `entryId`. |
| REPORT-01 | POST `/api/reports` | REFEREE | 200/201 | 200 | PASS | Assigned referee submitted report `51`. |
| REPORT-02 | GET `/api/reports` | ADMIN | 200 | 200 | PASS | Admin report list OK. |
| REPORT-03 | GET `/api/reports/768` | REFEREE | 200 | 200 | PASS | Assigned referee can view race reports. |
| REPORT-04 | PUT `/api/reports/51` | REFEREE | 200 | 200 | PASS | Referee updated own report. |
| REPORT-NEG-01 | POST `/api/reports` | No token | 401 | 401 | PASS | Report submit protected. |
| REPORT-NEG-02 | POST `/api/reports` | OWNER | 403 | 403 | PASS | Wrong role forbidden. |
| REPORT-NEG-03 | GET `/api/reports` | REFEREE | 403 | 403 | PASS | Admin-only report list enforced. |
| RESULT-NEG-01 | POST `/api/results` | No token | 401 | 401 | PASS | Result create protected. |
| RESULT-NEG-02 | POST `/api/results` | OWNER | 403 | 403 | PASS | Owner cannot create result. |
| RESULT-NEG-03 | POST `/api/results` | ADMIN | 404 | 404 | PASS | Non-existing `entryId` returns `Race entry not found`. |
| RESULT-GET-01 | GET `/api/results/768` | ADMIN | 200 | 200 | PASS | Results by race returns empty list. |
| RESULT-GET-02 | GET `/api/results/horse/845` | OWNER | 200 | 200 | PASS | Results by horse returns empty list. |
| PRIZE-01 | POST `/api/admin/races/768/recalculate-prizes` | ADMIN | 200/400/404 | 400 | PASS/EXPECTED PARTIAL | Returned `Race is not OFFICIAL`, expected because no result/official flow was reached. |

## End-To-End Flow Status

| Flow segment | Status | Evidence |
|---|---|---|
| Admin login and admin CRUD foundation | PASS | Login and create/list season, racecourse, meeting, condition, race, prize all OK. |
| Race staff/referee assignment | PASS | Race `768` created with `staffId=93`, `refereeId=720`. |
| Race status open registration | PASS | `DRAFT -> SCHEDULED -> OPEN_FOR_ENTRY` both returned 200. |
| Owner horse and registration | PASS | Horse `845`, registration `653`. |
| Staff review registration | PASS | Staff listed and approved registration. |
| Race invitation | PASS | Owner created invitation `185`, jockey listed and accepted it. |
| Referee report | PASS | Assigned referee created/viewed/updated report `51`. |
| Race Result happy path | BLOCKED/PARTIAL | `/api/results` requires `entryId`, but no Race Entry controller/Swagger endpoint was found, and invitation accept did not expose or create an entry through response. Negative/error paths pass. |
| Prize recalculation | PARTIAL | Endpoint reachable, but current race not OFFICIAL and no result exists, so response 400 is logical. |

## Issues / Notes For Next Fix Round

1. `GET /api/races/open` exists in Swagger but returns 401 without token. If this is intended as Spectator/Public API, SecurityConfig needs a public permit rule later.
2. `GET /api/health` and `GET /api/db-test` also return 401 without token. If health checks should be public, they need explicit permit rules later.
3. Wrong password login currently returns 400, not 401. Decide whether this is acceptable API contract.
4. Race Result happy path cannot be completed from HTTP-only API surface because `entryId` is required and no Race Entry API is exposed in Swagger/controllers.
5. `PUT /api/invitations/{id}/accept` changes invitation to accepted, but does not expose `entryId`; during source scan it also appeared to only update invitation status.

## Conclusion

Compile and full automated tests are green. Most core APIs are reachable and the main workflow passes up to race invitation acceptance and referee reporting. The main blocker before completing a true Race Result end-to-end flow is the missing/exposed Race Entry step or missing `entryId` in an API response.
