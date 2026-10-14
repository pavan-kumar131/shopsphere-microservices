<<<<<<< HEAD
# Verification report — ShopSphere Google/IAM update

## Implemented in this update

- Google Identity Services button wired into customer, seller, and admin login panels.
- `POST /api/auth/google` validates Google ID tokens with Google's published JWKS, issuer, audience, expiration, and verified-email claim; it exchanges a verified identity for a ShopSphere JWT.
- Separate `iam-service` with its own `iam` PostgreSQL schema, Flyway migration, user/role persistence, admin email allow-list, and internal user lookup/create APIs.
- OpenFeign integration from Auth Service to IAM. New customer identities receive only `CUSTOMER`; seller/admin privileges cannot be obtained from public signup. Admins must be pre-allow-listed.
- Numeric IAM IDs align with current Product/Order/Payment token-subject expectations.
- Gateway allows the Google auth exchange endpoint without requiring an existing ShopSphere bearer token.
- Compose health checks cover PostgreSQL, all backend services, all three frontends, gateway and Nginx; IAM has a health-gated dependency before Auth starts.
- Unit tests for Google-auth controller behavior (new customer creation, no admin self-registration, wrong-site role rejection, invalid site), existing IAM tests, and Jenkins Maven + frontend build/typecheck + Compose health smoke-test stages.
- Seller and admin Next.js base paths are configured to match Nginx routes.

## Checks actually run in this authoring environment

- `python3 scripts/verify-static.py`: static checks passed; the latest run count is recorded by its console output.
- Python YAML parser successfully parsed `docker-compose.yml`; all Compose build contexts exist.
- XML parser successfully parsed all service Maven POM files.
- JSON parser successfully parsed all frontend package manifests.
- `bash -n scripts/smoke-test.sh` passed.

## Not run / unavailable

- Docker Engine / Docker Compose are not installed in this environment, so Docker builds, `docker compose config`, service startup, container health checks, and end-to-end smoke tests were not run here.
- Maven is not installed and package downloads timed out, so Java compilation and JUnit execution were not run here.
- Frontend dependency installation timed out, so TypeScript checks and Next.js production builds were not run here.
- Google login was not tested against a real Google OAuth client. You must configure `GOOGLE_CLIENT_ID` and the allowed JavaScript origins in Google Cloud Console.
- AWS/CloudFormation validation and deployment were not run. Existing organization SCP restrictions may deny validation/certificate actions.

## Known boundaries

This update implements the requested Google-auth/IAM/health-check/automated-test layer, but it does not close every gap in the original ShopSphere master requirements. Payment remains sandbox/in-memory; notification delivery is not durable; there is no real payment provider/webhook/refund/reconciliation, full refresh-token lifecycle, dedicated Cart/Inventory/Media/Shipping/Review/Search/Return/etc. services, complete MinIO/S3 upload flow, or verified AWS deployment. See `REQUIREMENTS-CROSSCHECK.md` for the broader status.
=======
# Verification report — ShopSphere archive v4

## Changes in this archive

- Separate Next.js build contexts: `frontend-customer`, `frontend-seller`, and `frontend-admin`.
- Nginx and Compose updated to use the independent frontend contexts.
- JWT filters in Gateway/Product/Payment/Notification and JWT claim checks in Order Service.
- Product seller ownership persisted in `seller_id`; seller update/delete is restricted to the owning seller or an admin.
- Sandbox payment records are associated with the authenticated user and lookup is owner-scoped unless ADMIN.
- Order → Product Feign client now has authenticated internal stock reservation/release calls. Product Service uses a conditional database update (`stock >= quantity`) to atomically reserve stock; Order Service attempts compensation on failed checkout and declined sandbox payment.
- Added `Makefile`, `scripts/verify-static.py`, `docs/IMPLEMENTATION-STATUS.md`, and `docs/REQUIREMENTS-CROSSCHECK.md`.

## Checks actually run in this environment

- `python3 scripts/verify-static.py`: **42 structural checks passed, 0 failed**. This parses Maven XML and frontend package JSON, parses Compose YAML, checks Compose build contexts and expected services, and checks source invariants/brace balance.
- `bash -n scripts/deploy.sh scripts/bootstrap-ubuntu.sh scripts/destroy-demo.sh`: passed.
- `python3 -m py_compile scripts/verify-static.py`: passed.

These are static checks only. They do not establish that Java or TypeScript compiles or that the services work together.

## Not run / unavailable

Docker and Maven are not installed in this authoring environment, and outbound package downloads are blocked. Therefore Java compilation, Maven/JUnit tests, Docker builds, `docker compose config`, Compose startup, Next.js production builds, browser/E2E tests, real MinIO/S3 upload tests, CloudFormation validation, and AWS deployment were **not run**. Do not infer those tests passed.

## Known gaps

- This archive is not the complete master-prompt application. Consult `docs/REQUIREMENTS-CROSSCHECK.md` for each requirement and status.
- Payment is in-memory sandbox only; no real provider, signed webhook, refund, durable payment database or reconciliation.
- Notification is an in-memory acceptance API; no SMTP delivery, RabbitMQ, retries, DLQ or outbox.
- Stock reservation is implemented through Product Service, not a dedicated Inventory Service; distributed reservation compensation is best-effort and no durable reconciliation/outbox exists.
- Cart is browser localStorage; no Cart Service.
- MinIO, RabbitMQ, Redis, Mailpit, Prometheus, Grafana and OpenTelemetry Collector are not included in Compose.
- User, Seller, Admin, Media, Shipping, Promotion, Review, Search, Wishlist, Return, Audit and Analytics are not separate implemented backend services.
- Flyway migrations and service-owned database schemas are missing; core services share a PostgreSQL database with Hibernate schema updates.
- AWS template/deployment covers only a subset of the services and does not represent a validated production deployment. Previous AWS Organizations SCP restrictions may block CloudFormation validation and ACM actions.
>>>>>>> origin/main
