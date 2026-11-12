# Korean Pathway CRM

Korean Pathway CRM is a single-organization CRM backend for a Korean study-abroad consulting center. The project is being developed as a learning and portfolio project, one business module at a time.

## Current implementation

The current backend includes:

- Username/password authentication with BCrypt and JWT access tokens.
- Refresh-token rotation and logout/revocation.
- User administration, personal profile updates, password changes, filtering and pagination.
- Role-based permissions. Each user has one role; a user's permissions come from that role. Permission codes are application-managed reference data.
- PostgreSQL schema migrations managed by Flyway, including reference data and optional local demo fixtures.
- Shared API response and exception handling.

The CRM domain schema includes people, leads, customers, student profiles, school applications, documents, activities, tasks and financial records. Their tables are not evidence that the corresponding business APIs are implemented. Lead capture integrations, omnichannel inbox, Kanban UI, document storage, financial workflows, reports and a frontend remain planned work.

## Technology

- Java 17 and Spring Boot 4.0.6
- Spring MVC, Spring Security, JWT and Bean Validation
- Spring Data JPA, PostgreSQL and Flyway
- MapStruct
- Maven Wrapper

This is currently a backend-only repository. There is no `frontend` directory yet. Docker-based deployment and a public demo are also future work.

## Requirements

- JDK 17 or later
- PostgreSQL 14 or later
- Git

## Run locally on Windows

1. Clone the repository and enter its root directory:

   ```powershell
   git clone https://github.com/NguyenChien536/Korean-Pathway-CRM.git
   Set-Location Korean-Pathway-CRM
   ```

2. Create a local PostgreSQL database. The default local connection uses database `crm_korean`, username `postgres` and port `5432`. Override these with `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME` and `DB_PASSWORD` if your setup differs.

3. Initialize a persistent local JWT signing key once:

   ```powershell
   .\backend\scripts\initialize-local-jwt.ps1
   ```

   This creates `backend/.local/jwt.properties`, which is ignored by Git. Keep it between restarts. The key must contain at least 64 UTF-8 bytes for HS512. Do not commit this file or use a development key in a shared environment.

4. Start the backend from the repository root:

   ```powershell
   .\backend\mvnw.cmd -f backend\pom.xml spring-boot:run
   ```

   The API base URL is `http://localhost:8081/crm-korean`. Flyway applies the regular migrations on startup. Hibernate validates the resulting schema.

## Optional local demo profile

To create the disposable local demo administrator and load fictional person/lead records, start with the `dev` profile. Follow [backend/DEVELOPMENT.md](backend/DEVELOPMENT.md) for the exact setup and API examples.

The demo administrator is intended only for a disposable local database. Do not use the `dev` profile or its public sample credentials with shared or production data.

## API examples

The current API includes:

- `POST /crm-korean/api/v1/auth/token` — log in and receive access/refresh tokens.
- `POST /crm-korean/api/v1/auth/refresh` — rotate a refresh token.
- `POST /crm-korean/api/v1/auth/logout` — revoke the session and invalidate the access token.
- `/crm-korean/api/v1/users` — user administration and the signed-in user's `/me` endpoints.
- `/crm-korean/api/v1/roles` — role management and role-permission assignment.
- `/crm-korean/api/v1/permissions` — read the application-managed permission catalog.

Use the OpenAPI UI at `http://localhost:8081/crm-korean/swagger-ui/index.html` while the backend is running.

## Tests and database migrations

Run the regular automated suite from the repository root:

```powershell
.\backend\mvnw.cmd -f backend\pom.xml test
```

Most tests use an in-memory H2 database for speed. `FlywayMigrationSmokeTest` runs against PostgreSQL when `MIGRATION_TEST_DB_URL`, `MIGRATION_TEST_DB_USERNAME` and `MIGRATION_TEST_DB_PASSWORD` are provided; the GitHub Actions workflow supplies them automatically. A separate PostgreSQL security-policy run and local database instructions are documented in [backend/DEVELOPMENT.md](backend/DEVELOPMENT.md).

Versioned migration files are in `backend/src/main/resources/db/migration`:

- `V<n>__description.sql` runs once in version order. Do not edit an already-applied migration; create a new version for a schema change.
- `R__description.sql` is repeatable and runs again when its contents change. Its changes must be safe to repeat.
- Local-only demo fixtures are in `backend/src/main/resources/db/demo` and are loaded by the `dev` profile.

## Project documents

- [Local development and API examples](backend/DEVELOPMENT.md)
- [Requirements and use cases](docs/SRSReport.md)
- [Technical implementation roadmap](docs/ROADMAP_TRIEN_KHAI.md)
