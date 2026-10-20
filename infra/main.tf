module "network" {
  source = "./modules/network"

  project_name       = var.project_name
  environment        = var.environment
  vpc_cidr           = var.vpc_cidr
  availability_zones = var.availability_zones
  api_container_port = var.api_container_port
}

module "rds" {
  source = "./modules/rds"

  project_name       = var.project_name
  environment        = var.environment
  vpc_id             = module.network.vpc_id
  private_subnet_ids = module.network.private_subnet_ids

  allowed_security_group_ids = [module.network.api_task_security_group_id]

  db_name              = var.db_name
  db_username          = var.db_username
  db_instance_class    = var.db_instance_class
  db_allocated_storage = var.db_allocated_storage
  db_engine_version    = var.db_engine_version
}

module "ecs" {
  source = "./modules/ecs"

  project_name       = var.project_name
  environment        = var.environment
  vpc_id             = module.network.vpc_id
  public_subnet_ids  = module.network.public_subnet_ids
  private_subnet_ids = module.network.private_subnet_ids

  alb_security_group_id      = module.network.alb_security_group_id
  api_task_security_group_id = module.network.api_task_security_group_id

  container_image = var.api_image
  container_port  = var.api_container_port
  desired_count   = var.api_desired_count
  cpu             = var.api_cpu
  memory          = var.api_memory

  db_endpoint               = module.rds.endpoint
  db_name                   = module.rds.db_name
  db_credentials_secret_arn = module.rds.credentials_secret_arn
}

module "dashboard_site" {
  source = "./modules/static_site"

  project_name   = var.project_name
  environment    = var.environment
  domain_aliases = var.dashboard_domain_aliases
}
