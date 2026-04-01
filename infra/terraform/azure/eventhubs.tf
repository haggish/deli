# Azure Event Hubs with Kafka protocol support (Standard tier and above)
# Each azurerm_eventhub maps to one Kafka topic.

resource "azurerm_eventhub_namespace" "main" {
  name                = "${var.cluster_name}-kafka"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  sku                 = "Standard"
  capacity            = var.eventhubs_capacity

  # Kafka endpoint is always enabled on Standard tier and above (no flag needed)
  auto_inflate_enabled     = true
  maximum_throughput_units = 8

  tags = {
    Name = "${var.cluster_name}-kafka"
  }
}

locals {
  kafka_topics = [
    "location.updated",
    "route.updated",
    "stop.assigned",
    "delivery.confirmed",
    "delivery.failed",
    "shift.started",
    "shift.completed",
  ]
}

resource "azurerm_eventhub" "topics" {
  for_each = toset(local.kafka_topics)

  name                = each.key
  namespace_name      = azurerm_eventhub_namespace.main.name
  resource_group_name = azurerm_resource_group.main.name
  partition_count     = 3
  message_retention   = 7
}

# Shared access policy for application use (send + listen)
resource "azurerm_eventhub_namespace_authorization_rule" "app" {
  name                = "deli-app"
  namespace_name      = azurerm_eventhub_namespace.main.name
  resource_group_name = azurerm_resource_group.main.name
  listen              = true
  send                = true
  manage              = false
}
