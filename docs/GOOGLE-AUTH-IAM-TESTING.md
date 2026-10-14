# Google authentication, separate IAM, and verification

## Architecture

- `auth-service` exposes `POST /api/auth/google` and verifies the Google ID token signature against Google's published JWKS, issuer, audience (`GOOGLE_CLIENT_ID`), expiry, and `email_verified` claim.
- The browser sends the ID token only. The server does not trust client-provided email or role values.
- `auth-service` calls `iam-service` through OpenFeign on the private Compose network. IAM owns user IDs, active state, role assignments, the admin allow-list, and its own Flyway-managed `iam` schema.
- A first-time customer Google login creates a `CUSTOMER` IAM account. Seller/admin roles cannot be self-assigned; seller accounts must already have `SELLER`, and admins are seeded from `ADMIN_EMAILS` / `ADMIN_EMAIL`.
- Auth issues the ShopSphere HS256 access token accepted by the current gateway. Keep `JWT_SECRET` identical for auth and gateway and at least 32 decoded bytes.
- IAM's `/internal/**` endpoints are intentionally not routed by the API gateway and IAM does not publish a host port. Do not expose IAM directly to the internet.

## Google setup

1. In Google Cloud Console, configure an OAuth 2.0 Client ID of type **Web application**.
2. Add the local origin `http://localhost:3001` and the real deployment origin to **Authorized JavaScript origins**. This frontend uses Google Identity Services' JavaScript button; it does not use a client secret in the browser.
3. Copy the Web client ID to `.env` as `GOOGLE_CLIENT_ID=...apps.googleusercontent.com`. The same value is passed to the backend verifier and baked into the three Next.js builds as `NEXT_PUBLIC_GOOGLE_CLIENT_ID`.
4. Configure `ADMIN_EMAIL` / `ADMIN_EMAILS` with the exact Google account email intended to administer the app. Never make public signup grant `ADMIN`.
5. Rebuild after changing the public client ID: `docker compose up --build -d`.

Google sign-in is intentionally unavailable when no client ID is configured; the backend returns HTTP 503 rather than accepting unverified tokens. Google ID tokens are short-lived identity assertions, not ShopSphere API access tokens; the endpoint exchanges them for the app's own JWT.

## Automated checks

```bash
python3 scripts/verify-static.py
# Java tests for each service, including auth and IAM:
make test
# Full runtime check (requires Docker, .env, and running services):
make integration-test
```

`make test` executes Maven tests in containers. `make integration-test` builds/starts Compose and runs `scripts/smoke-test.sh`, checking gateway, Nginx routes, product endpoint, and service health. Jenkins runs Maven tests for `iam-service` and all existing Java services, plus TypeScript checks and production builds for all three frontends before deployment stages.

## Important limitations

These additions provide Google identity verification, IAM separation, health checks, and automated tests for this layer. They do not by themselves make every ShopSphere business workflow production-complete. In particular, Google customer login creates a customer IAM record but does not yet create a matching row in the legacy auth-service `user_accounts` table; downstream APIs should use the UUID subject in the token and avoid assuming every token subject is a numeric auth database ID. Full checkout, real payment/webhook, refresh-token rotation, and AWS deployment still need separate end-to-end verification.
