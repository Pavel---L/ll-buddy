package io.pl.telegram

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

class BotOrchestrator(
    private val botToken: String,
    private val allowedUsers: Set<Long>,
    private val openAIService: OpenAIService,
    private val botScope: CoroutineScope
) : BotController {
    private val botJob = AtomicReference<Job?>(null)

    /**
     * Starts the Telegram bot if it is not already running.
     * @return `true` if the bot was started, `false` if it was already running.
     */
    override fun startBot(): Boolean {
        if (botJob.get()?.isActive == true) {
            return false // Bot is already running
        }

        val newJob = botScope.launch {
            startTelegramBot(botToken, allowedUsers, openAIService, botScope)
        }

        // Atomically set botJob only if it was null
        return botJob.compareAndSet(null, newJob).also { started ->
            if (!started) newJob.cancel() // Cancel duplicate job
        }
    }

    /**
     * Stops the Telegram bot if it is running.
     * @return `true` if the bot was stopped, `false` if it was not running.
     */
    override fun stopBot(): Boolean {
        val currentJob = botJob.get() ?: return false // Bot is not running

        return botJob.compareAndSet(currentJob, null).also { stopped ->
            //if (stopped) currentJob.cancel() // Only cancel if we actually stopped it
        }
    }
}
