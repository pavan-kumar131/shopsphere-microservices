# Feature scope and acceptance checks

## Implemented in the supplied application source

- Product catalogue and product detail reads backed by PostgreSQL.
- Customer registration with BCrypt password hashing and login with a short-lived signed JWT.
- Admin account seeding on first startup (generated password is supplied through Secrets Manager on AWS).
- Admin-only product create/update/delete enforced by JWT verification in Product Service.
- Browser-local cart and a demo-only checkout confirmation.
- API Gateway routes `/api/auth/**` and `/api/products/**` to internal services.
- Container health checks, CloudWatch logs, ECS deployment circuit breakers, ECR image scanning-on-push, and a private RDS database.

## Not implemented; do not assume it exists

- Persisted order lifecycle, stock reservation/rollback, shipping/notification workflows, seller marketplace, customer profile service, or reviews/promotions microservices.
- Real payment-provider checkout/webhooks or PCI DSS controls.
- Automated email verification, password reset, MFA, refresh-token rotation, login throttling, or secure HttpOnly cookie/BFF sessions.
- HTTPS/custom domain/WAF in the included template.
- Production database migrations, load testing, dependency vulnerability gate, or full end-to-end browser tests.

## Smoke acceptance checks after deployment

1. `GET /api/products` returns a JSON array and seeded products.
2. Register a fresh customer, then log in; the login response includes a bearer token and `CUSTOMER` role.
3. A customer token cannot create/update/delete products (expect 403).
4. The seeded admin can create/update/delete a test product; an invalid/expired token is rejected.
5. Frontend can browse products and update the browser-local cart.
6. ECS services reach `runningCount == desiredCount`; logs show no datasource, JWT decoding, or service discovery errors.

This package is a deployable learning environment for the above feature slice, not a complete production e-commerce platform. The build/deploy environment used to prepare the archive did not have Docker, Maven, or AWS credentials, so a real image build, AWS CloudFormation deployment, and live smoke test could not be executed here. Run the acceptance checks in your account before relying on it.
