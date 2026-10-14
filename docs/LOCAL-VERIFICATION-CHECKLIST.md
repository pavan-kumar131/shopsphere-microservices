# Local verification checklist

Run from the project root:

```bash
cp .env.example .env
# Edit all placeholder values first.
docker compose config
docker compose up --build -d
docker compose ps
docker compose logs --tail=100 api-gateway auth-service product-service order-service payment-service notification-service
```

Then verify:

```bash
curl -fsS http://localhost:8080/actuator/health
curl -fsS http://localhost:8080/api/products
curl -fsS http://localhost:8080/actuator/gateway/routes
```

The exact product endpoint response depends on the current seed/catalog implementation. Test registration/login and checkout using a fresh local account; send `Authorization: Bearer <token>` and a unique `Idempotency-Key` to `POST /api/orders`. Payment sandbox API requires an order ID, amount, currency, and `Idempotency-Key`. Do not use real payment data.


## v4 security and stock-reservation checks

After `docker compose up --build -d` and registering a customer and seller, use these checks (replace tokens/IDs):

1. `GET /api/products` without a token returns the public catalogue.
2. `POST /api/products` without a token returns 401.
3. Seller A can create a product; seller B cannot update or delete seller A's product (403).
4. An admin can update/delete products across sellers.
5. A customer can create an order with an `Idempotency-Key`; repeating the same request/key returns the prior order.
6. Try two concurrent orders whose combined quantities exceed stock; the atomic stock decrement should allow only available stock to be reserved.
7. Declining the sandbox payment should attempt to release reserved stock. Verify the product stock afterwards. If any internal service call fails, inspect Order and Product logs; reconciliation is not implemented.
8. A customer cannot retrieve another customer's order or sandbox payment.
9. A tampered/expired JWT is rejected by the Gateway and each protected service.

These are manual test cases, not tests executed in the archive-authoring environment.
