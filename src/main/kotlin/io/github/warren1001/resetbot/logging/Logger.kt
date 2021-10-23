package io.github.warren1001.resetbot.logging

import io.github.warren1001.resetbot.Auriel
import io.github.warren1001.resetbot.listener.ShallowMessage

class Logger(private val auriel: Auriel) {
	
	private val errorLogger = ErrorLogger(auriel)
	private val channelLogger = ChannelLogger(auriel)
	
	fun getChannelLogger(): ChannelLogger {
		return channelLogger
	}
	
	fun logError(error: Throwable) {
		errorLogger.logError(error)
		error.message?.let { msg ->
			auriel.error(msg)
			auriel.getWarren().privateChannel.flatMap { it.createMessage(msg) }.subscribe()
			// channelLogger.logError(msg)
		}
	}
	
	fun logDelete(message: ShallowMessage, reason: String) {
		channelLogger.logDelete(message, reason)
	}
	
	fun stop() {
		errorLogger.close()
	}
	
}