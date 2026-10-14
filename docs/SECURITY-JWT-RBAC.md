# JWT validation and backend RBAC — archive v4

## Implemented in source (runtime unverified)
- API Gateway validates signed, expiring JWTs on protected routes. Auth register/login and public product GET remain public.
- Product Service validates JWTs on non-public API routes. Sellers can create products; updates/deletes are allowed only for the owning seller or ADMIN. Product rows now include `seller_id`.
- Order Service validates JWT signature/expiry/subject/role. Checkout requires CUSTOMER; order reads are scoped to the authenticated customer except ADMIN.
- Product stock is reserved through internal Feign endpoints using `INTERNAL_SERVICE_TOKEN`. The database update is conditional on sufficient stock. The internal endpoints are not routed through the public Gateway.
- Payment sandbox records carry a customer ID; lookup is owner-scoped unless ADMIN. Sandbox confirmation is ADMIN-only.
- Notification endpoints are ADMIN-only until a trusted event consumer/internal service identity is implemented.
- Public registration only allows CUSTOMER or SELLER; ADMIN is seeded from environment configuration.

## Secrets
Generate independent local values:

```bash
openssl rand -base64 48
openssl rand -hex 32
```

Use the first for `JWT_SECRET` (must decode to at least 32 bytes) and the second as a long `INTERNAL_SERVICE_TOKEN`. Put them in `.env`, never commit `.env`, and rotate any secret previously exposed in terminal/chat logs. Use Secrets Manager and ECS task roles in production; do not embed AWS keys.

## Limitations
- No refresh-token rotation/revocation, password reset/email verification, login throttling, shared reusable security library, issuer/audience validation, or comprehensive automated security tests.
- Seller ownership is checked for product update/delete. Seller onboarding/approval and seller order ownership are not implemented.
- Payment remains an in-memory sandbox and is not authoritatively linked to Order Service. Do not use for real transactions.
- Inventory compensation is best-effort; no durable outbox/reconciliation. Do not treat the distributed checkout as production-grade.
- JWT checks in the Gateway are defense-in-depth. Services must enforce their own authorization because internal service ports may be reachable within the network.

## Suggested manual checks
- `GET /api/products` without token should return the public catalogue.
- `POST /api/products` without token should return 401.
- Seller A can create a product; Seller B cannot update/delete it (403).
- ADMIN can update/delete products across sellers.
- Customer A cannot read Customer B's order or sandbox payment.
- Tampered and expired tokens should be rejected.
- Concurrent checkouts must not reserve more stock than exists.

These runtime checks have not been executed in the archive-authoring environment.
