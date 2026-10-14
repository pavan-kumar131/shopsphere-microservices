# Cost and compression notes

- Browser sends `Accept-Encoding`; Nginx negotiates gzip. No JavaScript browser detection is required.
- `Vary: Accept-Encoding` is set so caches do not serve the wrong representation.
- CloudFront compression is enabled for the S3 image distribution. JPEG/PNG/WebP are already compressed; optimize image dimensions and generate WebP/AVIF variants for the largest savings.
- Images are uploaded straight to S3 using a five-minute presigned URL. API servers do not proxy image bytes.
- S3 is private and CloudFront is the public read path. Do not make the bucket public.
- Use CloudFront cache TTLs, lifecycle rules, ECR lifecycle policies, CloudWatch retention, AWS Budgets, and Cost Anomaly Detection.
- Request-level payload bytes can be logged/metricized but AWS billing is not a single charge per payload size. Use Cost Explorer/CUR and service-specific pricing.
