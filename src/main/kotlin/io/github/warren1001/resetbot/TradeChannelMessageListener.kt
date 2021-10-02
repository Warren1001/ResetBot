package io.github.warren1001.resetbot

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import kotlinx.coroutines.reactor.mono
import java.time.Instant
import java.util.function.Consumer

class TradeChannelMessageListener(private val gateway: GatewayDiscordClient, private val channelId: Snowflake) : Consumer<MessageCreateEvent> {
	
	private val usersLastMessage = mutableMapOf<Snowflake, Snowflake>()
	
	init {
		gateway.getChannelById(channelId).filter { it is MessageChannel }.map { it as MessageChannel }.flatMapMany { it.getMessagesBefore(Snowflake.of(Instant.now())) }
			.filter { !it.isPinned && (it.content.isEmpty() || it.content[0] != '!') }.subscribe { msg ->
				msg.authorAsMember.subscribe {
					if (usersLastMessage.containsKey(it.id)) msg.delete().subscribe()
					else usersLastMessage[it.id] = msg.id
				}
			}
	}
	
	override fun accept(e: MessageCreateEvent) {
		mono { e.message }.filter { !it.isPinned && (it.content.isEmpty() || it.content[0] != '!') }.flatMap { it.authorAsMember }.filter { usersLastMessage.containsKey(it.id) }.map {
			val oldId = usersLastMessage[it.id]!!
			usersLastMessage[it.id] = e.message.id
			return@map oldId
		}.flatMap { gateway.getMessageById(channelId, it) }.filter { !it.isPinned }.flatMap { it.delete() }.subscribe()
	}
	
}