package io.github.warren1001.resetbot

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.MessageEvent
import discord4j.core.event.domain.message.MessageUpdateEvent
import discord4j.core.spec.MessageCreateSpec
import discord4j.rest.util.Permission
import kotlinx.coroutines.reactor.mono
import java.time.Duration
import java.time.Instant
import java.util.function.Consumer

class TradeChannelMessageListener(private val gateway: GatewayDiscordClient, private val channelId: Snowflake) : Consumer<MessageEvent> {
	
	private val maximumLines = 45
	
	private val usersLastMessage = mutableMapOf<Snowflake, Snowflake>()
	private val predicate: (Message) -> Boolean = { !it.isPinned && (it.content.isNullOrEmpty() ||
			(!it.authorAsMember.block()?.basePermissions?.block()?.contains(Permission.ADMINISTRATOR)!! || it.content[0] != '!')) &&
			!it.authorAsMember.block()?.basePermissions?.block()?.contains(Permission.MANAGE_MESSAGES)!! }
	
	init {
		gateway.getChannelById(channelId).filter { it is MessageChannel }.map { it as MessageChannel }.flatMapMany { it.getMessagesBefore(Snowflake.of(Instant.now())) }
			.filter(predicate).subscribe { msg ->
				msg.authorAsMember.filter { !it.isBot }.subscribe {
					if (usersLastMessage.containsKey(it.id)) msg.delete().subscribe()
					else usersLastMessage[it.id] = msg.id
				}
			}
	}
	
	override fun accept(e: MessageEvent) {
		
		if (e is MessageUpdateEvent) {
			
			e.message.filter { it.content.split('\n').size > maximumLines }.subscribe {
				it.authorAsMember.map { it.id }.subscribe { usersLastMessage.remove(it) }
				reply(it, "You can have at most $maximumLines lines in your post. You will have to wait the cooldown to post another message with $maximumLines or less lines.", true, 10)
			}
		
		} else if (e is MessageCreateEvent) {
			
			val type = e.message.data.type()
			if (type == 18 || type == 6) {
				e.message.delete().subscribe()
				return
			}
			if (e.message.content.split('\n').size > maximumLines) {
				reply(e, "You can have at most $maximumLines lines in your post. You will have to wait the cooldown to post another message with $maximumLines or less lines.", true, 10)
				return
			}
			mono { e.message }.filter(predicate).flatMap { it.authorAsMember }.subscribe {
					if (usersLastMessage.containsKey(it.id)) {
						gateway.getMessageById(channelId, usersLastMessage[it.id]!!).doOnError { ResetBot.error("4") }.filter { !it.isPinned }.flatMap { it.delete() }.subscribe()
					}
					usersLastMessage[it.id] = e.message.id
				}
			
		}
		
		
		
	}
	
	private fun reply(e: MessageEvent, msg: String, delete: Boolean = false, duration: Long = -1L) {
		
		val specBuilder = MessageCreateSpec.builder()
		
		if (e is MessageUpdateEvent) {
			
			if (delete) {
				e.message.flatMap { it.delete() }.subscribe()
				specBuilder.content("${e.message.block()?.authorAsMember?.block()?.mention}, $msg")
			}
			
			e.message.flatMap { it.channel }.flatMap { it.createMessage(specBuilder.build()) }.subscribe {
				if (duration != -1L) it.delete().delaySubscription(Duration.ofSeconds(duration)).subscribe()
			}
			
		} else if (e is MessageCreateEvent) {
			
			if (delete) {
				
				e.message.delete().subscribe()
				specBuilder.content("${e.message.authorAsMember.block()?.mention}, $msg")
				
			} else specBuilder.messageReference(e.message.id).content(msg)
			
			e.message.channel.flatMap { it.createMessage(specBuilder.build()) }.subscribe {
				if (duration != -1L) it.delete().delaySubscription(Duration.ofSeconds(duration)).subscribe()
			}
		
		}
		
	}
	
	private fun reply(message: Message, msg: String, delete: Boolean = false, duration: Long = -1L) {
		
		val specBuilder = MessageCreateSpec.builder()
		
		if (delete) {
			
			message.delete().subscribe()
			specBuilder.content("${message.authorAsMember.block()?.mention}, $msg")
			
		} else specBuilder.messageReference(message.id).content(msg)
		
		message.channel.flatMap { it.createMessage(specBuilder.build()) }.subscribe{
			if (duration != -1L) it.delete().delaySubscription(Duration.ofSeconds(duration)).subscribe()
		}
		
	}
	
}