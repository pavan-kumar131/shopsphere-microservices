# Order workflow implementation and test guide

## What this slice implements

- Authenticated `POST /api/orders` for CUSTOMER tokens.
- Server-side product lookups and price calculation; client-submitted prices are never trusted.
- Required `Idempotency-Key` header, unique per customer, to prevent accidental duplicate order records on retry.
- Persistent order records and customer order history in PostgreSQL.
- Customer ownership checks when reading an order.
- Explicit sandbox-only payment state transition and admin-only sandbox shipping transition.
- Cart page calls the order API; it no longer clears the cart while claiming a fake order was placed.

## API examples

Create order:

```bash
curl -X POST http://localhost:8080/api/orders \
  -H 'Authorization: Bearer <CUSTOMER_JWT>' \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: checkout-unique-123' \
  -d '{"items":[{"productId":1,"quantity":2}]}'
```

List current customer's orders:

```bash
curl http://localhost:8080/api/orders -H 'Authorization: Bearer <CUSTOMER_JWT>'
```

Simulate a successful payment for testing only:

```bash
curl -X POST 'http://localhost:8080/api/orders/1/payment-sandbox?outcome=success' \
  -H 'Authorization: Bearer <CUSTOMER_JWT>'
```

Simulate shipping status (ADMIN token only):

```bash
curl -X POST http://localhost:8080/api/orders/1/shipping-sandbox \
  -H 'Authorization: Bearer <ADMIN_JWT>'
```

## Important limitations

This is a runnable learning workflow slice, not a production commerce implementation. Payment is simulated and no card or money is processed. Shipping is simulated and no carrier is called. The order service checks available product stock but does not yet reserve/decrement stock atomically across the product and order services; concurrent checkout can oversell. Failed payment does not yet release inventory because there is no reservation. There is no outbox/SQS event-driven saga yet, and notification emails are not sent. Do not take real orders or deploy as a production shop until these are implemented and integration-tested.

## Manual acceptance test

1. `cp .env.example .env`, set strong random `POSTGRES_PASSWORD`, `ADMIN_PASSWORD`, and base64 `JWT_SECRET` (32+ random bytes), then run `docker compose up --build`.
2. Register a CUSTOMER, log in, browse a seeded product and add it to the cart.
3. Place an order from `/cart`; confirm response shows an order ID and `PENDING_PAYMENT`.
4. Refresh and check `GET /api/orders`; order must persist.
5. Use sandbox payment; verify order becomes `PAID` and shipping becomes `PROCESSING`.
6. Try reading another customer's order; expect 403. Try shipping with CUSTOMER token; expect 403.
7. Repeat checkout with the same idempotency key using curl; expect the same order, not a duplicate.
8. Before any real use, implement stock reservation, payment-provider webhooks, transactional outbox/SQS, shipping integration, and run concurrency/failure tests.
