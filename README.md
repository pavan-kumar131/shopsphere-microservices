# ShopSphere — e-commerce microservices project

A Docker Compose development project based on the existing ShopSphere repository. It includes three separately running Next.js frontends (customer, seller, admin), Nginx reverse proxy, Spring Cloud Gateway, Auth/Product/Order services, a typed Spring Cloud OpenFeign client from Order Service to Product Service, sandbox Payment Service, Notification API, and PostgreSQL persistence for the existing account/catalog/order paths.

## Start locally

Requirements: Docker Engine and the Docker Compose plugin.

```bash
cp .env.example .env
# Edit .env and replace every REPLACE_* value with strong local values.
# JWT_SECRET should be base64 for at least 32 random bytes, e.g. openssl rand -base64 48
docker compose config
docker compose up --build -d
docker compose ps
```

Open `http://localhost:3001` for the customer storefront. Nginx routes `/seller/` to the seller UI and `/admin/` to the admin UI. The API gateway is exposed at `http://localhost:8080`; PostgreSQL is exposed at `localhost:5432` for local development. Follow startup with `docker compose logs -f` if any service is unhealthy.

## Main API routes

- `POST /api/auth/register`, `POST /api/auth/login` — authentication.
- `/api/products/**` — product catalog.
- `POST /api/orders` and `/api/orders/**` — customer-owned order workflow. Checkout requires a bearer token and `Idempotency-Key`.
- `POST /api/payments` — creates a **sandbox-only** payment record; requires `Idempotency-Key`.
- `POST /api/payments/{id}/confirm-sandbox?outcome=success|failure` — development simulation only.
- `POST /api/notifications` — accepts a notification request for local demo/testing.
- `GET /actuator/health` on each service container — health check.

## Feign Client

`order-service` uses Spring Cloud OpenFeign (`ProductClient`) for typed internal calls to `product-service`; it does not send this internal call through the public API gateway. Configure `PRODUCT_SERVICE_URL` for the Compose service URL when running locally.

## Important functional and production boundaries

This is an **incomplete development baseline, not a production-ready commerce platform**. Payment is simulated in memory and is not connected to a payment provider, signed webhook, refunds, or reconciliation. The Notification API stores accepted demo messages in memory; SMTP/SMS and RabbitMQ event delivery are not wired. Order checkout now uses an atomic conditional stock decrement in Product Service through an authenticated internal Feign call and compensates reservations when checkout fails or sandbox payment is declined. This is still not a dedicated Inventory Service or a durable distributed saga; failed compensation requires reconciliation. Product image presigning source exists, but local MinIO, image metadata persistence, upload validation and full upload/retrieval are not wired end to end. Several required business services and observability components are missing. The AWS template/deploy script cover only a subset of the local project. Do not use this package for real payments or real customer traffic. See `docs/REQUIREMENTS-CROSSCHECK.md` for the full status matrix.

## AWS deployment

Review `docs/DEPLOYMENT.md`, `docs/IAM-POLICY.md`, and `infrastructure/` before attempting deployment. Your AWS organization previously denied CloudFormation validation and ACM certificate requests through an SCP, so AWS deployment/certificate provisioning must not be considered successful unless an organization administrator resolves those policy restrictions and live smoke tests pass. No AWS resources are created by these local instructions.

## Verification

See `docs/VERIFICATION-REPORT.md` and `docs/REQUIREMENTS-CROSSCHECK.md` for checks and unverified areas. Run `make validate` for offline structural checks. This environment has no Docker or Maven executable, so container builds and Java compilation/tests could not be run here. Static project/configuration checks are not a substitute for running the full stack on your machine.


## JWT and backend authorization update

See `docs/SECURITY-JWT-RBAC.md` for the implemented JWT gateway/service filters, role rules, configuration and remaining limitations. Set one shared base64 `JWT_SECRET` for all services. Seller-specific product ownership is represented on products and checked by Product Service. Payment records are associated with the authenticated user in the sandbox, but payment-to-order ownership is not verified against Order Service and payment state is in-memory. This is not a complete payment integration.
