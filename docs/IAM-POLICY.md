# Jenkins/AWS deploy identity permissions

This is a permission checklist, not a copy-paste production policy. Scope resource ARNs to your AWS account, region, stack names, ECR repositories and log groups before attaching it. CloudFormation may need `iam:PassRole` for the task roles created by the template.

The deployment identity needs:

- `sts:GetCallerIdentity`
- ECR auth and image operations: `ecr:GetAuthorizationToken`, `ecr:BatchCheckLayerAvailability`, `ecr:InitiateLayerUpload`, `ecr:UploadLayerPart`, `ecr:CompleteLayerUpload`, `ecr:PutImage`, `ecr:BatchGetImage`, `ecr:GetDownloadUrlForLayer`, `ecr:DescribeRepositories`, `ecr:DescribeImages`
- CloudFormation deploy/validate/describe/wait operations on the two named stacks
- Create/update/delete/read permissions for the resources declared in `infrastructure/application.yaml` during stack operations, including IAM role creation and `iam:PassRole`
- `ecs:DescribeServices`, `ecs:ListServices`, and `logs:DescribeLogStreams`/`logs:GetLogEvents` only if pipeline diagnostics require them

Prefer a dedicated deployment role and a separate runtime task execution role. Never give application tasks CloudFormation or ECR push permissions. Review CloudFormation change sets before production changes.
