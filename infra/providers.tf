provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project   = "bootupgrade"
      ManagedBy = "terraform"
      Env       = var.environment
    }
  }
}
