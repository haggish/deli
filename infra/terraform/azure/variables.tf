variable "location" {
  description = "Azure region to deploy into"
  type        = string
  default     = "West Europe"
}

variable "resource_group_name" {
  description = "Name of the Azure resource group"
  type        = string
  default     = "deli-production"
}

variable "environment" {
  description = "Deployment environment name (e.g. production, staging)"
  type        = string
  default     = "production"
}

variable "cluster_name" {
  description = "Base name used across all resources"
  type        = string
  default     = "deli"
}

# AKS
variable "kubernetes_version" {
  description = "AKS Kubernetes version"
  type        = string
  default     = "1.31"
}

variable "aks_node_vm_size" {
  description = "VM size for AKS default node pool"
  type        = string
  default     = "Standard_D2s_v3"
}

variable "node_min_count" {
  description = "Minimum number of AKS worker nodes"
  type        = number
  default     = 1
}

variable "node_count" {
  description = "Initial number of AKS worker nodes"
  type        = number
  default     = 2
}

variable "node_max_count" {
  description = "Maximum number of AKS worker nodes"
  type        = number
  default     = 4
}

# PostgreSQL Flexible Server
variable "db_sku_name" {
  description = "SKU name for Azure Database for PostgreSQL Flexible Server"
  type        = string
  default     = "GP_Standard_D2s_v3"
}

variable "db_storage_mb_courier" {
  description = "Storage in MB for courierdb (100Gi = 102400)"
  type        = number
  default     = 102400
}

variable "db_storage_mb_gps" {
  description = "Storage in MB for gpsdb / TimescaleDB (500Gi = 512000)"
  type        = number
  default     = 512000
}

variable "db_admin_login" {
  description = "Administrator login for both PostgreSQL Flexible Servers"
  type        = string
  default     = "deladmin"
}

variable "db_admin_password" {
  description = "Administrator password for both PostgreSQL Flexible Servers"
  type        = string
  sensitive   = true
}

# Redis
variable "redis_sku_name" {
  description = "Redis SKU (Basic, Standard, Premium)"
  type        = string
  default     = "Standard"
}

variable "redis_capacity" {
  description = "Redis cache size (Standard C1 = 1)"
  type        = number
  default     = 1
}

# Event Hubs
variable "eventhubs_capacity" {
  description = "Event Hubs namespace throughput units"
  type        = number
  default     = 1
}
