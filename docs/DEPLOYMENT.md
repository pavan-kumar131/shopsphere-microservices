# ShopSphere AWS deployment guide

This repository currently implements the **working learning storefront slice**: Next.js frontend, Spring Cloud Gateway, Spring Boot auth and product services, and PostgreSQL. The cart is browser-local. Checkout is a demo confirmation; there is no persisted order service or real payment integration in this codebase. Do not market it as a production commerce/payment system.

## Before you deploy

- AWS account with permission to create CloudFormation, VPC/subnets/routes/security groups, ECS/Fargate, ECR, ALB, RDS, Cloud Map, IAM roles, CloudWatch Logs and Secrets Manager resources.
- AWS CLI v2 authenticated with an IAM role/profile; Docker Engine; Git.
- Deploy in a region that supports Fargate, RDS PostgreSQL, and the listed services. Default region is `ap-south-1`.
- Set an AWS Budget before deploying. ALB, RDS, Fargate, CloudWatch logs and ECR can incur charges. The RDS database has a snapshot deletion policy; deleting the stack may leave a billable snapshot.
- The template currently exposes HTTP on port 80 only. It is for a demo. Before public production use, add an ACM certificate/HTTPS listener, domain, WAF/rate limiting, stricter egress, and a production authentication/session design.

## One-command manual deployment

From the repository root:

```bash
aws sts get-caller-identity
export AWS_REGION=ap-south-1
export PROJECT_NAME=shopsphere
export ADMIN_EMAIL=your-admin-email@example.com
# Optional: choose a small supported RDS class and task count
export DB_INSTANCE_CLASS=db.t3.micro
export DESIRED_COUNT=1
./scripts/deploy.sh
```

The script creates/updates the ECR bootstrap stack, builds all four Docker images, pushes unique immutable Git/build tags, validates CloudFormation templates, then deploys the app stack. It prints the storefront and API URLs. Initial RDS provisioning can take several minutes. The script uses your active AWS CLI identity; it does not store AWS keys in source files.

If running outside Jenkins, use `AWS_PROFILE=your-profile` if needed. Do not set `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` in a committed file.

## Retrieve the initial admin password

The password is generated in Secrets Manager and injected into the auth task at runtime. Get the secret ARN from stack output, then retrieve it securely on your own machine:

```bash
aws cloudformation describe-stacks --stack-name shopsphere-app --region "$AWS_REGION" \
  --query "Stacks[0].Outputs[?OutputKey=='AdminSecretArn'].OutputValue | [0]" --output text
aws secretsmanager get-secret-value --secret-id '<AdminSecretArn-from-output>' --region "$AWS_REGION" \
  --query SecretString --output text
```

The JSON includes the generated `password`. Do not paste the secret value into tickets, chat, source control, or build logs. The admin account is seeded only if that email does not already exist in the database. Changing the secret later does not automatically reset an existing database user's password.

## Verify deployment

```bash
aws ecs list-services --cluster shopsphere-cluster --region "$AWS_REGION"
aws ecs describe-services --cluster shopsphere-cluster \
  --services shopsphere-frontend shopsphere-api-gateway shopsphere-auth-service shopsphere-product-service \
  --region "$AWS_REGION" --query 'services[*].[serviceName,runningCount,desiredCount,deployments[0].rolloutState]'
```

Open the `StorefrontUrl` output. The product endpoint is `ApiProductsUrl`. Test account registration/login from the UI. Admin CRUD requires signing in with the seeded admin account.

For diagnostics:

```bash
aws ecs describe-services --cluster shopsphere-cluster --services shopsphere-api-gateway shopsphere-auth-service shopsphere-product-service --region "$AWS_REGION" --query 'services[*].events[0:5]'
aws logs tail /ecs/shopsphere --since 15m --follow --region "$AWS_REGION"
aws cloudformation describe-stack-events --stack-name shopsphere-app --region "$AWS_REGION" --max-items 30
```

## Deploy manually without the helper script

1. Create the repositories: deploy `infrastructure/ecr-bootstrap.yaml` as stack `shopsphere-ecr` with `ProjectName=shopsphere`.
2. Get repository URIs from stack outputs and authenticate Docker to ECR using `aws ecr get-login-password`.
3. Build and push each image with a unique tag:

```bash
# Substitute the actual ECR repository URI and unique tag for each service.
docker build --build-arg NEXT_PUBLIC_API_BASE_URL='' -t <frontend-repo>:<unique-tag> ./frontend
docker build -t <gateway-repo>:<unique-tag> ./api-gateway
docker build -t <auth-repo>:<unique-tag> ./auth-service
docker build -t <product-repo>:<unique-tag> ./product-service
docker push <frontend-repo>:<unique-tag>
docker push <gateway-repo>:<unique-tag>
docker push <auth-repo>:<unique-tag>
docker push <product-repo>:<unique-tag>
```

4. Deploy `infrastructure/application.yaml` as stack `shopsphere-app` with `--capabilities CAPABILITY_IAM`, passing `ProjectName`, `FrontendImage`, `GatewayImage`, `AuthImage`, and `ProductImage` as full ECR URI plus tag. Optionally pass `AdminEmail`, `DbInstanceClass`, `DesiredCount`, and `AllowedHttpCidr`.
5. Wait for CloudFormation completion and ECS services to stabilize. Use the stack outputs and verification commands above.

## Install prerequisites automatically (Ubuntu)

Run from the extracted project directory:

```bash
sudo ./scripts/bootstrap-ubuntu.sh
```

This installs Docker Engine/Compose, AWS CLI v2, Git, Java 21 and utilities. To install Jenkins as well on a dedicated Ubuntu host:

```bash
sudo ./scripts/bootstrap-ubuntu.sh --with-jenkins
```

This installs the Jenkins service, but you must still configure Jenkins plugins, Git webhook, AWS IAM role/credentials and job. Docker access grants Jenkins root-equivalent privileges; use an isolated build host. Spring Boot and Maven dependencies are resolved inside the Java 21 Maven Docker build image; Node.js/npm are supplied by the frontend Docker build image. ECS Fargate does not need these runtimes installed on the host. See `docs/AUTOMATIC-DEPENDENCIES.md`.

## Jenkins CI/CD

Use a Jenkins **Multibranch Pipeline** or a pipeline job configured to build the `main` branch. The Jenkins agent needs Docker, AWS CLI v2, Git, outbound access to Maven/npm/ECR, and AWS permissions for the resources above. Prefer an attached IAM role or short-lived assumed-role credentials; do not hard-code AWS keys.

The pipeline runs Maven tests inside Maven containers, validates both templates, and only deploys on `main`. Configure a webhook in your Git provider to trigger the job on push. Set region/stack names in the Jenkinsfile for your environment. The Jenkins identity must have the IAM permissions listed in `docs/IAM-POLICY.md` (review and scope them to your account before use).

## Tear down demo resources

`./scripts/destroy-demo.sh` deletes the app stack only after a typed confirmation. The database snapshot is retained, and the ECR stack/repositories are deliberately retained. Delete snapshots and ECR repositories separately only after verifying you no longer need them.
