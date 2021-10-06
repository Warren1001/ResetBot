package io.github.warren1001.resetbot

class Logger(private val auriel: Auriel) {
	
	private val errorLogger = ErrorLogger(auriel)
	private val channelLogger = ChannelLogger(auriel)
	
	fun getChannelLogger(): ChannelLogger {
		return channelLogger
	}
	
	fun logError(error: Throwable) {
		errorLogger.logError(error)
		error.message?.let {
			auriel.error(it)
			channelLogger.logError(it)
		}
	}
	
	fun logDelete(message: ShallowMessage, reason: String) {
		channelLogger.logDelete(message, reason)
	}
	
	fun stop() {
		errorLogger.close()
	}
	
}