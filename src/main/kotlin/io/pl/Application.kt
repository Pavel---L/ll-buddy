package io.pl

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    printStartupInfo(environment)

    routing {
        get("/api/test") {
            val name = call.parameters["name"] ?: "Unknown"
            println("Received parameter: name=$name")
            call.respondText("Hello, $name! This is your test endpoint with Ktor!")
        }
    }
}

fun printStartupInfo(environment: ApplicationEnvironment) {
    val runtimeVersion = Runtime.version()
    val kotlinVersion = KotlinVersion.CURRENT
    val ktorVersion = environment.config.propertyOrNull("ktor.version")?.getString() ?: "Unknown"

    println("PL: my first railway app V3>>")
    println("Runtime: $runtimeVersion")
    println("Kotlin: $kotlinVersion")
    println("Ktor: $ktorVersion")
    println("Timestamp: ${System.currentTimeMillis()}")
}