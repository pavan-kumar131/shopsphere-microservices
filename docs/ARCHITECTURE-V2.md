# ShopSphere architecture v2

## Runtime topology

- **Nginx reverse proxy** is the public web entry point. `/` routes to the customer Next.js container, `/seller` to the seller Next.js container, `/admin` to the admin Next.js container, and `/api/*` to Spring Cloud Gateway.
- **Three frontend containers** are built from the same Next.js source. They are isolated containers with separate ports and can be replaced with separate profile-specific builds later. The route-level UI is shared; a separate codebase/brand per profile is not implied.
- **API Gateway** routes `/api/auth/*` to Auth Service and `/api/products/*` to Product Service.
- **Auth Service** handles registration/login and issues signed JWTs with `CUSTOMER`, `SELLER`, or `ADMIN` role claims. Public registration cannot create ADMIN. Seller approval workflow and per-seller product ownership still need implementation before this is a marketplace production launch.
- **Product Service** persists product metadata in PostgreSQL. Product images are stored in a private S3 bucket; seller/admin gets a short-lived presigned PUT URL, and public reads go through CloudFront. ECS task role is limited to `s3:PutObject` under `products/*`.
- **RDS PostgreSQL** is private. Secrets Manager stores database/JWT/admin secrets. CloudWatch receives container logs.
- **ECR + Jenkins + CloudFormation** build/push versioned images and deploy the stack. Run `scripts/deploy.sh` manually or run the Jenkins pipeline from `main`.

## Product image flow

1. Seller selects JPEG/PNG/WebP image in `/seller`.
2. Browser requests `POST /api/products/image-upload-url` with JWT and content type.
3. Product Service validates SELLER/ADMIN role and content type, then returns a 5-minute presigned S3 PUT URL and CloudFront image URL.
4. Browser uploads directly to S3, avoiding routing image bytes through the API containers.
5. Product metadata stores the CloudFront URL.

Current limitations: request body size is not enforced by the signed URL endpoint beyond file type, and the browser form has no image dimension/size precheck. Add a signed content-length range or upload proxy/policy and image malware/content scanning before production. S3 lifecycle policy for old images is not configured by default.

## Compression and AWS charges

Nginx negotiates gzip based on the browser's `Accept-Encoding`. If a browser does not advertise gzip, Nginx sends an uncompressed response; the browser handles decompression transparently. CloudFront `Compress: true` is enabled for cached image delivery and compressible text content. Images such as JPEG/WebP are already compressed and usually should not be gzipped again. Brotli is not enabled in this Nginx image; use CloudFront's supported compression where appropriate or build an Nginx image with a Brotli module if explicitly required.

There is no universal AWS per-request fee based only on payload bytes. Depending on the path, costs can include API Gateway requests, ALB capacity/LCUs and processed bytes, CloudFront requests/data transfer, internet data transfer out, S3 storage/requests, and logs. Compression may lower bytes transferred and some byte-related cost dimensions; it does not reduce every request-based charge. CloudWatch/Nginx can measure bytes, but exact billable usage must be checked in Cost Explorer/CUR. Avoid logging request bodies or secrets.

## Services planned for the next marketplace phase

The current runnable application slice contains gateway, auth, product, three frontends, Nginx, PostgreSQL, and S3/CloudFront image delivery. Separate seller, cart, order, inventory, payment, shipping, notification, review, promotion, and analytics services from the broader roadmap are **not yet implemented** in this package. Add these as independently tested services before claiming the entire marketplace is complete. Payment integration must use a provider-hosted checkout/tokenization and never store card PAN/CVV.
