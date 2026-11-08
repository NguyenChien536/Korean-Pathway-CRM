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

Once a database has run `R__seed_demo_data.sql`, keep the demo location visible to Flyway when using that database. Starting without the `dev` profile hides `db/demo` and can fail validation with `Detected applied migration not resolved locally: seed demo data`. In IntelliJ, activate `dev` for the local demo database (Active profiles, or program argument `--spring.profiles.active=dev`). To run without demo migrations, use a separate database initialized from the normal migration location. An existing history entry is not removed by switching profiles; `repair` is not the appropriate first action when the file still exists and the problem is its location.

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

Role assignment is restricted to `SYSTEM_ADMIN`, including the initial role of a new user. A delegated user manager may edit ordinary users, but cannot reset/deactivate system administrators or grant roles. Self-deactivation is forbidden. Removing the last active system administrator returns `409` with code `1014`.

User-management mutations acquire a write lock on the `SYSTEM_ADMIN` role row before reading account state. Every future flow that changes administrator membership must follow the same lock protocol. This intentionally serializes those mutations for this single-organization CRM. `User` uses dynamic updates so a concurrent profile-only write does not restore a stale role or active flag.

## Verify security policies

Run the suite against the default in-memory test database:

```powershell
.\mvnw.cmd test
```

`SecurityPolicyIntegrationTest` covers delegated-manager restrictions, administrator membership, concurrent self-demotion, and introspection after token invalidation. Its database is isolated from the other test classes.

To exercise the same cases with Flyway and PostgreSQL, use a dedicated disposable database. For example, start a container with a test-only password and inspect its random local port:

```powershell
docker run --rm --detach --name crm-policy-check -e POSTGRES_PASSWORD=policy_test_only -e POSTGRES_DB=crm_policy_test -p 127.0.0.1::5432 postgres:16-alpine
docker port crm-policy-check 5432
```

Wait until `docker exec crm-policy-check pg_isready -U postgres -d crm_policy_test` reports readiness, then replace `<port>` with the reported port:

```powershell
$env:POLICY_TEST_DB_URL = "jdbc:postgresql://127.0.0.1:<port>/crm_policy_test"
.\mvnw.cmd "-Dtest=SecurityPolicyIntegrationTest" "-Dspring.datasource.driver-class-name=org.postgresql.Driver" "-Dspring.datasource.username=postgres" "-Dspring.datasource.password=policy_test_only" "-Dspring.jpa.hibernate.ddl-auto=validate" "-Dspring.flyway.enabled=true" test
Remove-Item Env:POLICY_TEST_DB_URL
docker stop crm-policy-check
```

The policy test clears users and tokens in its test database between cases. Use only a disposable test database for this command.

## Migration convention

- `V<n>__...sql` is a versioned migration for schema or reference-data changes. Never edit a versioned migration after it has been applied to a shared database.
- `R__...sql` is a repeatable migration. Keep its writes idempotent because Flyway can rerun it when its contents change.
- Optional local demo fixtures live under `db/demo` and are loaded only by `application-dev.yml`.
