package io.pl.telegram

interface BotController {
    /** Starts the bot if it is not already running. Returns `true` if started, `false` otherwise. */
    fun startBot(): Boolean

    /** Stops the bot if it is running. Returns `true` if stopped, `false` otherwise. */
    fun stopBot(): Boolean
}
