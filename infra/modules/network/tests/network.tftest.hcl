mock_provider "aws" {}

variables {
  project_name       = "bootupgrade"
  environment        = "dev"
  vpc_cidr           = "10.42.0.0/16"
  availability_zones = ["us-east-1a", "us-east-1b"]
  api_container_port = 8080
}

run "naming_follows_project_and_environment" {
  command = plan

  assert {
    condition     = aws_vpc.this.tags["Name"] == "bootupgrade-dev-vpc"
    error_message = "VPC name must be <project>-<environment>-vpc"
  }

  assert {
    condition     = aws_internet_gateway.this.tags["Name"] == "bootupgrade-dev-igw"
    error_message = "Internet gateway must follow the <project>-<environment>-igw naming convention"
  }
}

run "subnets_match_az_count" {
  command = plan

  assert {
    condition     = length(aws_subnet.public) == 2
    error_message = "Expected one public subnet per availability zone"
  }

  assert {
    condition     = length(aws_subnet.private) == 2
    error_message = "Expected one private subnet per availability zone"
  }

  assert {
    condition     = alltrue([for s in aws_subnet.public : s.map_public_ip_on_launch])
    error_message = "Public subnets must auto-assign public IPs so the ALB is reachable"
  }
}

run "subnet_cidrs_do_not_overlap" {
  command = plan

  assert {
    condition = length(distinct(concat(
      [for s in aws_subnet.public : s.cidr_block],
      [for s in aws_subnet.private : s.cidr_block],
    ))) == 4
    error_message = "All four carved subnet CIDRs must be distinct"
  }
}

run "api_task_security_group_only_allows_alb_on_container_port" {
  command = plan

  assert {
    condition     = alltrue([for rule in aws_security_group.api_task.ingress : rule.from_port == 8080 && rule.to_port == 8080])
    error_message = "The api task security group must open exactly the configured container port"
  }

  assert {
    condition     = alltrue([for rule in aws_security_group.api_task.ingress : rule.cidr_blocks == null || length(rule.cidr_blocks) == 0])
    error_message = "The api task security group must restrict ingress to the ALB security group, never a CIDR block"
  }
}

run "alb_security_group_is_public_on_80" {
  command = plan

  assert {
    condition     = alltrue([for rule in aws_security_group.alb.ingress : rule.from_port == 80 && rule.to_port == 80])
    error_message = "The ALB security group must accept HTTP on port 80"
  }
}
