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
