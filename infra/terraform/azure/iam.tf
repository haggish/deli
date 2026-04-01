# User-assigned managed identity for the delivery-service pod (Workload Identity)
resource "azurerm_user_assigned_identity" "delivery_service" {
  name                = "${var.cluster_name}-delivery-service-identity"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
}

# Grant the delivery-service identity access to blob storage
resource "azurerm_role_assignment" "delivery_service_storage" {
  scope                = azurerm_storage_account.main.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = azurerm_user_assigned_identity.delivery_service.principal_id
}

# Federated identity credential — allows the k8s ServiceAccount to assume this identity
# The subject must match: system:serviceaccount:<namespace>:<service-account-name>
resource "azurerm_federated_identity_credential" "delivery_service" {
  name                = "${var.cluster_name}-delivery-service-federated"
  resource_group_name = azurerm_resource_group.main.name
  parent_id           = azurerm_user_assigned_identity.delivery_service.id
  audience            = ["api://AzureADTokenExchange"]
  issuer              = azurerm_kubernetes_cluster.main.oidc_issuer_url
  subject             = "system:serviceaccount:deli:delivery-service"
}
