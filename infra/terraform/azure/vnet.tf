resource "azurerm_virtual_network" "main" {
  name                = "${var.cluster_name}-vnet"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  address_space       = ["10.0.0.0/8"]

  tags = {
    Name = "${var.cluster_name}-vnet"
  }
}

# AKS node subnet
resource "azurerm_subnet" "aks" {
  name                 = "${var.cluster_name}-aks"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = ["10.0.1.0/24"]
}

# courierdb (PostgreSQL) — delegated subnet required by Flexible Server
resource "azurerm_subnet" "db_courier" {
  name                 = "${var.cluster_name}-db-courier"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = ["10.0.2.0/24"]

  delegation {
    name = "postgresql-delegation"
    service_delegation {
      name    = "Microsoft.DBforPostgreSQL/flexibleServers"
      actions = ["Microsoft.Network/virtualNetworks/subnets/join/action"]
    }
  }
}

# gpsdb (TimescaleDB) — separate delegated subnet
resource "azurerm_subnet" "db_gps" {
  name                 = "${var.cluster_name}-db-gps"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = ["10.0.3.0/24"]

  delegation {
    name = "postgresql-delegation"
    service_delegation {
      name    = "Microsoft.DBforPostgreSQL/flexibleServers"
      actions = ["Microsoft.Network/virtualNetworks/subnets/join/action"]
    }
  }
}

# Redis subnet
resource "azurerm_subnet" "redis" {
  name                 = "${var.cluster_name}-redis"
  resource_group_name  = azurerm_resource_group.main.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = ["10.0.4.0/24"]
}

# Private DNS zones for PostgreSQL Flexible Server (one per server)
resource "azurerm_private_dns_zone" "courierdb" {
  name                = "${var.cluster_name}-courierdb.private.postgres.database.azure.com"
  resource_group_name = azurerm_resource_group.main.name
}

resource "azurerm_private_dns_zone" "gpsdb" {
  name                = "${var.cluster_name}-gpsdb.private.postgres.database.azure.com"
  resource_group_name = azurerm_resource_group.main.name
}

resource "azurerm_private_dns_zone_virtual_network_link" "courierdb" {
  name                  = "${var.cluster_name}-courierdb-dns-link"
  private_dns_zone_name = azurerm_private_dns_zone.courierdb.name
  resource_group_name   = azurerm_resource_group.main.name
  virtual_network_id    = azurerm_virtual_network.main.id
}

resource "azurerm_private_dns_zone_virtual_network_link" "gpsdb" {
  name                  = "${var.cluster_name}-gpsdb-dns-link"
  private_dns_zone_name = azurerm_private_dns_zone.gpsdb.name
  resource_group_name   = azurerm_resource_group.main.name
  virtual_network_id    = azurerm_virtual_network.main.id
}
