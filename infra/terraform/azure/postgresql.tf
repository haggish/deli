# ─── courierdb — standard PostgreSQL 16 ─────────────────────────────────────

resource "azurerm_postgresql_flexible_server" "courierdb" {
  name                   = "${var.cluster_name}-courierdb"
  location               = azurerm_resource_group.main.location
  resource_group_name    = azurerm_resource_group.main.name
  version                = "16"
  administrator_login    = var.db_admin_login
  administrator_password = var.db_admin_password

  storage_mb = var.db_storage_mb_courier
  sku_name   = var.db_sku_name

  # Private access via VNet integration
  delegated_subnet_id = azurerm_subnet.db_courier.id
  private_dns_zone_id = azurerm_private_dns_zone.courierdb.id

  backup_retention_days        = 7
  geo_redundant_backup_enabled = false
  high_availability {
    mode = "ZoneRedundant"
  }

  depends_on = [azurerm_private_dns_zone_virtual_network_link.courierdb]

  tags = {
    Name = "${var.cluster_name}-courierdb"
  }
}

resource "azurerm_postgresql_flexible_server_database" "courierdb" {
  name      = "courierdb"
  server_id = azurerm_postgresql_flexible_server.courierdb.id
  collation = "en_US.utf8"
  charset   = "utf8"
}

resource "azurerm_postgresql_flexible_server_configuration" "courierdb_pg_stat_statements" {
  name      = "shared_preload_libraries"
  server_id = azurerm_postgresql_flexible_server.courierdb.id
  value     = "pg_stat_statements"
}

# ─── gpsdb — PostgreSQL 16 with TimescaleDB extension ───────────────────────
# TimescaleDB is available as an extension in Azure Database for PostgreSQL Flexible Server.
# After terraform apply, run in gpsdb: CREATE EXTENSION IF NOT EXISTS timescaledb;

resource "azurerm_postgresql_flexible_server" "gpsdb" {
  name                   = "${var.cluster_name}-gpsdb"
  location               = azurerm_resource_group.main.location
  resource_group_name    = azurerm_resource_group.main.name
  version                = "16"
  administrator_login    = var.db_admin_login
  administrator_password = var.db_admin_password

  storage_mb = var.db_storage_mb_gps
  sku_name   = var.db_sku_name

  delegated_subnet_id = azurerm_subnet.db_gps.id
  private_dns_zone_id = azurerm_private_dns_zone.gpsdb.id

  backup_retention_days        = 7
  geo_redundant_backup_enabled = false

  depends_on = [azurerm_private_dns_zone_virtual_network_link.gpsdb]

  tags = {
    Name = "${var.cluster_name}-gpsdb"
  }
}

resource "azurerm_postgresql_flexible_server_database" "gpsdb" {
  name      = "gpsdb"
  server_id = azurerm_postgresql_flexible_server.gpsdb.id
  collation = "en_US.utf8"
  charset   = "utf8"
}

# Load timescaledb alongside pg_stat_statements
resource "azurerm_postgresql_flexible_server_configuration" "gpsdb_timescaledb" {
  name      = "shared_preload_libraries"
  server_id = azurerm_postgresql_flexible_server.gpsdb.id
  value     = "timescaledb,pg_stat_statements"
}
