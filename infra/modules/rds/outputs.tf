output "endpoint" {
  value = aws_db_instance.this.endpoint
}

output "db_name" {
  value = aws_db_instance.this.db_name
}

output "security_group_id" {
  value = aws_security_group.db.id
}

output "credentials_secret_arn" {
  value = aws_secretsmanager_secret.db_credentials.arn
}

output "db_subnet_ids" {
  description = "Exposed for cross-module integration tests, not consumed by the root module."
  value       = aws_db_subnet_group.this.subnet_ids
}
