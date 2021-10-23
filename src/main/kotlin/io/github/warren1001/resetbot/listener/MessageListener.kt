package io.github.warren1001.resetbot.listener

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.PrivateChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.MessageDeleteEvent
import discord4j.core.event.domain.message.MessageEvent
import discord4j.core.event.domain.message.MessageUpdateEvent
import discord4j.rest.util.Permission
import io.github.warren1001.resetbot.Auriel
import io.github.warren1001.resetbot.command.CommandManager
import io.github.warren1001.resetbot.command.impl.FilterCommand
import io.github.warren1001.resetbot.command.impl.TradeChannelCommand
import io.github.warren1001.resetbot.filter.BotFilter
import io.github.warren1001.resetbot.filter.SwearFilter
import io.github.warren1001.resetbot.trivia.TriviaCommand
import io.github.warren1001.resetbot.trivia.TriviaManager
import io.github.warren1001.resetbot.youtube.YoutubeCommand
import reactor.core.publisher.Flux
import reactor.util.function.Tuples
import java.util.function.Consumer

class MessageListener(private val auriel: Auriel) : Consumer<MessageEvent> {
	
	private val swearFilter = SwearFilter(auriel)
	private val botFilter = BotFilter(auriel)
	private val commandManager = CommandManager(auriel, this)
	private val tradeChannelListener = TradeChannelCommand(auriel)
	private val triviaManager = TriviaManager(auriel)
	
	init {
		
		commandManager.registerSimpleCommand("ping", UserManager.MODERATOR) { it.msg.reply("Pong.", true, 5) }
		commandManager.registerSimpleCommand("imamod", UserManager.EVERYONE) { ctx ->
			ctx.msg.message.authorAsMember.flatMap { member -> member.basePermissions.map { Tuples.of(member, it) } }.filter { it.t2.contains(Permission.BAN_MEMBERS) }.subscribe {
				auriel.getUserManager().addModerator(it.t1.id)
				ctx.msg.reply("fine.", true, 10L)
			}
		}
		commandManager.registerSimpleCommand("imadmin", UserManager.EVERYONE) { ctx ->
			ctx.msg.message.authorAsMember.flatMap { member -> member.basePermissions.map { Tuples.of(member, it) } }.filter { it.t2.contains(Permission.ADMINISTRATOR) }.subscribe {
				auriel.getUserManager().addAdministrator(it.t1.id)
				ctx.msg.reply("fine.", true, 10L)
			}
		}
		commandManager.registerSimpleCommand("stop") { ctx ->
			ctx.msg.reply("Bye!")
			auriel.stop()
		}
		commandManager.registerCommand("tc", action = tradeChannelListener)
		commandManager.registerSimpleCommand("swearfilterlist", UserManager.MODERATOR) { it.msg.reply("Here are the swear filters currently in place:\n${swearFilter.getListOfPatterns()}") }
		commandManager.registerCommand("log") {
			
			val arguments = it.arguments
			if (arguments.isEmpty()) return@registerCommand false
			
			val channelId = it.msg.message.channelId
			if (arguments.equals("set", true)) {
				
				if (auriel.getLogger().getChannelLogger().addLogChannel(channelId)) {
					it.msg.reply("This channel has been added as a logging channel.", true, 5)
				} else {
					it.msg.reply("This channel is already a logging channel.", true, 5)
				}
				
				return@registerCommand true
			} else if (arguments.equals("remove", true)) {
				
				if (auriel.getLogger().getChannelLogger().removeLogChannel(channelId)) {
					it.msg.reply("This channel has been removed as a logging channel.", true, 5)
				} else {
					it.msg.reply("This channel is not a logging channel.", true, 5)
				}
				
				return@registerCommand true
			}
			
			return@registerCommand false
		}
		commandManager.registerCommand("filter", action = FilterCommand(auriel))
		commandManager.registerCommand("bot") {
			
			val arguments = it.arguments
			if (arguments.isEmpty() || !arguments.contains(' ')) return@registerCommand false
			
			val args = arguments.split(' ', limit = 2)
			if (args.size == 2) {
				
				if (args[0].equals("role", true)) {
					
					val id = args[1]
					botFilter.setHumanRoleId(Snowflake.of(id))
					it.msg.reply("Set '$id' ID as the human role.", true, 10)
					
					return@registerCommand true
					
				} else if (args[0].equals("channel", true)) {
					
					val id = args[1]
					botFilter.setHumanChannelId(Snowflake.of(id))
					it.msg.reply("Set '$id' ID as the human channel.", true, 10)
					
					return@registerCommand true
					
				} else if (args[0].equals("success", true)) {
					
					val msg = args[1]
					botFilter.setSuccessMessage(msg)
					it.msg.reply("Set '$msg' as the captcha verification msg.", true, 10)
					
					return@registerCommand true
					
				} else if (args[0].equals("alreadyin", true)) {
					
					val msg = args[1]
					botFilter.setAlreadyInMessage(msg)
					it.msg.reply("Set '$msg' as the \"you're already in the damn server\" msg.", true, 10)
					
					return@registerCommand true
					
				}
				
			}
			
			return@registerCommand false
		}
		commandManager.registerCommand("trivia", action = TriviaCommand(auriel))
		commandManager.registerCommand("youtube", action = YoutubeCommand(auriel))
		
	}
	
	override fun accept(e: MessageEvent) {
		
		when (e) {
			is MessageDeleteEvent -> {
				
				tradeChannelListener.remove(e.channelId, e.messageId)
				if (botFilter.isBotMessage(e.messageId)) botFilter.setupBotMessage()
				
			}
			is MessageUpdateEvent -> {
				
				Flux.combineLatest(e.channel, e.message, Tuples::of).filter { it.t1 !is PrivateChannel && it.t2.author.isPresent }.map { ShallowMessage(auriel, it.t2, it.t1) }.subscribe {
					
					if (swearFilter.checkMessage(it))
						it.replyDeleted("Your message contained a swear or censored word in it, so it was deleted. Remember that this is a family friendly community. :)")
					else if (!it.author.isBot) tradeChannelListener.handle(it)
					
				}
				
			}
			is MessageCreateEvent -> {
				
				e.message.channel.filter { it !is PrivateChannel }.map { Tuples.of(it, e.message) }.filter { it.t2.author.isPresent && !it.t2.author.get().isBot && !it.t2.content.isNullOrEmpty() }
					.map { ShallowMessage(auriel, it.t2, it.t1) }.subscribe {
						
						if (commandManager.handle(it)) return@subscribe
						
						if (tradeChannelListener.isTradeChannel(it.message.channelId)) tradeChannelListener.handle(it)
						else {
							if (swearFilter.checkMessage(it)) return@subscribe
							if (triviaManager.handle(it)) return@subscribe
						}
						
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
	
	fun getTriviaManager(): TriviaManager {
		return triviaManager
	}
	
}