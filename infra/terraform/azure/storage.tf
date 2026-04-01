resource "azurerm_storage_account" "main" {
  name                     = "${replace(var.cluster_name, "-", "")}${var.environment}store"
  location                 = azurerm_resource_group.main.location
  resource_group_name      = azurerm_resource_group.main.name
  account_tier             = "Standard"
  account_replication_type = "LRS"

  https_traffic_only_enabled      = true
  min_tls_version                 = "TLS1_2"
  allow_nested_items_to_be_public = false
  public_network_access_enabled   = false

  blob_properties {
    versioning_enabled = true
  }

  tags = {
    Name = "${var.cluster_name}-storage"
  }
}

resource "azurerm_storage_container" "delivery_proof_photos" {
  name                  = "delivery-proof-photos"
  storage_account_name  = azurerm_storage_account.main.name
  container_access_type = "private"
}

resource "azurerm_storage_container" "delivery_signatures" {
  name                  = "delivery-signatures"
  storage_account_name  = azurerm_storage_account.main.name
  container_access_type = "private"
}
