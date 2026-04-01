# AKS
output "aks_cluster_name" {
  description = "AKS cluster name — use with: az aks get-credentials --name <value> --resource-group <rg>"
  value       = azurerm_kubernetes_cluster.main.name
}

output "aks_kube_config" {
  description = "Raw kubeconfig for the AKS cluster"
  value       = azurerm_kubernetes_cluster.main.kube_config_raw
  sensitive   = true
}

output "aks_oidc_issuer_url" {
  description = "OIDC issuer URL — needed when adding further Workload Identity federations"
  value       = azurerm_kubernetes_cluster.main.oidc_issuer_url
}

# PostgreSQL
output "postgresql_courierdb_fqdn" {
  description = "FQDN for courierdb — use as spring.datasource.url host"
  value       = azurerm_postgresql_flexible_server.courierdb.fqdn
}

output "postgresql_gpsdb_fqdn" {
  description = "FQDN for gpsdb (TimescaleDB) — use as spring.datasource.url host in location-service"
  value       = azurerm_postgresql_flexible_server.gpsdb.fqdn
}

# Redis
output "redis_hostname" {
  description = "Redis hostname — use as spring.data.redis.host"
  value       = azurerm_redis_cache.main.hostname
}

output "redis_primary_access_key" {
  description = "Redis primary access key — use as spring.data.redis.password"
  value       = azurerm_redis_cache.main.primary_access_key
  sensitive   = true
}

# Event Hubs (Kafka)
output "eventhubs_namespace_name" {
  description = "Event Hubs namespace name"
  value       = azurerm_eventhub_namespace.main.name
}

output "eventhubs_bootstrap_server" {
  description = "Kafka-compatible bootstrap server — use as spring.kafka.bootstrap-servers"
  value       = "${azurerm_eventhub_namespace.main.name}.servicebus.windows.net:9093"
}

output "eventhubs_connection_string" {
  description = "Event Hubs Kafka connection string (SASL_SSL)"
  value       = azurerm_eventhub_namespace_authorization_rule.app.primary_connection_string
  sensitive   = true
}

# Storage
output "storage_account_name" {
  description = "Azure Storage account name — use as s3.bucket-name context or azure.storage.account-name"
  value       = azurerm_storage_account.main.name
}

output "storage_account_primary_connection_string" {
  description = "Storage account primary connection string"
  value       = azurerm_storage_account.main.primary_connection_string
  sensitive   = true
}

# IAM
output "delivery_service_identity_client_id" {
  description = "Client ID for the delivery-service managed identity — annotate the k8s ServiceAccount with this"
  value       = azurerm_user_assigned_identity.delivery_service.client_id
}
