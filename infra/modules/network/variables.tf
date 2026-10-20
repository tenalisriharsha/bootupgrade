variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "vpc_cidr" {
  type = string
}

variable "availability_zones" {
  type = list(string)
}

variable "api_container_port" {
  description = "Port the api ECS tasks listen on; used for the task security group's ingress rule."
  type        = number
}
