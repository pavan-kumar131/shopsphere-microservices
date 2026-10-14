# ShopSphere requirements cross-check (archive v4)

This document compares the master prompt against the code shipped in this archive. It intentionally distinguishes source presence from runtime verification.

Legend: **Implemented (source)** = code/config exists; **Partial** = some source exists but the requested workflow is incomplete; **Missing** = not implemented; **Unverified** = could not execute here.

| Requirement from master prompt | Status | Evidence / limitation |
|---|---|---|
| Three independently buildable Next.js apps | Partial | Separate `frontend-customer`, `frontend-seller`, `frontend-admin` contexts now exist; seller/admin still reuse much of the customer source and are not feature-complete separate products. |
| Nginx routes UI and `/api/*` | Implemented (source), runtime unverified | `nginx/nginx.conf`; Compose context paths checked by `scripts/verify-static.py`. |
| API Gateway routes core APIs and validates JWT | Partial | Gateway routes auth, products, orders, payments, notifications and validates bearer JWTs; no rate limiter, issuer/audience validation, refresh/revocation, or tracing. |
| Auth registration/login and BCrypt | Implemented (source), runtime unverified | Auth service. Public registration only supports CUSTOMER/SELLER; no seller approval, email verification, password reset, refresh rotation/revocation, or login throttling. |
| Server-side JWT and role checks | Partial | Filters/controllers in core services. No uniform shared security library or comprehensive automated negative tests. |
| Seller product ownership | Implemented (source), migration/runtime unverified | `Product.sellerId`, owner checks in `ProductController`; admin can manage all products, sellers only their own. Hibernate `ddl-auto:update` is still used, not Flyway. |
| Customer order ownership | Partial | Order read checks customer ID; sandbox payment/order flow is not connected to Payment Service. |
| Payment provider, signed webhooks, idempotency, refunds, reconciliation | Missing/Partial | Sandbox payment records in memory with owner checks; no real provider, signed webhook, durable store, refund or reconciliation. Order Service currently has its own sandbox payment shortcut. |
| Inventory Service with atomic reservations | Missing | Product stock is checked by Order Service but not atomically reserved/decremented; oversell is possible under concurrent checkout. |
| Cart Service and persisted carts | Missing | Cart currently lives in browser localStorage. |
| User/Profile, Seller, Admin services | Missing as independent backend services | Role-related code exists in Auth/Product, but dedicated services and their APIs are not implemented. |
| Media Service and private S3/MinIO upload/retrieval | Partial | Presigned S3 URL source exists in Product Service, but no MinIO local integration, image metadata persistence, robust signature/dimension validation, or end-to-end test. |
| PostgreSQL persistence and isolated service schemas | Partial | Auth, Product, Order use the same PostgreSQL database; no per-service database/schema ownership strategy or Flyway migrations. Payment and notifications are in-memory. |
| Feign Client | Implemented (source), runtime unverified | Order → Product `ProductClient` retrieves products and calls authenticated atomic reserve/release endpoints; no other required Feign calls or tests. |
| RabbitMQ durable events/outbox/DLQ | Missing | Not configured or connected. |
| Notification delivery via Mailpit/SMTP | Missing/Partial | Notification API accepts in-memory demo records only; no delivery. |
| Promotion, Review, Search, Wishlist, Return, Audit, Analytics, Shipping services | Missing | Not present as independently implemented services. A sandbox shipping status is part of Order Service only. |
| Docker Compose full architecture | Partial | Core UI, gateway, auth, product, order, payment, notification and PostgreSQL defined. RabbitMQ, Redis, MinIO, Mailpit, Prometheus, Grafana and OTel collector are absent. Docker execution unavailable here. |
| Health checks and env placeholders | Partial | Compose has checks for core services; `docker compose config` could not be run because Docker is unavailable. |
| Jenkins CI/CD | Partial | `Jenkinsfile` exists, but its cloud validation/deploy stages do not cover all services, and cloud operations were not run. |
| AWS IAM, ECR, ECS, ALB, RDS, S3/CloudFront, Secrets Manager | Partial | CloudFormation templates contain a core deployment. They do not deploy all local services; no successful AWS validation/deployment. Prior account SCP restrictions block some CloudFormation/ACM actions. |
| HTTPS/ACM, private service networking, production autoscaling | Partial/Missing | Existing template is HTTP-oriented and not complete for the full service list. Do not treat as production ready. |
| Prometheus/Grafana/OTel/CloudWatch dashboards and alerts | Missing/Partial | Actuator health endpoints exist; dashboards, collector and meaningful alerts are not included. |
| Unit, integration, browser/E2E, security tests | Partial | Some test dependency declarations exist; full suite and browser tests have not been run. |
| Makefile / offline static validation | Implemented | `Makefile`, `scripts/verify-static.py`. Static checks are not build/runtime tests. |

## Verification performed in this environment

- Maven POM XML parsing: see the output of `make validate` / `scripts/verify-static.py`.
- Frontend package JSON parsing and required scripts.
- Compose YAML parsing and build-context existence.
- Static checks for seller ownership, payment owner checks, JWT expiry checks, and Feign client presence.

## Verification not possible here

Docker and Maven executables are unavailable in this environment and external package downloads are blocked. Therefore no Java compilation, Maven tests, Docker image builds, Compose runtime, frontend production build, browser E2E tests, AWS CloudFormation validation, S3 upload, or deployment is claimed as passed.

## Security caveats

Use a unique strong `JWT_SECRET` in `.env`, never commit `.env`, and rotate any secret previously exposed in terminal/chat logs. Sandbox payments are not real payments. Do not expose service ports or use this project for real customer data until the missing requirements are implemented and runtime security tests pass.
