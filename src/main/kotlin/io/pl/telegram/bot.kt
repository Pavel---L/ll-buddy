package io.pl.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.photos
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.logging.LogLevel
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles
import java.util.concurrent.atomic.AtomicReference

private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
private val botRef = AtomicReference<Bot?>(null)

fun startTelegramBot(botToken: String) {
    val photoHandlerService by lazy(LazyThreadSafetyMode.NONE) {
        PhotoHandlerService(botRef.get()!!, botToken)
    }

    logger.info("Starting Telegram Bot...")

    val bot = bot {
        token = botToken
        timeout = 30
        logLevel = LogLevel.Network.Body

        dispatch {
            command("start") {
                logger.info("Received /start command from chat id: ${message.chat.id}")
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id), text = "Hi there! Send me a photo and I'll process it."
                )
            }

            text {
                logger.info("Received text message: '$text' from chat id: ${message.chat.id}")
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id), text = "You said: $text"
                )
            }

            photos {
                val chatId = message.chat.id
                try {
                    photoHandlerService.handlePhotoMessage(message, chatId)
                } catch (e: Exception) {
                    logger.error("Failed to process photo message", e)
                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = "Произошла ошибка при обработке фото."
                    )
                }
            }
        }
    }
    botRef.set(bot)

    logger.info("Starting polling for Telegram Bot...")
    bot.startPolling()
}