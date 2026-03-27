package com.deli.location

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LocationServiceApplication

fun main(args: Array<String>) {
    runApplication<LocationServiceApplication>(*args)
}
