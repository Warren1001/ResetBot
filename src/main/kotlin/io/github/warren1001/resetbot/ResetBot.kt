package io.github.warren1001.resetbot

import discord4j.core.DiscordClient
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.entity.channel.PrivateChannel
import discord4j.core.spec.MessageCreateMono
import discord4j.core.spec.MessageCreateSpec
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono

fun main(args: Array<String>) {
	
	val token = args[0]
	val youtubeKey = args[1]
	val client = DiscordClient.create(token)
	
	client.withGateway { gateway ->
		mono {
			Auriel(gateway, youtubeKey)
		}
	}.block()
	
}

fun String.limit(): String = if (this.length > 2000) this.substring(0, 2000) else this

fun PrivateChannel.createMessageWithLimit(content: String): MessageCreateMono = this.createMessage(content.limit())

fun PrivateChannel.createMessageWithLimit(spec: MessageCreateSpec): Mono<Message> = this.createMessage(spec.withContent(spec.contentOrElse("").limit()))

fun MessageChannel.createMessageWithLimit(content: String): MessageCreateMono = this.createMessage(content.limit())

fun MessageChannel.createMessageWithLimit(spec: MessageCreateSpec): Mono<Message> = this.createMessage(spec.withContent(spec.contentOrElse("").limit()))

