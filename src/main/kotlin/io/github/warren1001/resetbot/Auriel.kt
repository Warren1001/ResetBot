package io.github.warren1001.resetbot

import com.google.gson.JsonObject
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.User
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.MessageDeleteEvent
import discord4j.core.event.domain.message.MessageUpdateEvent
import io.github.warren1001.resetbot.listener.MessageListener
import io.github.warren1001.resetbot.listener.UserManager
import io.github.warren1001.resetbot.logging.Logger
import io.github.warren1001.resetbot.utils.FileUtils
import io.github.warren1001.resetbot.youtube.YoutubeManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Auriel(private val gateway: GatewayDiscordClient, youtubeKey: String) {
	
	private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss:SS")
	private val jsonObject = FileUtils.readJsonLines("data.json")
	private val userManager = UserManager()
	private val logger: Logger = Logger(this)
	private lateinit var warren: User
	private val messageListener: MessageListener = MessageListener(this)
	private val youtubeManager: YoutubeManager = YoutubeManager(this, youtubeKey)
	
	init {
		gateway.on(MessageCreateEvent::class.java).onErrorContinue { error, _ -> logger.logError(error) }.subscribe(messageListener)
		gateway.on(MessageUpdateEvent::class.java).onErrorContinue { error, _ -> logger.logError(error) }.subscribe(messageListener)
		gateway.on(MessageDeleteEvent::class.java).onErrorContinue { error, _ -> logger.logError(error) }.subscribe(messageListener)
		gateway.onDisconnect().doOnError { logger.logError(it) }.doOnSuccess { sys("Logging out and shutting down.") }.subscribe()
		gateway.getUserById(Snowflake.of(164118147073310721L)).doOnError { logger.logError(it) }.subscribe {
			warren = it
			messageListener.getBotFilter().setRandomCaptcha()
		}
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
		FileUtils.saveJsonLines("data.json", jsonObject)
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
	
	fun getWarren(): User {
		return warren
	}
	
	fun hasWarrenInit(): Boolean {
		return this::warren.isInitialized
	}
	
	fun getYoutubeManager(): YoutubeManager {
		return youtubeManager
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
		messageListener.getTriviaManager().savePlayerData()
		gateway.logout().subscribe()
		logger.stop()
	}
	
}