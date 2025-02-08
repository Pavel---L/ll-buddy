package io.pl

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.pl.telegram.BotController
import io.pl.telegram.BotOrchestrator
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

private val logger = LoggerFactory.getLogger("MainApp")

// Global bot scope
private val botScope = CoroutineScope(Dispatchers.IO)
private val botOrchestrator: BotController = BotOrchestrator(botScope)

fun main() = runBlocking {

    botOrchestrator.startBot()

    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    logger.info("Starting Ktor server on port $port")
    val server = embeddedServer(Netty, port = port, module = Application::module)
    val serverJob = launch {
        server.start(wait = false)
    }

    logger.info("Ktor server started; waiting for jobs to complete...")
    serverJob.join()
}

fun Application.module() {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/api") }
    }

    logStartupInfo(environment)

    routing {
        route("/api") {
            get("/test") {
                val name = call.parameters["name"] ?: "Unknown"
                logger.info("Received API request: /test?name=$name")
                call.respondText("Hello, $name! This is your test endpoint with Ktor!")
            }

            //curl -X POST http://localhost:8080/api/bot/start
            post("/bot/start") {
                val started = botOrchestrator.startBot()
                if (started) {
                    logger.info("Bot started successfully.")
                    call.respondText("Bot started successfully.")
                } else {
                    logger.info("Bot is already running.")
                    call.respondText("Bot is already running.")
                }
            }

            //curl -X POST http://localhost:8080/api/bot/stop
            post("/bot/stop") {
                val stopped = botOrchestrator.stopBot()
                if (stopped) {
                    logger.info("Bot stopped successfully.")
                    call.respondText("Bot stopped successfully.")
                } else {
                    logger.info("Bot was not running.")
                    call.respondText("Bot was not running.")
                }
            }
        }
    }
}

fun Application.logStartupInfo(environment: ApplicationEnvironment) {
    val runtimeVersion = Runtime.version()
    val kotlinVersion = KotlinVersion.CURRENT
    val ktorVersion = environment.config.propertyOrNull("ktor.version")?.getString() ?: "Unknown"

    log.info("PL: my first railway app V6>>")
    log.info("Runtime: {}", runtimeVersion)
    log.info("Kotlin: {}", kotlinVersion)
    log.info("Ktor: {}", ktorVersion)
    log.info("Timestamp: {}", System.currentTimeMillis())
}
