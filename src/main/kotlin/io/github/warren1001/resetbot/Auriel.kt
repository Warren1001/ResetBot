package io.github.warren1001.resetbot

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.MessageDeleteEvent
import discord4j.core.event.domain.message.MessageUpdateEvent
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Auriel(private val gateway: GatewayDiscordClient) {
	
	private val messageListener: MessageListener = MessageListener(this)
	private val logger: Logger = Logger(this)
	private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss:SS")
	private val warrenMention: String
	
	init {
		gateway.on(MessageCreateEvent::class.java).doOnError { logger.logError(it) }.subscribe(messageListener)
		gateway.on(MessageUpdateEvent::class.java).doOnError { logger.logError(it) }.subscribe(messageListener)
		gateway.on(MessageDeleteEvent::class.java).doOnError { logger.logError(it) }.subscribe(messageListener)
		gateway.onDisconnect().doOnError { logger.logError(it) }.doOnSuccess { sys("Logging out and shutting down.") }.subscribe()
		warrenMention = gateway.getUserById(Snowflake.of(164118147073310721)).doOnError { logger.logError(it) }.block()?.mention!!
	}
	
	fun getGateway(): GatewayDiscordClient {
		return gateway
	}
	
	fun getMessageListener(): MessageListener {
		return messageListener
	}
	
	fun getLogger(): Logger {
		return logger
	}
	
	fun getCurrentTime(): String {
		return LocalDateTime.now().format(timeFormat)
	}
	
	fun getWarrenMention(): String {
		return warrenMention
	}
	
	fun info(msg: String) {
		log("INFO", msg)
	}
	
	fun debug(msg: String) {
		log("DEBUG", msg)
	}
	
	fun sys(msg: String) {
		log("SYS", msg)
	}
	
	fun error(msg: String) {
		log("ERROR", msg)
	}
	
	private fun log(prefix: String, msg: String) {
		println("[${getCurrentTime()}] ${prefix.uppercase()}: $msg")
	}
	
	fun stop() {
		messageListener.getBotFilter().setOfflineMessage()
		gateway.logout().subscribe()
		logger.stop()
	}
	
}