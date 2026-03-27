pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "deli"

include(
    "shared:domain-model",
    "shared:common-api",
)

include(
    "services:api-gateway",
    "services:route-service",
    "services:delivery-service",
    "services:location-service",
    "services:notification-service",
)
