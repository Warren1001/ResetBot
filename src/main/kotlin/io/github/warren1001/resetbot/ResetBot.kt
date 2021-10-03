package io.github.warren1001.resetbot

import discord4j.core.DiscordClient
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.MessageDeleteEvent
import discord4j.core.event.domain.message.MessageUpdateEvent
import kotlinx.coroutines.reactor.mono
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun main(args: Array<String>) {
	
	val token = args[0]
	val client = DiscordClient.create(token)
	
	client.withGateway { gateway ->
		val messageListener = MessageListener(gateway)
		mono {
			gateway.on(MessageCreateEvent::class.java).doOnError { e ->
				run {
					e.printStackTrace()
					gateway.logout().subscribe()
				}
			}.onErrorStop().subscribe(messageListener)
			gateway.on(MessageUpdateEvent::class.java).doOnError { e ->
				run {
					e.printStackTrace()
					gateway.logout().subscribe()
				}
			}.onErrorStop().subscribe(messageListener)
			gateway.on(MessageDeleteEvent::class.java).doOnError { e ->
				run {
					e.printStackTrace()
					gateway.logout().subscribe()
				}
			}.onErrorStop().subscribe(messageListener)
			gateway.onDisconnect().doOnSuccess { ResetBot.sys("Logging out and shutting down...") }.subscribe()
		}
	}.block()
	
}

object ResetBot {
	
	var DEBUG: Boolean = true
	
	private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss:SS")
	
	private fun log(prefix: String, msg: String) {
		println("[${LocalDateTime.now().format(timeFormat)}] ${prefix.uppercase()}: $msg")
	}
	
	fun debug(msg: String) {
		if (DEBUG) log("DEBUG", msg)
	}
	
	fun sys(msg: String) {
		log("SYS", msg)
	}
	
	fun info(msg: String) {
		log("INFO", msg)
	}
	
	fun error(msg: String) {
		log("ERROR", msg)
	}
	
}