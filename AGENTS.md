# AGENTS.md

## Learned User Preferences

- Respond in Chinese (中文) by default.
- This machine uses Podman / `podman-compose`, not Docker — use Podman commands for container builds and runs.
- When asked to test a module or UI, prefer comprehensive coverage of both success paths and error/edge branches.

## Learned Workspace Facts

- Multi-module Maven backend (`lims-common`, `lims-model`, `lims-dao`, `lims-service`, `lims-web`, `lims-workflow`) plus a UmiJS / Ant Design Pro frontend in `lims-web-ui`.
- Spring Boot 3.2 backend using MyBatis-Plus, Flowable workflow, and PostgreSQL / Redis / MinIO.
- Build and test the backend with the Maven wrapper `./mvnw`; run targeted tests via `./mvnw -pl <modules> -am test -Dtest='...' -Dsurefire.failIfNoSpecifiedTests=false`.
- Frontend tests use Jest + ts-jest + Testing Library; run with `cd lims-web-ui && npm test` (config in `jest.config.ts`, global mocks in `src/tests/setup.ts`).
- Local infrastructure runs via `podman-compose` (postgres, redis, minio, lims-backend); containers are named `material-lims_<service>_1`.
- Database schema and seed data live in `lims-web/src/main/resources/db/init.sql`.
- The `dev` profile auto-injects virtual user `dev-user-0001` (all roles) through `DevAuthFilter` and permits all endpoints.
- The `Dockerfile` base image must be `eclipse-temurin:17-jdk` / `17-jre` (non-alpine) for ARM64 / macOS Podman builds.
- Backend serves on port 8080; the frontend dev server runs on 8000 and proxies `/api` to 8080.
- API responses are wrapped by the `R` result class (`{code, data, ...}`).
