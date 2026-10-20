variable "aws_region" {
  description = "AWS region to provision into."
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Deployment environment name (e.g. dev, staging, prod). Used as a naming/tagging suffix."
  type        = string
  default     = "dev"
}

variable "project_name" {
  description = "Short name used as a prefix for every resource this stack creates."
  type        = string
  default     = "bootupgrade"
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC."
  type        = string
  default     = "10.42.0.0/16"
}

variable "availability_zones" {
  description = "Availability zones to spread public/private subnets across."
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b"]
}

variable "api_image" {
  description = "Container image (repository:tag or ECR URI) for the api service. Built from api/Dockerfile."
  type        = string
  default     = "ghcr.io/bootupgrade/api:latest"
}

variable "api_container_port" {
  description = "Port the api container listens on (matches server.port in application.yml)."
  type        = number
  default     = 8080
}

variable "api_desired_count" {
  description = "Number of ECS Fargate tasks to run for the api service."
  type        = number
  default     = 2
}

variable "api_cpu" {
  description = "Fargate task CPU units for the api service."
  type        = number
  default     = 512
}

variable "api_memory" {
  description = "Fargate task memory (MiB) for the api service."
  type        = number
  default     = 1024
}

variable "db_name" {
  description = "Postgres database name."
  type        = string
  default     = "bootupgrade"
}

variable "db_username" {
  description = "Postgres master username. The password is generated and stored in Secrets Manager, never passed as a plain variable."
  type        = string
  default     = "bootupgrade_app"
}

variable "db_instance_class" {
  description = "RDS instance class."
  type        = string
  default     = "db.t4g.micro"
}

variable "db_allocated_storage" {
  description = "RDS allocated storage in GB."
  type        = number
  default     = 20
}

variable "db_engine_version" {
  description = "Postgres engine version."
  type        = string
  default     = "16.4"
}

variable "dashboard_domain_aliases" {
  description = "Optional CloudFront alternate domain names (CNAMEs) for the dashboard. Empty by default (uses the generated *.cloudfront.net domain)."
  type        = list(string)
  default     = []
}
