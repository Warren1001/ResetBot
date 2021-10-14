package io.github.warren1001.resetbot

import com.google.gson.JsonObject
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.MessageDeleteEvent
import discord4j.core.event.domain.message.MessageUpdateEvent
import io.github.warren1001.resetbot.listener.MessageListener
import io.github.warren1001.resetbot.listener.UserManager
import io.github.warren1001.resetbot.logging.Logger
import io.github.warren1001.resetbot.utils.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Auriel(private val gateway: GatewayDiscordClient) {
	
	private val userManager = UserManager(this)
	private val jsonObject = FileUtils.readJsonLines("data.json")
	private val messageListener: MessageListener = MessageListener(this)
	private val logger: Logger = Logger(this)
	private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss:SS")
	private lateinit var warrenMention: String
	
	init {
		gateway.on(MessageCreateEvent::class.java).onErrorContinue { error, _ -> logger.logError(error) }.subscribe(messageListener)
		gateway.on(MessageUpdateEvent::class.java).onErrorContinue { error, _ -> logger.logError(error) }.subscribe(messageListener)
		gateway.on(MessageDeleteEvent::class.java).onErrorContinue { error, _ -> logger.logError(error) }.subscribe(messageListener)
		gateway.onDisconnect().doOnError { logger.logError(it) }.doOnSuccess { sys("Logging out and shutting down.") }.subscribe()
		gateway.getUserById(Snowflake.of(164118147073310721L)).doOnError { logger.logError(it) }.subscribe {
			warrenMention = it.mention
			// TODO
		}
		//warrenMention = gateway.getUserById(Snowflake.of(164118147073310721L)).doOnError { logger.logError(it) }.block()?.mention!!
	}
	
	fun getGateway(): GatewayDiscordClient {
		return gateway
	}
	
	fun getUserManager(): UserManager {
		return userManager
	}
	
	fun getJson(): JsonObject {
		return jsonObject
	}
	
	fun saveJson() {
		CoroutineScope(Dispatchers.IO).async { FileUtils.saveJsonLines("data.json", jsonObject) }
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
		saveJson()
		gateway.logout().subscribe()
		logger.stop()
	}
	
}