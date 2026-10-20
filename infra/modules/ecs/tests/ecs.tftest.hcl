mock_provider "aws" {
  override_data {
    target = data.aws_iam_policy_document.ecs_tasks_assume_role
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
    target = data.aws_iam_policy_document.read_db_secret
    values = {
      json = jsonencode({
        Version = "2012-10-17"
        Statement = [{
          Effect   = "Allow"
          Action   = "secretsmanager:GetSecretValue"
          Resource = "arn:aws:secretsmanager:us-east-1:123456789012:secret:bootupgrade-dev-db-credentials-xyz"
        }]
      })
    }
  }

  override_data {
    target = data.aws_region.current
    values = {
      name = "us-east-1"
    }
  }
}

variables {
  project_name       = "bootupgrade"
  environment        = "dev"
  vpc_id             = "vpc-0123456789abcdef0"
  public_subnet_ids  = ["subnet-pub1111", "subnet-pub2222"]
  private_subnet_ids = ["subnet-priv1111", "subnet-priv2222"]

  alb_security_group_id      = "sg-alb1234"
  api_task_security_group_id = "sg-apitask1234"

  container_image = "ghcr.io/bootupgrade/api:test"
  container_port  = 8080
  desired_count   = 2
  cpu             = 512
  memory          = 1024

  db_endpoint               = "bootupgrade-dev-db.abc123.us-east-1.rds.amazonaws.com:5432"
  db_name                   = "bootupgrade"
  db_credentials_secret_arn = "arn:aws:secretsmanager:us-east-1:123456789012:secret:bootupgrade-dev-db-credentials-xyz"
}

run "cluster_and_service_naming" {
  command = plan

  assert {
    condition     = aws_ecs_cluster.this.name == "bootupgrade-dev-cluster"
    error_message = "Cluster name must be <project>-<environment>-cluster"
  }

  assert {
    condition     = aws_ecs_service.api.desired_count == 2
    error_message = "Desired count must come from the desired_count variable"
  }

  assert {
    condition     = aws_ecs_service.api.launch_type == "FARGATE"
    error_message = "This stack is Fargate-only, no EC2 launch type"
  }
}

run "task_definition_uses_fargate_awsvpc_networking" {
  command = plan

  assert {
    condition     = aws_ecs_task_definition.api.network_mode == "awsvpc"
    error_message = "Fargate requires awsvpc networking mode"
  }

  assert {
    condition     = contains(aws_ecs_task_definition.api.requires_compatibilities, "FARGATE")
    error_message = "Task definition must declare FARGATE compatibility"
  }
}

run "db_password_never_passed_as_plaintext_environment_variable" {
  command = plan

  assert {
    condition     = !strcontains(aws_ecs_task_definition.api.container_definitions, "DB_PASSWORD\":\"")
    error_message = "DB_PASSWORD must come from the secrets block (Secrets Manager), never a literal environment value"
  }

  assert {
    condition     = strcontains(aws_ecs_task_definition.api.container_definitions, "bootupgrade-dev-db-credentials-xyz:username::")
    error_message = "DB_USERNAME must resolve from the rds module's Secrets Manager secret ARN"
  }

  assert {
    condition     = strcontains(aws_ecs_task_definition.api.container_definitions, "jdbc:postgresql://bootupgrade-dev-db.abc123.us-east-1.rds.amazonaws.com:5432/bootupgrade")
    error_message = "DB_URL must be built from the rds module's endpoint and db_name outputs"
  }
}

run "load_balancer_health_check_targets_the_api" {
  command = plan

  assert {
    condition     = aws_lb_target_group.api.health_check[0].path == "/api/runs"
    error_message = "Health check must hit a real api endpoint"
  }

  assert {
    condition     = aws_lb_target_group.api.port == 8080
    error_message = "Target group port must match the container port variable"
  }
}
