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
