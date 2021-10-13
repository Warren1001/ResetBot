package io.github.warren1001.resetbot

import discord4j.core.DiscordClient
import discord4j.core.event.domain.lifecycle.ReadyEvent
import kotlinx.coroutines.reactor.mono

fun main(args: Array<String>) {
	
	val token = args[0]
	val client = DiscordClient.create(token)
	
	client.withGateway { gateway ->
		mono {
			gateway.on(ReadyEvent::class.java).subscribe { Auriel(gateway) }
		}
	}.block()
	
}

