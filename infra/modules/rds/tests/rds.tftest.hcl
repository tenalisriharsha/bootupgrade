mock_provider "aws" {}
mock_provider "random" {}

variables {
  project_name               = "bootupgrade"
  environment                = "dev"
  vpc_id                     = "vpc-0123456789abcdef0"
  private_subnet_ids         = ["subnet-aaaa1111", "subnet-bbbb2222"]
  allowed_security_group_ids = ["sg-apitask1234"]
  db_name                    = "bootupgrade"
  db_username                = "bootupgrade_app"
  db_instance_class          = "db.t4g.micro"
  db_allocated_storage       = 20
  db_engine_version          = "16.4"
}

run "instance_naming_and_sizing" {
  command = plan

  assert {
    condition     = aws_db_instance.this.identifier == "bootupgrade-dev-db"
    error_message = "DB identifier must be <project>-<environment>-db"
  }

  assert {
    condition     = aws_db_instance.this.instance_class == "db.t4g.micro"
    error_message = "Instance class must come from the db_instance_class variable"
  }

  assert {
    condition     = aws_db_instance.this.publicly_accessible == false
    error_message = "RDS must never be publicly accessible"
  }
}

run "not_publicly_reachable_and_encrypted" {
  command = plan

  assert {
    condition     = aws_db_instance.this.storage_encrypted == true
    error_message = "Database storage must be encrypted at rest"
  }
}

run "ingress_scoped_to_allowed_security_groups_only" {
  command = plan

  assert {
    condition     = length(aws_security_group_rule.db_ingress_from_api) == 1
    error_message = "Expected exactly one ingress rule per allowed security group"
  }

  assert {
    condition     = aws_security_group_rule.db_ingress_from_api[0].source_security_group_id == "sg-apitask1234"
    error_message = "Postgres ingress must be scoped to the api task security group, never a CIDR block"
  }

  assert {
    condition     = aws_security_group_rule.db_ingress_from_api[0].from_port == 5432 && aws_security_group_rule.db_ingress_from_api[0].to_port == 5432
    error_message = "Ingress must be scoped to the Postgres port"
  }
}

run "credentials_never_stored_in_plan_as_plain_variable" {
  command = plan

  assert {
    condition     = aws_secretsmanager_secret.db_credentials.name == "bootupgrade-dev-db-credentials"
    error_message = "Secret name must follow the <project>-<environment>-db-credentials naming convention"
  }
}
