package io.pl.telegram

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.photos
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.logging.LogLevel
import org.slf4j.LoggerFactory

// Создаём логгер для модуля бота
private val logger = LoggerFactory.getLogger("TelegramBot")

fun startTelegramBot() {
    // Получите токен из переменной окружения или конфигурационного файла
    // val botToken = System.getenv("TELEGRAM_BOT_TOKEN") ?: error("TELEGRAM_BOT_TOKEN не задан")
    val botToken = "8016863707:AAGG0_f1oIv3pLFClXpgxiTrjeP4kMyGT20"

    logger.info("Starting Telegram Bot...")

    val bot = bot {
        token = botToken
        timeout = 30
        logLevel = LogLevel.Network.Body
        dispatch {
            command("start") {
                logger.info("Received /start command from chat id: ${message.chat.id}")
                val result = bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "Hi there!"
                )
                result.fold(
                    {
                        logger.info("Message sent successfully to chat id: ${message.chat.id}")
                    },
                    { error ->
                        logger.error("Error sending message to chat id: ${message.chat.id}", error)
                    }
                )
            }

            text {
                logger.info("Received text message: '$text' from chat id: ${message.chat.id}")
                val sendResult = bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    messageThreadId = message.messageThreadId,
                    text = text,
                    protectContent = true,
                    disableNotification = false,
                )
                sendResult.fold(
                    {
                        logger.info("Echoed message to chat id: ${message.chat.id}")
                    },
                    { error ->
                        logger.error("Error echoing message to chat id: ${message.chat.id}", error)
                    }
                )
            }

            // Handle photo messages
            photos {
                val chatId = message.chat.id
                val photoSizes = message.photo ?: return@photos
                if (photoSizes.isEmpty()) return@photos

                // Select the highest resolution photo
                val largestPhoto = photoSizes.maxByOrNull { it.width * it.height }
                if (largestPhoto == null) {
                    bot.sendMessage(chatId = ChatId.fromId(chatId), text = "Couldn't retrieve photo details.")
                    return@photos
                }

                val width = largestPhoto.width
                val height = largestPhoto.height
                val fileSize = largestPhoto.fileSize ?: 0

                logger.info("Received photo from chat id: $chatId | Width: $width, Height: $height, Size: $fileSize bytes")

                // Respond with photo details
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = "📸 Your photo details:\n- **Resolution:** ${width}x${height} pixels\n- **File Size:** $fileSize bytes"
                )
            }
        }
    }

    logger.info("Starting polling for Telegram Bot...")
    bot.startPolling()
}
