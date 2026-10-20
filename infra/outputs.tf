output "api_load_balancer_dns_name" {
  description = "Public DNS name of the api's ALB. The dashboard's environment.apiBaseUrl should point here (fronted by HTTPS/a custom domain in a real deployment)."
  value       = module.ecs.alb_dns_name
}

output "ecs_cluster_name" {
  value = module.ecs.cluster_name
}

output "ecs_service_name" {
  value = module.ecs.service_name
}

output "db_endpoint" {
  value = module.rds.endpoint
}

output "db_credentials_secret_arn" {
  description = "Secrets Manager ARN holding the generated Postgres master credentials."
  value       = module.rds.credentials_secret_arn
}

output "dashboard_bucket_name" {
  value = module.dashboard_site.bucket_name
}

output "dashboard_url" {
  description = "CloudFront domain serving the built Angular dashboard."
  value       = "https://${module.dashboard_site.distribution_domain_name}"
}
