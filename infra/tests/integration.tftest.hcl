mock_provider "aws" {
  override_data {
    target = module.ecs.data.aws_iam_policy_document.ecs_tasks_assume_role
    values = {
      json = jsonencode({
        Version = "2012-10-17"
        Statement = [{
          Effect    = "Allow"
          Action    = "sts:AssumeRole"
          Principal = { Service = "ecs-tasks.amazonaws.com" }
        }]
      })
    }
  }

  override_data {
    target = module.ecs.data.aws_iam_policy_document.read_db_secret
    values = {
      json = jsonencode({
        Version   = "2012-10-17"
        Statement = [{ Effect = "Allow", Action = "secretsmanager:GetSecretValue", Resource = "*" }]
      })
    }
  }

  override_data {
    target = module.dashboard_site.data.aws_iam_policy_document.dashboard_bucket_policy
    values = {
      json = jsonencode({
        Version   = "2012-10-17"
        Statement = [{ Effect = "Allow", Action = "s3:GetObject", Principal = { Service = "cloudfront.amazonaws.com" } }]
      })
    }
  }

  # The mock provider's default fake values for computed ARN-typed
  # attributes are opaque random strings, which fail the provider's own
  # client-side "looks like an arn:" validation the moment a downstream
  # resource (the listener, the task definition) consumes them. Give
  # these upstream resources real-shaped ARNs so the rest of the plan
  # still exercises the actual cross-module wiring.
  override_resource {
    target = module.ecs.aws_lb.api
    values = {
      arn = "arn:aws:elasticloadbalancing:us-east-1:123456789012:loadbalancer/app/bootupgrade-test-api-alb/abc123"
    }
  }

  override_resource {
    target = module.ecs.aws_lb_target_group.api
    values = {
      arn = "arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/bootupgrade-test-api-tg/def456"
    }
  }

  override_resource {
    target = module.ecs.aws_iam_role.execution
    values = {
      arn = "arn:aws:iam::123456789012:role/bootupgrade-test-api-execution-role"
    }
  }

  override_resource {
    target = module.ecs.aws_iam_role.task
    values = {
      arn = "arn:aws:iam::123456789012:role/bootupgrade-test-api-task-role"
    }
  }
}

mock_provider "random" {}

variables {
  project_name = "bootupgrade"
  environment  = "test"
}

# This is the one test that matters more than any single module's own
# tests: it proves the three modules actually hand each other the right
# values — not just that each module is internally consistent in
# isolation (modules/*/tests/*.tftest.hcl already cover that).
run "ecs_task_definition_resolves_rds_secret_and_endpoint" {
  command = apply

  assert {
    condition     = strcontains(module.ecs.task_definition_container_definitions, module.rds.credentials_secret_arn)
    error_message = "The api task definition must reference the actual secret ARN rds produced, not a stale/hardcoded one"
  }

  assert {
    condition     = strcontains(module.ecs.task_definition_container_definitions, module.rds.endpoint)
    error_message = "DB_URL must be built from rds's real endpoint output"
  }
}

run "ecs_and_rds_share_the_same_network_module_vpc" {
  command = apply

  assert {
    condition     = toset(module.ecs.alb_subnet_ids) == toset(module.network.public_subnet_ids)
    error_message = "The ALB must sit in the network module's public subnets"
  }

  assert {
    condition     = toset(module.rds.db_subnet_ids) == toset(module.network.private_subnet_ids)
    error_message = "RDS must sit in the network module's private subnets, never the public ones"
  }
}

run "dashboard_site_is_independent_of_the_api_stack" {
  command = apply

  assert {
    condition     = module.dashboard_site.bucket_name != ""
    error_message = "The dashboard bucket must be created"
  }

  assert {
    condition     = module.dashboard_site.distribution_domain_name != ""
    error_message = "The CloudFront distribution must produce a usable domain name"
  }
}
