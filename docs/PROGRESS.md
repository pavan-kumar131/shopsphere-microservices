# ShopSphere AWS Deployment: Progress

## Goal
Deploy all services on AWS, built by hand, learning-focused.

## Decisions
- Staging first: 2 EC2 instances in own VPC (app server public, DB server private)
- S3 only for product images; frontends run as containers
- AWS foundation done manually in console first
- Single entry point: nginx routes / (customer), /seller, /admin, /api (gateway)
- Later: ECS, API Gateway, SNS/SQS, Jenkins, Prometheus, Grafana

## Done
- [x] Merge conflicts resolved
- [x] Local docker compose runs, all services healthy (nginx on :3001)
- [x] Fixed order-service JWT (was hardcoded HS256, tokens are HS384)
- [x] Order creation works (201)
- [x] AWS: VPC, 4 subnets, IGW, route tables, 2 security groups, app + DB EC2 created
- [x] Docker + 4GB swap installed on app server

## Next
- [ ] RS256 JWT (auth-service signs, others verify with public key)
- [ ] Payment service: DB persistence started (entity+repo), Razorpay + verify + webhook next
- [ ] Deploy to EC2 (app server), Postgres on DB server
- [ ] Domain + HTTPS

## Notes
- Compose publishes nginx 3001:80 locally; on server use 80:80, never publish 5432/8080
- Tokens last 15 minutes
