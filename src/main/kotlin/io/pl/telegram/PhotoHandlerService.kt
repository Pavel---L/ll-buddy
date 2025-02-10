package io.pl.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.types.TelegramBotResult
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.invoke.MethodHandles
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption


class PhotoHandlerService(
    private val bot: Bot,
    private val botToken: String,
    private val openAIService: OpenAIService,
    private val botScope: CoroutineScope
) {

    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())


    suspend fun handlePhotoMessage(message: Message, chatId: Long) {
        // Замеряем время начала обработки
        val startTime = System.currentTimeMillis()

        val photoSizes = message.photo ?: return
        if (photoSizes.isEmpty()) return

        // Находим самое большое фото
        val largestPhoto = photoSizes.maxByOrNull { it.width * it.height }
            ?: throw IllegalStateException("No valid photo sizes")

        val fileId = largestPhoto.fileId
        val fileSize = largestPhoto.fileSize ?: 0
        val width = largestPhoto.width
        val height = largestPhoto.height

        logger.info("Received photo from chat id: $chatId | Width: $width, Height: $height, Size: $fileSize bytes")

        // Запрашиваем filePath у телеграма
        val (response, exception) = bot.getFile(fileId)
        if (exception != null) {
            logger.error("Error retrieving file path from Telegram API: ${exception.message}", exception)
            bot.sendMessage(ChatId.fromId(chatId), "Failed to process the photo due to an error.")
            return
        }
        if (response?.isSuccessful != true) {
            logger.error("Failed to retrieve file. Response: ${response?.errorBody()?.string()}")
            bot.sendMessage(ChatId.fromId(chatId), "Failed to process the photo.")
            return
        }

        val filePath = response.body()?.result?.filePath
            ?: run {
                logger.error("Telegram API did not return a valid file path.")
                bot.sendMessage(ChatId.fromId(chatId), "Could not retrieve photo file path.")
                return
            }

        val downloadUrl = "https://api.telegram.org/file/bot$botToken/$filePath"

        // Рассчитываем время обработки (в миллисекундах)
        val endTime = System.currentTimeMillis()
        val processingTime = endTime - startTime

        // Получаем username (может быть null)
        val username = message.from?.username ?: "Unknown"

        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = """
                    📸 Your photo details:
                    **Resolution:** ${width}x${height} pixels
                    **File Size:** $fileSize bytes
                    ⏱ **Processing Time:** ${processingTime}ms
                """.trimIndent(),
            parseMode = ParseMode.MARKDOWN_V2
        )

        try {
            val textFromImage = openAIService.analyzeImageToText(downloadUrl, "О чем текст на изображении").getOrThrow()
            val category = openAIService.categorizeText(textFromImage).getOrThrow()
            val categoryLower = category.lowercase()
            val regex = Regex("final\\s*answer\\s*:\\s*(exercise|reading|other)")
            val match = regex.find(categoryLower)
            val extractedCategory = match?.groupValues?.get(1)
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = "Категория: $extractedCategory",
                parseMode = ParseMode.HTML,
            )

            if (extractedCategory.equals("exercise")) {
                val exerciseDescription = openAIService.whatToDo(textFromImage).getOrThrow()
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = "Задание: $exerciseDescription",
                    parseMode = ParseMode.HTML
                )

                val exerciseText = openAIService.exercise(textFromImage).getOrThrow()
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = "Упражнение: $exerciseText",
                    parseMode = ParseMode.HTML
                )
                val adoptedText = openAIService.frank(exerciseText).getOrThrow()
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = "📸 Адаптированный текст: $adoptedText",
                    parseMode = ParseMode.HTML
                )
            } else {
                val sendMessageResult = bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = "📸 Анализ фото: $textFromImage",
                    parseMode = if (extractedCategory.equals("reading")) ParseMode.HTML else null
                )
                sendMessageResult.fold(
                    {
                        // do something here with the response
                    },
                    { error ->
                        bot.sendMessage(
                            chatId = ChatId.fromId(chatId),
                            text = "📸 Ошибка: ${error}",
                            parseMode = ParseMode.HTML
                        )
                    },
                )

                val adoptedText = openAIService.frank(textFromImage).getOrThrow()
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = "📸 Адаптированный текст: $adoptedText",
                    parseMode = ParseMode.HTML
                )
            }

        } catch (e: Exception) {
            logger.error("openAIService Failed to process photo message", e)
            throw e;
        }
    }

    private fun savePhoto(fileUrl: String, fileId: String) = try {
        val tempFile = File.createTempFile("photo_$fileId", ".jpg")
        tempFile.deleteOnExit()
        URL(fileUrl).openStream().use { input ->
            Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        logger.info("Photo saved temporarily at: ${tempFile.absolutePath}")
        tempFile.absolutePath
    } catch (e: Exception) {
        logger.error("Failed to download photo: ${e.message}", e)
        "Error saving file"
    }
}