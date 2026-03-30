package com.deli.gateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.ConstructorDetector
import tools.jackson.databind.json.JsonMapper

@Configuration
@EnableR2dbcRepositories(basePackages = ["com.deli.gateway.auth"])
class AppConfig {
    /**
     * Jackson 3.x ObjectMapper for Spring WebFlux codecs.
     *
     * Jackson 3.x: builder lives on JsonMapper (concrete type), not abstract ObjectMapper.
     * KotlinModule is not needed: ConstructorDetector.USE_PROPERTIES_BASED + javaParameters=true
     * (see root build.gradle.kts) lets Jackson resolve primary-constructor parameters by name.
     */
    @Bean
    fun objectMapper(): ObjectMapper =
        JsonMapper
            .builder()
            .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()
}
