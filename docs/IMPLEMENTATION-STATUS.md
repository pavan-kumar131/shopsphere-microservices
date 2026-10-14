# Implementation status — Google/IAM update

## Added in this update
- Google Identity Services sign-in on customer, seller, and admin login screens.
- Server-side Google ID-token signature, issuer, audience, expiry and email-verification checks.
- Separate IAM service, Flyway-managed schema, role/user persistence, admin allow-list, and internal OpenFeign calls from Auth Service.
- Role-safe first login: only customer accounts can be created by the public Google login flow; seller/admin roles are not self-assigned.
- Health checks for IAM, backend services, frontend services, Nginx and gateway.
- Auth unit tests and CI stages for Maven tests, frontend TypeScript/production builds, and Compose health/routing smoke checks.

## Still incomplete
- Payment is in-memory sandbox only; no real provider, signed webhook, refund, durable payment database or reconciliation.
- Notification is an in-memory acceptance API; no SMTP delivery, RabbitMQ, retries, DLQ or outbox.
- Stock reservation is implemented through Product Service, not a dedicated Inventory Service; compensation is best-effort.
- Cart is browser localStorage; no Cart Service.
- MinIO, RabbitMQ, Redis, Mailpit, Prometheus, Grafana and OpenTelemetry Collector are not included in Compose.
- User/Seller/Admin/Media/Shipping/Promotion/Review/Search/Wishlist/Return/Audit/Analytics are not all separate implemented business services.
- Full refresh-token lifecycle, complete checkout saga, complete AWS IaC and real deployment verification remain outstanding.

## Validation
See `VERIFICATION-REPORT.md`. Static validation passed. Docker/Maven were unavailable and frontend dependency installation timed out, so runtime, Java/JUnit and frontend build checks remain unverified in this environment.
