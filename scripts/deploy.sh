#!/usr/bin/env bash
set -Eeuo pipefail

# One-command manual deployment: creates ECR, builds/pushes images, then deploys ECS/RDS/ALB.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ "${ALLOW_PARTIAL_CORE_DEPLOY:-false}" != "true" ]]; then
  cat >&2 <<'NOTICE'
Refusing to deploy as a complete ShopSphere release: the current CloudFormation template
only deploys the core frontend, gateway, auth, product, and order services. Payment and
notification services plus several required business/observability services are not yet
included in AWS infrastructure. After reviewing docs/REQUIREMENTS-CROSSCHECK.md, set
ALLOW_PARTIAL_CORE_DEPLOY=true only if you intentionally want this incomplete core demo.
NOTICE
  exit 2
fi
AWS_REGION="${AWS_REGION:-ap-south-1}"
PROJECT_NAME="${PROJECT_NAME:-shopsphere}"
ECR_STACK="${ECR_STACK:-${PROJECT_NAME}-ecr}"
APP_STACK="${APP_STACK:-${PROJECT_NAME}-app}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@shopsphere.local}"
DB_INSTANCE_CLASS="${DB_INSTANCE_CLASS:-db.t3.micro}"
DESIRED_COUNT="${DESIRED_COUNT:-1}"
TAG="${IMAGE_TAG:-git-$(git -C "$ROOT" rev-parse --short=12 HEAD 2>/dev/null || echo source)-${BUILD_NUMBER:-$(date -u +%Y%m%d%H%M%S)}}"

command -v aws >/dev/null || { echo 'ERROR: AWS CLI v2 is required.' >&2; exit 1; }
command -v docker >/dev/null || { echo 'ERROR: Docker is required.' >&2; exit 1; }
aws sts get-caller-identity --region "$AWS_REGION" >/dev/null
ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text --region "$AWS_REGION")"
REGISTRY="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

printf '\n== 1/4: Create/update ECR repositories ==\n'
aws cloudformation deploy --region "$AWS_REGION" --stack-name "$ECR_STACK" \
  --template-file "$ROOT/infrastructure/ecr-bootstrap.yaml" \
  --parameter-overrides "ProjectName=$PROJECT_NAME" \
  --no-fail-on-empty-changeset

repo_uri() {
  aws cloudformation describe-stacks --region "$AWS_REGION" --stack-name "$ECR_STACK" \
    --query "Stacks[0].Outputs[?OutputKey=='$1'].OutputValue | [0]" --output text
}
FRONTEND_REPO="$(repo_uri FrontendRepositoryUri)"
SELLER_FRONTEND_REPO="$(repo_uri SellerFrontendRepositoryUri)"
ADMIN_FRONTEND_REPO="$(repo_uri AdminFrontendRepositoryUri)"
NGINX_REPO="$(repo_uri NginxRepositoryUri)"
GATEWAY_REPO="$(repo_uri GatewayRepositoryUri)"
AUTH_REPO="$(repo_uri AuthRepositoryUri)"
PRODUCT_REPO="$(repo_uri ProductRepositoryUri)"
ORDER_REPO="$(repo_uri OrderRepositoryUri)"

printf '\n== 2/4: Build and push immutable images (%s) ==\n' "$TAG"
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$REGISTRY"
docker build --pull -t "$FRONTEND_REPO:$TAG" --build-arg NEXT_PUBLIC_API_BASE_URL='' "$ROOT/frontend-customer"
docker build --pull -t "$SELLER_FRONTEND_REPO:$TAG" --build-arg NEXT_PUBLIC_API_BASE_URL='' "$ROOT/frontend-seller"
docker build --pull -t "$ADMIN_FRONTEND_REPO:$TAG" --build-arg NEXT_PUBLIC_API_BASE_URL='' "$ROOT/frontend-admin"
docker build --pull -t "$NGINX_REPO:$TAG" "$ROOT/nginx"
docker build --pull -t "$GATEWAY_REPO:$TAG" "$ROOT/api-gateway"
docker build --pull -t "$AUTH_REPO:$TAG" "$ROOT/auth-service"
docker build --pull -t "$PRODUCT_REPO:$TAG" "$ROOT/product-service"
docker build --pull -t "$ORDER_REPO:$TAG" "$ROOT/order-service"
for image in "$FRONTEND_REPO:$TAG" "$SELLER_FRONTEND_REPO:$TAG" "$ADMIN_FRONTEND_REPO:$TAG" "$NGINX_REPO:$TAG" "$GATEWAY_REPO:$TAG" "$AUTH_REPO:$TAG" "$PRODUCT_REPO:$TAG" "$ORDER_REPO:$TAG"; do docker push "$image"; done

printf '\n== 3/4: Validate and deploy AWS application stack ==\n'
for template in "$ROOT/infrastructure/ecr-bootstrap.yaml" "$ROOT/infrastructure/application.yaml"; do
  aws cloudformation validate-template --region "$AWS_REGION" --template-body "file://$template" >/dev/null
done
aws cloudformation deploy --region "$AWS_REGION" --stack-name "$APP_STACK" \
  --template-file "$ROOT/infrastructure/application.yaml" \
  --capabilities CAPABILITY_IAM \
  --parameter-overrides \
    "ProjectName=$PROJECT_NAME" \
    "FrontendImage=$FRONTEND_REPO:$TAG" \
    "SellerFrontendImage=$SELLER_FRONTEND_REPO:$TAG" \
    "AdminFrontendImage=$ADMIN_FRONTEND_REPO:$TAG" \
    "NginxImage=$NGINX_REPO:$TAG" \
    "GatewayImage=$GATEWAY_REPO:$TAG" \
    "AuthImage=$AUTH_REPO:$TAG" \
    "ProductImage=$PRODUCT_REPO:$TAG" \
    "OrderImage=$ORDER_REPO:$TAG" \
    "AdminEmail=$ADMIN_EMAIL" \
    "DbInstanceClass=$DB_INSTANCE_CLASS" \
    "DesiredCount=$DESIRED_COUNT" \
  --no-fail-on-empty-changeset

printf '\n== 4/4: Deployment outputs ==\n'
aws cloudformation describe-stacks --region "$AWS_REGION" --stack-name "$APP_STACK" \
  --query 'Stacks[0].Outputs[*].[OutputKey,OutputValue]' --output table
IMAGE_BUCKET="$(aws cloudformation describe-stacks --region "$AWS_REGION" --stack-name "$APP_STACK" --query "Stacks[0].Outputs[?OutputKey=='ProductImagesBucketName'].OutputValue | [0]" --output text)"
printf '\n== Upload sample product image assets to private S3 ==\n'
aws s3 cp "$ROOT/assets/product-images/seed/" "s3://$IMAGE_BUCKET/seed/" --recursive --region "$AWS_REGION" --cache-control 'public,max-age=31536000,immutable' --no-progress

printf '\nAdmin initial password secret ARN (retrieve value securely; do not paste it into chat):\n'
aws cloudformation describe-stacks --region "$AWS_REGION" --stack-name "$APP_STACK" \
  --query "Stacks[0].Outputs[?OutputKey=='AdminSecretArn'].OutputValue | [0]" --output text
printf '\nDeployment command completed. Wait for ECS services to become stable, then smoke-test the StorefrontUrl and ApiProductsUrl outputs.\n'
