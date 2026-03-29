# Repository Guidelines

## Project Structure & Module Organization
- Backend lives under `services/` with per-service Spring Boot modules (`api-gateway`, `route-service`, `delivery-service`, `location-service`, `notification-service`); shared contracts are in `shared/domain-model` (Kafka events) and `shared/common-api` (HTTP DTOs/filters).
- Mobile Ionic/Angular app is in `mobile-app/`; assets and Capacitor configs stay co-located with feature folders.
- Infrastructure and deployment assets sit in `infra/` (Helm charts, Docker init SQL) and `docker-compose.yml`; utility scripts are in `scripts/`; additional docs in `docs/`.

## Build, Test, and Development Commands
- Start local infra: `docker compose up -d`.
- Backend build + unit tests: `./gradlew build` (runs ktlint and tests across services).
- Service dev loop: `./gradlew :services:api-gateway:bootRun` (swap module name/port per service).
- Lint-only: `./gradlew ktlintCheck`; format: `./gradlew ktlintFormat`.
- Mobile app: `cd mobile-app && npm install --legacy-peer-deps && npm start` for live reload; `npm test -- --watch=false --browsers=ChromeHeadless` for unit tests; `npm run lint` for Angular lint; production build via `npm run build:prod`.
- E2E curl flow lives in `docs/e2e-test.sh`; use it after backend + infra are up.

## Coding Style & Naming Conventions
- Kotlin: 4-space indent, idiomatic ktlint defaults; package names lowercase dot-separated; classes/DTOs in `shared/` should remain immutable data classes where possible.
- Spring configs use Kotlin DSL Gradle; keep `application-*.yml` per service/environment under that service’s `src/main/resources`.
- Angular: feature folders and components in kebab-case (e.g., `tracking-map/route-card.component.ts`); prefer standalone components and typed RxJS streams; keep SCSS colocated.

## Testing Guidelines
- Backend unit/integration tests live beside code in each service under `src/test/kotlin`; use descriptive test names (`should...`) and include Kafka contract expectations when emitting events.
- Run `./gradlew test` before PRs; add focused integration tests when touching persistence or messaging boundaries.
- Mobile: keep spec files next to components; prefer harnessed DOM queries over brittle CSS selectors.
- Use `docs/e2e-test.sh` as a smoke test after changes that cross services or affect auth/routing.

## Commit & Pull Request Guidelines
- Commits: short, imperative subjects; include scope when helpful (`route: handle empty shift`). Squash noisy WIP commits before opening a PR.
- PRs must follow `.github/pull_request_template.md`: summary + key changes, testing checklist (`./gradlew test`, `./gradlew ktlintCheck`, curl E2E, `npm run build:prod`), and safety checks (no secrets, migrations backward compatible, Kafka topics and API shapes unchanged unless versioned).
- Link relevant issues and add screenshots for UI-facing changes (mobile).
