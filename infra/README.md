# bootupgrade Infrastructure

Terraform provisioning the API and dashboard on AWS (ECS Fargate, RDS,
S3 + CloudFront). Built in Phase 5. Validated with `terraform fmt`/`validate`/
`plan` and `terraform test` with a mock provider — never applied against a
real account. See [PROGRESS.md](../PROGRESS.md) for the build plan.
