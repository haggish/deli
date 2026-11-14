plugins {
    id("deli.kotlin-library")
}

description = "Shared OpenAPI DTOs and HTTP request/response models"

dependencies {
    api(project(":shared:domain-model"))
    api(libs.jakarta.validation.api)
    // No dependency-management plugin here, so pull Spring versions from the Boot BOM
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.spring.web)
    implementation(libs.spring.context)
    implementation(libs.jakarta.servlet.api)
    implementation(libs.slf4j.api)
}
