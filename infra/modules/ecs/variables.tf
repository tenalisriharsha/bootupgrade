variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "public_subnet_ids" {
  type = list(string)
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "alb_security_group_id" {
  type = string
}

variable "api_task_security_group_id" {
  type = string
}

variable "container_image" {
  type = string
}

variable "container_port" {
  type = number
}

variable "desired_count" {
  type = number
}

variable "cpu" {
  type = number
}

variable "memory" {
  type = number
}

variable "db_endpoint" {
  description = "RDS endpoint (host:port) to build the prod JDBC URL from."
  type        = string
}

variable "db_name" {
  type = string
}

variable "db_credentials_secret_arn" {
  description = "Secrets Manager ARN holding {username, password} for the database."
  type        = string
}
