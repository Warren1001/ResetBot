package io.github.warren1001.resetbot.listener

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.PrivateChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.MessageDeleteEvent
import discord4j.core.event.domain.message.MessageEvent
import discord4j.core.event.domain.message.MessageUpdateEvent
import discord4j.core.spec.MessageCreateSpec
import discord4j.rest.util.Permission
import io.github.warren1001.resetbot.Auriel
import io.github.warren1001.resetbot.command.CommandManager
import io.github.warren1001.resetbot.command.impl.FilterCommand
import io.github.warren1001.resetbot.command.impl.TradeChannelCommand
import io.github.warren1001.resetbot.filter.BotFilter
import io.github.warren1001.resetbot.filter.SwearFilter
import reactor.core.publisher.Flux
import reactor.util.function.Tuples
import java.time.Duration
import java.util.function.Consumer

class MessageListener(private val auriel: Auriel) : Consumer<MessageEvent> {
	
	private val swearFilter = SwearFilter(auriel)
	private val botFilter = BotFilter(auriel)
	private val commandManager = CommandManager(auriel)
	private val tradeChannelListener = TradeChannelCommand(auriel)
	
	init {
		
		
		commandManager.registerSimpleCommand("ping", UserManager.MODERATOR) { reply(it.msg, "Pong.", true, 5) }
		commandManager.registerSimpleCommand("imamod", UserManager.MODERATOR) { ctx ->
			ctx.msg.message.authorAsMember.flatMap { member -> member.basePermissions.map { Tuples.of(member, it) } }.filter { it.t2.contains(Permission.BAN_MEMBERS) }.subscribe {
				auriel.getUserManager().addModerator(it.t1.id)
				reply(ctx.msg, "fine.", true, 10L)
			}
		}
		commandManager.registerSimpleCommand("imadmin") { ctx ->
			ctx.msg.message.authorAsMember.flatMap { member -> member.basePermissions.map { Tuples.of(member, it) } }.filter { it.t2.contains(Permission.ADMINISTRATOR) }.subscribe {
				auriel.getUserManager().addAdministrator(it.t1.id)
				reply(ctx.msg, "fine.", true, 10L)
			}
		}
		commandManager.registerSimpleCommand("stop") { ctx ->
			reply(ctx.msg, "Bye!")
			auriel.stop()
		}
		commandManager.registerCommand("tc", action = tradeChannelListener)
		commandManager.registerSimpleCommand("swearfilterlist") { reply(it.msg, "Here are the swear filters currently in place:\n${swearFilter.getListOfPatterns()}") }
		commandManager.registerCommand("log") {
			
			val arguments = it.arguments
			if (arguments.isEmpty()) return@registerCommand false
			
			val channelId = it.msg.message.channelId
			if (arguments.equals("set", true)) {
				
				if (auriel.getLogger().getChannelLogger().addLogChannel(channelId)) {
					reply(it.msg, "This channel has been added as a logging channel.", true, 5)
				} else {
					reply(it.msg, "This channel is already a logging channel.", true, 5)
				}
				
				return@registerCommand true
			} else if (arguments.equals("remove", true)) {
				
				if (auriel.getLogger().getChannelLogger().removeLogChannel(channelId)) {
					reply(it.msg, "This channel has been removed as a logging channel.", true, 5)
				} else {
					reply(it.msg, "This channel is not a logging channel.", true, 5)
				}
				
				return@registerCommand true
			}
			
			return@registerCommand false
		}
		commandManager.registerCommand("filter", action = FilterCommand(auriel))
		commandManager.registerCommand("bot") {
			
			val arguments = it.arguments
			if (arguments.isEmpty() || !arguments.contains(' ')) return@registerCommand false
			
			val args = arguments.split(' ')
			if (args.size == 2) {
				
				if (args[0].equals("role", true)) {
					
					val id = args[1]
					botFilter.setHumanRoleId(Snowflake.of(id))
					reply(it.msg, "Set '$id' ID as the human role.", true, 10)
					
					return@registerCommand true
					
				} else if (args[0].equals("channel", true)) {
					
					val id = args[1]
					botFilter.setHumanChannelId(Snowflake.of(id))
					reply(it.msg, "Set '$id' ID as the human channel.", true, 10)
					
					return@registerCommand true
				}
				
			}
			
			return@registerCommand false
		}
		
	}
	
	override fun accept(e: MessageEvent) {
		
		when (e) {
			is MessageDeleteEvent -> {
				
				tradeChannelListener.remove(e.channelId, e.messageId)
				
			}
			is MessageUpdateEvent -> {
				
				Flux.combineLatest(e.channel, e.message, Tuples::of).filter { it.t1 !is PrivateChannel && it.t2.author.isPresent }.map { ShallowMessage(auriel, it.t2, it.t1) }.subscribe {
					
					if (swearFilter.checkMessage(it))
						replyDeleted(it, "Your message contained a swear or censored word in it, so it was deleted. Remember that this is a family friendly community. :)")
					else if (!it.author.isBot) tradeChannelListener.handle(it)
					
				}
				
			}
			is MessageCreateEvent -> {
				
				e.message.channel.filter { it !is PrivateChannel }.map { Tuples.of(it, e.message) }.filter { it.t2.author.isPresent && !it.t2.author.get().isBot && !it.t2.content.isNullOrEmpty() }
					.map { ShallowMessage(auriel, it.t2, it.t1) }.filter { !commandManager.handle(it) && !botFilter.humanCheck(it.message) }.subscribe {
						
						if (tradeChannelListener.isTradeChannel(it.message.channelId)) tradeChannelListener.handle(it)
						else swearFilter.checkMessage(it)
						
					}
				
			}
		}
		
	}
	
	fun getBotFilter(): BotFilter {
		return botFilter
	}
	
	fun getSwearFilter(): SwearFilter {
		return swearFilter
	}
	
	fun getCommandManager(): CommandManager {
		return commandManager
	}
	
	fun replyDeleted(message: ShallowMessage, msg: String, duration: Long = -1L) {
		
		var content = "${message.author.mention}, $msg"
		if (content.length > 2000) content = content.substring(0, 2000)
		
		val specBuilder = MessageCreateSpec.builder().content(content)
		
		message.channel.createMessage(specBuilder.build()).subscribe {
			if (duration != -1L) it.delete().delaySubscription(Duration.ofSeconds(duration)).subscribe()
		}
		
	}
	
	fun reply(message: ShallowMessage, msg: String, delete: Boolean = false, duration: Long = -1L) {
		
		val specBuilder = MessageCreateSpec.builder()
		var content = msg
		
		if (delete) {
			
			content = "${message.author.mention}, $msg"
			message.delete()
			
		} else specBuilder.messageReference(message.message.id)
		
		if (content.length > 2000) content = content.substring(0, 2000)
		
		specBuilder.content(content)
		message.channel.createMessage(specBuilder.build()).subscribe {
			if (duration != -1L) it.delete().delaySubscription(Duration.ofSeconds(duration)).subscribe()
		}
		
	}
	
	fun delete(message: ShallowMessage, reason: String) {
		message.delete { auriel.getLogger().logDelete(it, reason) }
	}
	
}