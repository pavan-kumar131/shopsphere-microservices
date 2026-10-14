# Automatic dependency installation

ShopSphere uses Docker multi-stage builds, so the application build does not require the operator to install Spring Boot, Maven, or Node.js globally on the deployment host:

- `auth-service/Dockerfile`, `product-service/Dockerfile`, and `api-gateway/Dockerfile` use a Maven + Eclipse Temurin Java 21 build image. Maven downloads the Spring Boot/Spring Cloud dependencies declared in each `pom.xml`, packages the executable JAR, and the runtime stage contains Java 21 JRE.
- `frontend/Dockerfile` uses Node.js 22 images and runs `npm install` and `npm run build` inside the image build.
- `nginx/Dockerfile` uses the official Nginx image.
- `docker-compose.yml` starts the prebuilt containers; `scripts/deploy.sh` builds the images and pushes them to ECR. ECS Fargate runs the images without requiring Java, Maven, Node.js, or Docker to be installed on the ECS host.

## Bootstrap an Ubuntu machine

For Ubuntu 22.04/24.04, install Docker Engine + Compose plugin, AWS CLI v2, Git, Java 21, jq and basic utilities:

```bash
sudo ./scripts/bootstrap-ubuntu.sh
```

To additionally install Jenkins on a dedicated Ubuntu host:

```bash
sudo ./scripts/bootstrap-ubuntu.sh --with-jenkins
```

The optional Jenkins install does not configure your Git provider, Jenkins plugins, AWS permissions, credentials, or webhook automatically. Complete those steps using `docs/DEPLOYMENT.md` and `docs/IAM-POLICY.md`. Treat Jenkins Docker access as root-equivalent and use a dedicated isolated host/agent.

## AWS permissions and credentials

The script intentionally does not create or save AWS access keys. On an AWS EC2 Jenkins agent, attach a least-privilege instance profile. On a workstation, use `aws configure sso` or another approved credential mechanism. Confirm access with `aws sts get-caller-identity` before deployment.

The bootstrap script installs tools on Ubuntu only. On Windows/macOS, use Docker Desktop and AWS CLI v2; Java/Maven/Node are still supplied by the Docker build images.
