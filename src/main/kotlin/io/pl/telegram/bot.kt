package io.pl.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.logging.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.concurrent.atomic.AtomicReference

private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
private val botRef = AtomicReference<Bot?>(null)

fun startTelegramBot(
    botToken: String,
    allowedUsers: Set<Long>,
    openAIService: OpenAIService,
    botScope: CoroutineScope
) {
    val photoHandlerService by lazy(LazyThreadSafetyMode.NONE) {
        PhotoHandlerService(botRef.get()!!, botToken, openAIService, botScope)
    }

    logger.info("Starting Telegram Bot...")

    val bot = bot {
        token = botToken
        timeout = 30
        logLevel = LogLevel.Network.Body

        dispatch {
            command("start") {
                if (!isUserAllowed(message.chat.id, allowedUsers)) return@command

                logger.info("Received /start command from chat id: ${message.chat.id}")
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id), text = "Hi there! Send me a photo and I'll process it."
                )
            }

            text {
                if (!isUserAllowed(message.chat.id, allowedUsers)) return@text

                logger.info("Received text message: '$text' from chat id: ${message.chat.id}")
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id), text = "You said: $text"
                )
            }

            photos {
                val chatId = message.chat.id
                if (!isUserAllowed(chatId, allowedUsers)) return@photos
                botScope.launch {
                    try {
                        photoHandlerService.handlePhotoMessage(message, chatId)
                    } catch (e: Exception) {
                        logger.error("Failed to process photo message", e)
                        bot.sendMessage(
                            chatId = ChatId.fromId(chatId),
                            text = "❌ Произошла ошибка при обработке фото."
                        )
                    }
                }
            }

            telegramError {
                logger.error("Failed to process photo message {} {}", error.getType(), error.getErrorMessage())
            }
        }
    }
    botRef.set(bot)

    logger.info("Starting polling for Telegram Bot...")
    bot.startPolling()
}

private fun isUserAllowed(userId: Long, allowedUsers: Set<Long>): Boolean {
    return if (allowedUsers.contains(userId)) {
        true
    } else {
        logger.warn("Unauthorized access attempt by user ID: $userId")
        botRef.get()?.sendMessage(
            chatId = ChatId.fromId(userId),
            text = "⛔ Доступ запрещён. Вы не находитесь в списке разрешённых пользователей."
        )
        false
    }
}
