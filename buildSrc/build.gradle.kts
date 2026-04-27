plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.3.20")
    implementation("org.jetbrains.kotlin:kotlin-allopen:2.3.20")
    implementation("org.jetbrains.kotlin:kotlin-noarg:2.3.20")
    implementation("org.springframework.boot:spring-boot-gradle-plugin:4.0.6")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.7")
    //implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.8")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:14.0.1")
}
