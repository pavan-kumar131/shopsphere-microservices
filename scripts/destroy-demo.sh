#!/usr/bin/env bash
set -Eeuo pipefail
AWS_REGION="${AWS_REGION:-ap-south-1}"
APP_STACK="${APP_STACK:-shopsphere-app}"
ECR_STACK="${ECR_STACK:-shopsphere-ecr}"
echo "This removes the ECS/ALB/network stack. The RDS database has DeletionPolicy Snapshot, so a final snapshot is retained. ECR repositories are retained unless you delete them separately."
read -r -p "Type DELETE-$APP_STACK to continue: " confirm
[[ "$confirm" == "DELETE-$APP_STACK" ]] || { echo 'Cancelled'; exit 1; }
aws cloudformation delete-stack --region "$AWS_REGION" --stack-name "$APP_STACK"
aws cloudformation wait stack-delete-complete --region "$AWS_REGION" --stack-name "$APP_STACK"
echo "App stack deleted. ECR stack kept. Delete it manually only after confirming you no longer need images."
