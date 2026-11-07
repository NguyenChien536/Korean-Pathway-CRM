# Local development

## Enable demo data

The normal Flyway migrations create the schema and reference data, but intentionally do not create an account with a known password. To bootstrap a local account and load demo data, activate the `dev` Spring profile. `ApplicationInitConfig` creates the local admin after Flyway has run; this profile also adds the repeatable migration in `src/main/resources/db/demo` to Flyway's locations.

From the `backend` directory in PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
.\mvnw.cmd spring-boot:run
```

On startup with the `dev` profile, the application creates:

- `admin` / `Admin123!` with the `SYSTEM_ADMIN` role.

Flyway then applies the repeatable demo migration to create three fictional people and leads. The demo leads are initially unassigned.

These credentials are public and intended only for a disposable local development database. Do not activate the `dev` profile against a shared, staging, or production database. Do not reuse this password for a real account.

## Create a user through the API

First log in as the local demo administrator:

```http
POST http://localhost:8081/crm-korean/api/v1/auth/token
Content-Type: application/json
```

```json
{
  "username": "admin",
  "password": "Admin123!"
}
```

Use the returned access token to read the available roles and choose a role ID:

```http
GET http://localhost:8081/crm-korean/api/v1/roles
Authorization: Bearer <accessToken>
```

Then create the account. `roleId` must be the UUID of the selected role. The password must be 8–72 characters and include uppercase, lowercase, a digit, and a special character.

```http
POST http://localhost:8081/crm-korean/api/v1/users
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "username": "counselor01",
  "email": "counselor01@example.test",
  "phone": "+821012345678",
  "fullName": "Demo Counselor",
  "password": "Counselor123!",
  "confirmPassword": "Counselor123!",
  "roleId": "<UUID from GET /api/v1/roles>"
}
```

The service checks `USERS.MANAGE`, validates unique username/email/phone and password confirmation, resolves the role, hashes the password with BCrypt, and records the current actor in the audit fields. There is no public self-signup endpoint.

## Migration convention

- `V<n>__...sql` is a versioned migration for schema or reference-data changes. Never edit a versioned migration after it has been applied to a shared database.
- `R__...sql` is a repeatable migration. Keep its writes idempotent because Flyway can rerun it when its contents change.
- Optional local demo fixtures live under `db/demo` and are loaded only by `application-dev.yml`.
