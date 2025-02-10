package io.pl

import io.github.cdimascio.dotenv.Dotenv
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.pl.telegram.BotController
import io.pl.telegram.BotOrchestrator
import io.pl.telegram.OpenAIService
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.lang.invoke.MethodHandles
import java.util.concurrent.atomic.AtomicReference


private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

private val dotenv = Dotenv.configure().ignoreIfMissing().load()

// Global Ktor HttpClient instance
private val httpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.INFO
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 60000
        connectTimeoutMillis = 60000
        socketTimeoutMillis = 60000
    }
}


// Global bot scope
private val botScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
private val botControllerRef:AtomicReference<BotController?> = AtomicReference<BotController?>();

suspend fun stopBotIfRunning() {
    try {
        val response: HttpResponse = httpClient.post("http://ll-buddy-production.up.railway.app/api/bot/stop") {
            contentType(ContentType.Application.Json)
        }
        logger.info("Attempted to stop the bot. Response: ${response.status}")
    } catch (e: Exception) {
        logger.warn("Failed to stop the bot. It may not be running.", e)
    }
}

suspend fun startBotOnShutdown() {
    try {
        val response: HttpResponse = httpClient.post("http://ll-buddy-production.up.railway.app/api/bot/start") {
            contentType(ContentType.Application.Json)
        }
        logger.info("Attempted to restart the bot. Response: ${response.status}")
    } catch (e: Exception) {
        logger.warn("Failed to restart the bot on shutdown.", e)
    }
}

fun main() = runBlocking {
    // Ensure the bot stops before starting
    stopBotIfRunning()

    // Register shutdown hook to restart the bot when the process terminates
    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking {
            startBotOnShutdown()
        }
    })

    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    logger.info("Starting Ktor server on port $port")
    val server = embeddedServer(Netty, port = port, module = Application::module)
    val serverJob = launch {
        server.start(wait = false)
    }

    val openAIService = OpenAIService(
        httpClient,
        dotenv["OPEN_AI_API_TOKEN"] ?: throw IllegalStateException("OPENAI_API_KEY is not set"),
        ApplicationConfig("application.conf").config("ktor.prompts")
    )

    val botOrchestrator: BotController = BotOrchestrator(
        dotenv["TELEGRAM_BOT_LLBUDDY_TOKEN"] ?: throw IllegalStateException("TELEGRAM_BOT_TOKEN is not set"),
        dotenv["TELEGRAM_BOT_LLBUDDY_WHITELIST"]?.split(",")?.mapNotNull { it.trim().toLongOrNull() }?.toSet()
            ?: throw IllegalStateException("TELEGRAM_BOT_TOKEN_WHITELIST is not set"),
        openAIService,
        botScope
    )
    botControllerRef.set(botOrchestrator)
    botOrchestrator.startBot()

    logger.info("Ktor server started; waiting for jobs to complete...")
    serverJob.join()
    Thread.currentThread().join()

    // Clean up HttpClient when the application stops
    httpClient.close()
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

            // curl -X POST http://localhost:8080/api/bot/start
            post("/bot/start") {
                val started = botControllerRef.get()!!.startBot()
                if (started) {
                    logger.info("Bot started successfully.")
                    call.respondText("Bot started successfully.")
                } else {
                    logger.info("Bot is already running.")
                    call.respondText("Bot is already running.")
                }
            }

            // curl -X POST http://localhost:8080/api/bot/stop
            post("/bot/stop") {
                val stopped = botControllerRef.get()!!.stopBot()
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
    log.info("environment: {}", ApplicationConfig("application.conf").keys())
}
