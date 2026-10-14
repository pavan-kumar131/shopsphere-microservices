# Implementation status — archive v4

## Updated in this archive
- Split frontend build contexts into `frontend-customer`, `frontend-seller`, and `frontend-admin`; updated Compose and Nginx upstreams.
- Added seller ownership (`seller_id`) to Product and server-side owner-or-admin checks for update/delete.
- Hardened Product/Payment/Notification JWT filters so downstream controller failures are not misreported as token failures.
- Added authenticated user ownership to sandbox payment records and owner checks for payment lookup.
- Added atomic conditional stock reservation/release endpoints in Product Service and authenticated internal Feign calls from Order Service; checkout attempts compensation on failure and releases stock on sandbox payment failure.
- Updated deployment script frontend contexts, added `Makefile`, offline static validator, and a requirement-by-requirement status matrix.

## Still incomplete
- See `docs/REQUIREMENTS-CROSSCHECK.md`. In particular: independent User/Seller/Admin/Inventory/Cart/Media/Shipping/Promotion/Review/Search/Wishlist/Return/Audit/Analytics services, RabbitMQ/outbox/DLQ, MinIO local storage, durable notifications, dedicated Inventory Service, durable inventory reconciliation/full checkout saga, refresh token lifecycle, real payment provider/webhooks/refunds, complete AWS IaC and monitoring.

## Validation
- Run `make validate` locally.
- Docker and Maven were unavailable in the authoring environment, and outbound package downloads were blocked; Java builds, frontend production builds, Docker runtime and integration tests have not been executed here.
