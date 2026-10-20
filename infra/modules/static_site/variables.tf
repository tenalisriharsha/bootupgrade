variable "project_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "domain_aliases" {
  description = "Optional CloudFront alternate domain names (CNAMEs)."
  type        = list(string)
  default     = []
}
