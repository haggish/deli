resource "azurerm_redis_cache" "main" {
  name                = "${var.cluster_name}-redis"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name

  sku_name  = var.redis_sku_name
  family    = "C"
  capacity  = var.redis_capacity

  redis_version           = "6"
  non_ssl_port_enabled    = false
  minimum_tls_version     = "1.2"
  public_network_access_enabled = false

  redis_configuration {
    maxmemory_policy = "allkeys-lru"
  }

  # Standard tier provides 1 replica automatically
  replicas_per_master = 1

  tags = {
    Name = "${var.cluster_name}-redis"
  }
}

# Allow access from the VNet address space
resource "azurerm_redis_firewall_rule" "vnet" {
  name                = "vnet_access"
  redis_cache_name    = azurerm_redis_cache.main.name
  resource_group_name = azurerm_resource_group.main.name
  start_ip            = "10.0.0.0"
  end_ip              = "10.255.255.255"
}
