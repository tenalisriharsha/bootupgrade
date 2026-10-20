output "alb_dns_name" {
  value = aws_lb.api.dns_name
}

output "cluster_name" {
  value = aws_ecs_cluster.this.name
}

output "service_name" {
  value = aws_ecs_service.api.name
}

output "task_definition_container_definitions" {
  description = "Rendered container_definitions JSON — exposed for cross-module integration tests, not consumed by the root module."
  value       = aws_ecs_task_definition.api.container_definitions
}

output "alb_subnet_ids" {
  value = aws_lb.api.subnets
}
