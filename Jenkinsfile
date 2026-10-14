pipeline {
  agent any
  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '20'))
    timeout(time: 90, unit: 'MINUTES')
  }
  environment {
    AWS_REGION = 'ap-south-1'
    PROJECT_NAME = 'shopsphere'
    APP_STACK = 'shopsphere-app'
    ECR_STACK = 'shopsphere-ecr'
    // Configure the Jenkins agent with an AWS IAM role or short-lived credentials.
    // Do not store AWS access keys, database passwords, or JWT secrets in this file.
  }
  stages {
    stage('Checkout') {
      steps { checkout scm }
    }
    stage('Backend tests') {
      steps {
        sh '''
          set -eu
          for service in auth-service product-service api-gateway order-service payment-service notification-service; do
            docker run --rm -v "$WORKSPACE/$service:/workspace" -w /workspace \\
              maven:3.9-eclipse-temurin-21 mvn -B test
          done
        '''
      }
    }
    stage('CloudFormation validation') {
      steps {
        sh '''
          set -eu
          aws sts get-caller-identity --region "$AWS_REGION"
          aws cloudformation validate-template --region "$AWS_REGION" --template-body file://infrastructure/ecr-bootstrap.yaml >/dev/null
          aws cloudformation validate-template --region "$AWS_REGION" --template-body file://infrastructure/application.yaml >/dev/null
        '''
      }
    }
    stage('Build, push and deploy') {
      when { branch 'main' }
      steps {
        sh '''
          set -eu
          BUILD_NUMBER="$BUILD_NUMBER" ./scripts/deploy.sh
        '''
      }
    }
    stage('Deployment smoke test') {
      when { branch 'main' }
      steps {
        sh '''
          set -eu
          URL="$(aws cloudformation describe-stacks --region "$AWS_REGION" --stack-name "$APP_STACK" --query "Stacks[0].Outputs[?OutputKey=='StorefrontUrl'].OutputValue | [0]" --output text)"
          API="$(aws cloudformation describe-stacks --region "$AWS_REGION" --stack-name "$APP_STACK" --query "Stacks[0].Outputs[?OutputKey=='ApiProductsUrl'].OutputValue | [0]" --output text)"
          echo "Checking storefront: $URL"
          curl --fail --retry 18 --retry-delay 10 --retry-all-errors "$URL" >/dev/null
          echo "Checking product API: $API"
          curl --fail --retry 18 --retry-delay 10 --retry-all-errors "$API" | head -c 1000
        '''
      }
    }
  }
  post {
    always { echo 'Pipeline finished. Review test results, CloudFormation events, ECS service events, and CloudWatch logs.' }
  }
}
