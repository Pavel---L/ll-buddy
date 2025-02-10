package io.pl.telegram

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class OpenAIService(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val applicationConfig: ApplicationConfig
) {

    private val jsonEncoder = Json { prettyPrint = true }
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    suspend fun analyzeImageToText(imagePath: String, prompt: String): Result<String> {
        val systemPrompt = applicationConfig.getPrompt("analyzeImage").trimIndent()
        val request = createTextRequest(model = "gpt-4o", systemPrompt, prompt, imagePath)

        val jsonString = jsonEncoder.encodeToString(OpenAIRequest.serializer(), request)
        logger.info("analyzeImageToText request: $jsonString")


        val response: HttpResponse = httpClient.post("https://api.openai.com/v1/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(jsonString)
        }

        return extractContent(response)
    }

    suspend fun categorizeText(prompt: String): Result<String> {

        val systemPrompt = applicationConfig.getPrompt("categorizeText").trimIndent()
        val request = createTextRequest(model = "gpt-4o", systemPrompt, prompt)

        val jsonString = jsonEncoder.encodeToString(OpenAIRequest.serializer(), request)
        println(jsonString)

        val response: HttpResponse = httpClient.post("https://api.openai.com/v1/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(jsonString)
        }

        return extractContent(response)
    }

    suspend fun whatToDo(prompt: String): Result<String> {

        val systemPrompt = applicationConfig.getPrompt("whatToDo").trimIndent()
        val request = createTextRequest(model = "gpt-4o", systemPrompt, prompt)

        val jsonString = jsonEncoder.encodeToString(OpenAIRequest.serializer(), request)
        println(jsonString)

        val response: HttpResponse = httpClient.post("https://api.openai.com/v1/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(jsonString)
        }

        return extractContent(response)
    }


    suspend fun exercise(prompt: String): Result<String> {

        val systemPrompt = applicationConfig.getPrompt("exercise").trimIndent()
        val request = createTextRequest(model = "gpt-4o", systemPrompt, prompt)

        val jsonString = jsonEncoder.encodeToString(OpenAIRequest.serializer(), request)
        println(jsonString)

        val response: HttpResponse = httpClient.post("https://api.openai.com/v1/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(jsonString)
        }

        return extractContent(response)
    }

    suspend fun frank(prompt: String): Result<String> {

        val systemPrompt = applicationConfig.getPrompt("frank").trimIndent()
        val request = createTextRequest(model = "gpt-4o", systemPrompt, prompt)

        val jsonString = jsonEncoder.encodeToString(OpenAIRequest.serializer(), request)
        //println(jsonString)

        val response: HttpResponse = httpClient.post("https://api.openai.com/v1/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(jsonString)
        }

        return extractContent(response)
    }

    private fun createTextRequest(
        model: String,
        systemPrompt: String,
        prompt: String,
        imageBase64Url: String? = null
    ): OpenAIRequest {
        val systemMessage = Message(
            role = "system",
            content = JsonPrimitive(systemPrompt)
        )

        val userMessage = Message(
            role = "user",
            content = createContent(prompt, imageBase64Url)
        )

        val request = OpenAIRequest(
            model = model,
            messages = listOf(systemMessage, userMessage)
        )
        return request
    }

    private fun createContent(prompt: String?, imageBase64Url: String? = null) = buildJsonArray {
        add(buildJsonObject {
            put("type", "text")
            put("text", if (prompt != null) prompt else "")
        })
        if (imageBase64Url != null) {
            add(
                buildJsonObject {
                    put("type", "image_url")
                    putJsonObject("image_url") {
                        put("url", imageBase64Url)
                    }
                }
            )
        }
    }


    private suspend fun extractContent(response: HttpResponse): Result<String> {
        return runCatching {
            // 1. Check if HTTP response is successful
            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                throw IllegalStateException("API returned error: ${response.status} with body: $errorBody")
            }

            // 2. Read response body as text
            val responseBody = response.bodyAsText()

            // 3. Parse JSON response
            val root = Json.parseToJsonElement(responseBody).jsonObject

            // 4. Extract "choices" array
            val choices = root["choices"]?.jsonArray
                ?: throw IllegalStateException("No 'choices' field found")

            // 5. Get first element in "choices" array
            val firstChoice = choices.getOrNull(0)?.jsonObject
                ?: throw IllegalStateException("Choices array is empty")

            // 6. Extract "message" object
            val message = firstChoice["message"]?.jsonObject
                ?: throw IllegalStateException("No 'message' in first choice")

            // 7. Extract "content" field
            val content = message["content"]?.jsonPrimitive?.content
                ?: throw IllegalStateException("No 'content' in message")

            content // Return the extracted content
        }
    }

}

fun ApplicationConfig.getPrompt(key: String): String {
    return propertyOrNull(key)?.getString()
        ?: throw IllegalStateException("Missing prompt: $key in application.conf")
}

// Функция для кодирования изображения в base64
fun ByteArray.toBase64(): String {
    return java.util.Base64.getEncoder().encodeToString(this)
}

// Структура JSON для OpenAI API
@Serializable
data class OpenAIRequest(
    val model: String,
    val messages: List<Message>
)

@Serializable
data class Message(
    val role: String,
    /**
     * content может быть простой строкой, массивом, объектом и т.д.
     * Например:
     * "content": "Простая строка"
     * или
     * "content": [
     *    { "type": "text", "text": "..." },
     *    { "type": "image_url", "image_url": { "url": "..." } }
     * ]
     */
    val content: JsonElement
)
