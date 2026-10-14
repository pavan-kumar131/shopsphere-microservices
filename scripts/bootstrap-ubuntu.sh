#!/usr/bin/env bash
# Bootstrap an Ubuntu 22.04/24.04 workstation or Jenkins build agent for ShopSphere.
# Java/Maven/Node/Spring Boot application dependencies are installed inside Docker builds;
# host Java is installed for diagnostics and optional local development.
set -Eeuo pipefail
WITH_JENKINS=false
for arg in "$@"; do
  case "$arg" in
    --with-jenkins) WITH_JENKINS=true ;;
    -h|--help) echo "Usage: sudo $0 [--with-jenkins]"; exit 0 ;;
    *) echo "Unknown argument: $arg" >&2; exit 2 ;;
  esac
done
if [[ "$(id -u)" -ne 0 ]]; then echo "Run with sudo: sudo $0 [--with-jenkins]" >&2; exit 1; fi
if ! grep -qi ubuntu /etc/os-release; then echo 'This bootstrap supports Ubuntu only.' >&2; exit 1; fi
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y ca-certificates curl gnupg lsb-release unzip zip git jq make openjdk-21-jdk

# Docker's official apt repository
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
. /etc/os-release
cat > /etc/apt/sources.list.d/docker.sources <<DOCKERREPO
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: ${VERSION_CODENAME}
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
DOCKERREPO
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable --now docker

# AWS CLI v2 (avoid Ubuntu's older awscli package)
ARCH="$(uname -m)"
case "$ARCH" in x86_64) AWS_ARCH=x86_64 ;; aarch64|arm64) AWS_ARCH=aarch64 ;; *) echo "Unsupported architecture: $ARCH" >&2; exit 1 ;; esac
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-${AWS_ARCH}.zip" -o "$TMP/awscliv2.zip"
unzip -q "$TMP/awscliv2.zip" -d "$TMP"
if command -v aws >/dev/null 2>&1; then "$TMP/aws/install" --update; else "$TMP/aws/install"; fi

# The Jenkinsfile runs Maven and Node in Docker images, so host Maven/Node are not required.
if [[ "$WITH_JENKINS" == true ]]; then
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key -o /etc/apt/keyrings/jenkins-keyring.asc
  chmod a+r /etc/apt/keyrings/jenkins-keyring.asc
  echo "deb [signed-by=/etc/apt/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/" > /etc/apt/sources.list.d/jenkins.list
  apt-get update
  apt-get install -y jenkins
  systemctl enable --now jenkins
  # Jenkins needs Docker access for this project's pipeline. Membership in docker is effectively root.
  usermod -aG docker jenkins
  systemctl restart jenkins
  echo 'Jenkins installed. Install Pipeline, Git, and Credentials Binding plugins; configure Git webhook and least-privilege AWS credentials/instance role.'
  echo 'Security: docker-group membership grants root-equivalent control. Use a dedicated, isolated build agent.'
fi

printf '\nInstalled versions:\n'
java -version
docker --version
docker compose version
aws --version
git --version
printf '\nBootstrap complete. Configure AWS credentials/role, then run ./scripts/deploy.sh from the project directory.\n'
