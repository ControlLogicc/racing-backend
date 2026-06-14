# Postman Test Guide - Auth and User Management

Base URL:

```text
http://localhost:8080
```

Recommended Postman environment variables:

- `baseUrl`: `http://localhost:8080`
- `adminToken`: JWT token returned by admin login, without `Bearer `
- `ownerToken`: JWT token returned by horse owner login, without `Bearer `
- `userId`: user id used for detail/update/status tests

## A. Register API

Endpoint:

```http
POST {{baseUrl}}/api/auth/register
```

Cases:

1. Register `HORSE_OWNER` successfully.
2. Register `JOCKEY` successfully.
3. Register `SPECTATOR` successfully.
4. Register `ADMIN` by public register should fail with `400`.
5. Register `RACE_REFEREE` by public register should fail with `400`.
6. Duplicate email should fail with `400`.
7. Missing email or password should fail.

Example body:

```json
{
  "fullName": "Owner A",
  "email": "owner@gmail.com",
  "password": "123456",
  "role": "HORSE_OWNER"
}
```

## B. Login API

Endpoint:

```http
POST {{baseUrl}}/api/auth/login
```

Cases:

1. Correct email and password returns `200` and a JWT token.
2. Wrong password returns an error.
3. Unknown email returns an error.
4. Account with non-`ACTIVE` status cannot login because the current code checks user status.

Example body:

```json
{
  "email": "owner@gmail.com",
  "password": "123456"
}
```

## C. Internal Account API

Main endpoint:

```http
POST {{baseUrl}}/api/admin/users
```

Header:

```text
Authorization: Bearer {{adminToken}}
```

Cases:

1. Admin creates `RACE_REFEREE` successfully.
2. Admin creates `ADMIN` successfully.
3. Duplicate email returns `400`.
4. Roles `HORSE_OWNER`, `JOCKEY`, `SPECTATOR` are rejected with `400`.
5. No token returns `401`.
6. `HORSE_OWNER` token returns `403`.

Example body:

```json
{
  "fullName": "Referee A",
  "email": "referee@gmail.com",
  "password": "123456",
  "role": "RACE_REFEREE"
}
```

## D. Users Management API

Endpoints:

```http
GET {{baseUrl}}/api/admin/users
GET {{baseUrl}}/api/admin/users/{{userId}}
PUT {{baseUrl}}/api/admin/users/{{userId}}
PATCH {{baseUrl}}/api/admin/users/{{userId}}/status
```

Cases:

1. Admin `GET /api/admin/users` succeeds.
2. Admin `GET /api/admin/users/{id}` succeeds.
3. Unknown id returns `404`.
4. Admin `PUT /api/admin/users/{id}` updates user successfully.
5. Admin `PATCH /api/admin/users/{id}/status` updates status successfully.
6. Normal user calling admin APIs returns `403`.
7. No token calling admin APIs returns `401`.

PUT body:

```json
{
  "fullName": "Nguyen Van A",
  "email": "user01@gmail.com",
  "role": "HORSE_OWNER",
  "status": "ACTIVE"
}
```

PATCH status body:

```json
{
  "status": "ACTIVE"
}
```

## E. Role Permission Matrix

| Actor | Request | Expected |
|---|---|---|
| ADMIN | `GET /api/admin/users` | `200` |
| HORSE_OWNER | `GET /api/admin/users` | `403` |
| SPECTATOR | `GET /api/admin/users` | `403` |
| No token | `GET /api/admin/users` | `401` |
| ADMIN | `POST /api/admin/users` | `200` |
| HORSE_OWNER | `POST /api/admin/users` | `403` |

## Suggested Test Order

1. Make sure the database has one active `ADMIN` account.
2. Login admin and save the returned token to `adminToken`.
3. Register a `HORSE_OWNER`, then login and save token to `ownerToken`.
4. Use `adminToken` to create internal users and save one returned `userId`.
5. Run list, detail, update, and status update requests.
6. Re-run admin requests with no token and with `ownerToken` to verify `401` and `403`.
