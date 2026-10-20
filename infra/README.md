# bootupgrade Infrastructure

Terraform provisioning the `api` and `dashboard` on AWS. Built in Phase 5,
validated with `terraform fmt`/`validate`/`plan` and `terraform test` against
a mocked AWS provider — **never applied against a real account**. Only dummy
credentials exist in the environment this was built in.

## What it provisions

```
infra/
├── modules/
│   ├── network/      VPC, 2 public + 2 private subnets (one NAT gateway),
│   │                 the ALB and api-task security groups
│   ├── ecs/           ECS Fargate cluster + service running the api
│   │                 container behind an ALB, task/execution IAM roles,
│   │                 CloudWatch log group
│   ├── rds/           RDS Postgres (private subnets only), master
│   │                 credentials generated and stored in Secrets Manager
│   │                 (never a plain variable), security group scoped to
│   │                 the api task security group on 5432
│   └── static_site/   S3 bucket (private, versioned) + CloudFront
│                       distribution (OAC, SPA 403/404→index.html fallback)
│                       serving the built Angular dashboard
├── main.tf / variables.tf / outputs.tf   Root module wiring the four
│                                          modules together
├── environments/dev.tfvars.example       Copy to dev.tfvars (gitignored)
│                                          before ever pointing this at a
│                                          real account
└── tests/integration.tftest.hcl          Cross-module wiring checks (the
                                           rds secret ARN/endpoint actually
                                           reach the ecs task definition,
                                           both sit in the right subnets)
```

The `api`'s `prod` Spring profile (`api/src/main/resources/application-prod.yml`)
reads `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` from the environment — the `ecs`
module's task definition sets `DB_URL` directly from the `rds` module's
endpoint/db_name outputs and resolves `DB_USERNAME`/`DB_PASSWORD` from the
Secrets Manager secret `rds` creates, so the real credential never appears
in a container's environment as a plain string or in Terraform's own output.
`api/Dockerfile` is the image the `ecs` module's `api_image` variable expects
(built and pushed to a registry outside Terraform — this stack doesn't build
images, only runs them).

## Module wiring

A module dependency cycle was the main design constraint: `ecs` needs `rds`'s
secret ARN/endpoint to build the task definition, and the natural instinct is
for `rds` to restrict its ingress to the ECS tasks' security group — which
would make `rds` depend on `ecs` right back. The fix is that the `alb` and
`api_task` security groups are owned by the `network` module (which neither
`ecs` nor `rds` needs anything back from), and both `ecs` and `rds` take the
relevant security group ID as an input. See the comment above the security
group resources in `modules/network/main.tf`.

## Validating (no AWS account needed)

```bash
cd infra
terraform fmt -check -recursive
terraform init
terraform validate

# Cross-module wiring, against a mocked AWS provider — no real credentials,
# no network calls to AWS:
terraform test

# Each module also carries its own focused test suite:
for m in network rds ecs static_site; do
  (cd "modules/$m" && terraform init -input=false && terraform test)
done
```

A real `terraform plan`/`apply` needs valid AWS credentials and is
deliberately **not** exercised here — this environment only has dummy
credentials, and `terraform plan` against them fails at the provider's own
`sts:GetCallerIdentity` credential check before it gets anywhere near
planning a resource. `terraform validate` plus the mocked-provider
`terraform test` suites are the validation this stack is held to.

## Known simplifications (see DAILY_REPORT.md for the full list)

- Single NAT gateway (not one per AZ) — fine for a dev-sized stack, a single
  point of failure in a real multi-AZ production deployment.
- No HTTPS listener/ACM certificate on the ALB or a custom CloudFront
  domain — both default to their AWS-issued domains (`*.amazonaws.com`,
  `*.cloudfront.net`), which already serve HTTPS for CloudFront but only
  HTTP for the ALB. A real deployment would terminate TLS at the ALB too.
- No CI/CD pipeline building `api/Dockerfile` or the dashboard's production
  build and pushing them anywhere — `api_image` and the dashboard's S3
  contents are both assumed to be supplied by a separate build step.
