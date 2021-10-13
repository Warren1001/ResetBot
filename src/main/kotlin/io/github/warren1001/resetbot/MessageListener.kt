package io.github.warren1001.resetbot

import discord4j.core.`object`.entity.channel.PrivateChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.MessageDeleteEvent
import discord4j.core.event.domain.message.MessageEvent
import discord4j.core.event.domain.message.MessageUpdateEvent
import discord4j.core.spec.MessageCreateSpec
import discord4j.rest.util.Permission
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
		
		
		commandManager.registerCommand("ping", UserManager.MODERATOR) { reply(it.msg, "Pong.", true, 5) }
		commandManager.registerCommand("imamod", UserManager.MODERATOR) { ctx ->
			ctx.msg.message.authorAsMember.flatMap { member -> member.basePermissions.map { Tuples.of(member, it) } }.filter { it.t2.contains(Permission.BAN_MEMBERS) }.subscribe {
				auriel.getUserManager().addModerator(it.t1.id)
				reply(ctx.msg, "fine.", true, 10L)
			}
		}
		commandManager.registerCommand("imadmin") { ctx ->
			ctx.msg.message.authorAsMember.flatMap { member -> member.basePermissions.map { Tuples.of(member, it) } }.filter { it.t2.contains(Permission.ADMINISTRATOR) }.subscribe {
				auriel.getUserManager().addAdministrator(it.t1.id)
				reply(ctx.msg, "fine.", true, 10L)
			}
		}
		commandManager.registerCommand("stop") { ctx ->
			reply(ctx.msg, "Bye!")
			auriel.stop()
		}
		commandManager.registerCommand("tc", action = tradeChannelListener)
		
	}
	
	override fun accept(e: MessageEvent) {
		
		when (e) {
			is MessageDeleteEvent -> {
				
				tradeChannelListener.remove(e.channelId, e.messageId)
				
			}
			is MessageUpdateEvent -> {
				
				Flux.combineLatest(e.channel, e.message, Tuples::of).filter { it.t1 !is PrivateChannel && it.t2.author.isPresent }.map { ShallowMessage(auriel, it.t2, it.t1) }.subscribe {
					
					if (swearFilter.checkMessage(it)) replyDeleted(it, "Your message contained a swear or censored word in it, so it was deleted. Remember that this is a family friendly community. :)")
					else if (!it.author.isBot) tradeChannelListener.handle(it)
					
				}
				
			}
			is MessageCreateEvent -> {
				
				e.message.channel.filter { it !is PrivateChannel }.map { Tuples.of(it, e.message) }.filter { it.t2.author.isPresent && !it.t2.author.get().isBot && !it.t2.content.isNullOrEmpty() }
					.map { ShallowMessage(auriel, it.t2, it.t1) }.filter { !commandManager.handle(it) && !botFilter.humanCheck(it.message) }.subscribe {
						
						if (tradeChannelListener.isTradeChannel(it.message.channelId)) tradeChannelListener.handle(it)
						else swearFilter.checkMessage(it)
						
					}
				
				/*
					
					"settradechannel" -> {
						tradeListeners[channelId] = TradeChannelMessageListener(auriel, channelId, false)
						if (tradeChannelsFile.readLines().isEmpty()) tradeChannelsFile.writeText("${channelId.asString()},false")
						else tradeChannelsFile.appendText(System.lineSeparator() + "${channelId.asString()},false")
						reply(msg, "This channel has been set as a trade channel.", true, 5)
					}
					
					"removetradechannel" -> {
						tradeListeners.remove(channelId)
						val list = tradeChannelsFile.readLines().toMutableList()
						list.removeIf { it.startsWith(channelId.asString()) }
						tradeChannelsFile.writeText(list.joinToString(separator = System.lineSeparator()))
						reply(msg, "This channel has been removed as a trade channel.", true, 5)
					}
					
					"setbuychannel" -> {
						
						if (tradeListeners.containsKey(channelId)) {
							
							if (tradeListeners[channelId]?.setIsBuy(true) == true) {
								
								val list = tradeChannelsFile.readLines().toMutableList()
								list.remove("${channelId.asString()},false")
								list.add("${channelId.asString()},true")
								tradeChannelsFile.writeText(list.joinToString(separator = System.lineSeparator()))
								reply(msg, "This channel has been set as a buy channel.", true, 5)
								
							} else reply(msg, "This channel is already a buy channel!", true, 5)
							
						} else {
							
							tradeListeners[channelId] = TradeChannelMessageListener(auriel, channelId, true)
							if (tradeChannelsFile.readLines().isEmpty()) tradeChannelsFile.writeText("${channelId.asString()},true")
							else tradeChannelsFile.appendText(System.lineSeparator() + "${channelId.asString()},true")
							reply(msg, "This channel has been set as a buy channel.", true, 5)
							
						}
						
					}
					
					"removebuychannel" -> {
						
						if (tradeListeners.containsKey(channelId) && tradeListeners[channelId]?.setIsBuy(false) == true) {
							
							val list = tradeChannelsFile.readLines().toMutableList()
							list.remove("${channelId.asString()},true")
							list.add("${channelId.asString()},false")
							tradeChannelsFile.writeText(list.joinToString(separator = System.lineSeparator()))
							reply(msg, "This channel has been removed as a buy channel.", true, 5)
							
						} else reply(msg, "This channel is already not a buy channel!", true, 5)
						
					}
					
					"addbuychannelblacklistword" -> {
						
						if (arguments.isNullOrEmpty()) return reply(msg, "Usage: !addbuychannelblacklistword [word]")
						
						if (TradeChannelMessageListener.addBlacklistWord(arguments)) {
							reply(msg, "Added '$arguments' as a buy channel blacklist word.")
						} else {
							reply(msg, "'$arguments' is already a buy channel blacklist word.")
						}
						
					}
					
					"removebuychannelblacklistword" -> {
						
						if (arguments.isNullOrEmpty()) return reply(msg, "Usage: !removebuychannelblacklistword [word]")
						
						if (TradeChannelMessageListener.removeBlacklistWord(arguments)) {
							reply(msg, "Removed '$arguments' as a buy channel blacklist word.")
						} else {
							reply(msg, "'$arguments' is not a buy channel blacklist word.")
						}
						
					}
					
					"setlogchannel" -> {
						
						if (auriel.getLogger().getChannelLogger().addLogChannel(channelId)) {
							reply(msg, "This channel has been added as a logging channel.", true, 5)
						} else {
							reply(msg, "This channel is already a logging channel.", true, 5)
						}
						
					}
					
					"removelogchannel" -> {
						
						if (auriel.getLogger().getChannelLogger().removeLogChannel(channelId)) {
							reply(msg, "This channel has been removed as a logging channel.", true, 5)
						} else {
							reply(msg, "This channel is not a logging channel.", true, 5)
						}
						
					}
					
					"addswearfilter" -> {
						
						if (arguments.isNullOrEmpty()) return reply(msg, "Usage: !addswearfilter [filter] [replacement]")
						
						val split = arguments.split(' ')
						
						if (split.size != 2) return reply(msg, "Usage: !addswearfilter [filter] [replacement]")
						
						val pattern = split[0]
						val replacement = split[1]
						
						if (swearFilter.addSwearFilterPattern(pattern, replacement)) {
							reply(msg, "Added '$arguments' pattern to the swear filters list.")
						} else {
							reply(msg, "'$arguments' pattern is already on the swear filters list.")
						}
						
					}
					
					"removeswearfilter" -> {
						
						if (arguments.isNullOrEmpty()) return reply(msg, "Usage: !removeswearfilter [filter]")
						
						
						if (swearFilter.removeSwearFilterPattern(arguments)) {
							reply(msg, "Removed '$arguments' pattern from the swear filters list.")
						} else {
							reply(msg, "'$arguments' pattern is not on the swear filters list.")
						}
						
					}
					
					"addswearword" -> {
						
						if (arguments.isNullOrEmpty()) return reply(msg, "Usage: !addswearword [word] [replacement]")
						
						val split = arguments.split(' ')
						
						if (split.size != 2) return reply(msg, "Usage: !addswearword [word] [replacement]")
						
						val pattern = swearFilter.constructBasicPattern(split[0])
						val replacement = split[1]
						
						if (swearFilter.addSwearFilterPattern(pattern, replacement)) {
							reply(msg, "Added '$pattern' pattern to the swear filters list.")
						} else {
							reply(msg, "'$pattern' pattern is already on the swear filters list.")
						}
						
					}
					
					"removeswearword" -> {
						
						if (arguments.isNullOrEmpty()) return reply(msg, "Usage: !removeswearword [word]")
						
						val pattern = swearFilter.constructBasicPattern(arguments)
						
						if (swearFilter.removeSwearFilterPattern(pattern)) {
							reply(msg, "Removed '$pattern' pattern from the swear filters list.")
						} else {
							reply(msg, "'$pattern' pattern is not on the swear filters list.")
						}
						
					}
					
					"swearfilterlist" -> {
						reply(msg, "Here are the swear filters currently in place:\n${swearFilter.getListOfPatterns()}")
					}
					
					"sethumanroleid" -> {
						
						if (arguments.isNullOrEmpty()) return reply(msg, "Usage: !sethumanroleid [id]")
						
						botFilter.setHumanRoleId(Snowflake.of(arguments))
						reply(msg, "Set $arguments ID as the human role.", true, 10)
						
					}
					
					"sethumanchannelid" -> {
						
						if (arguments.isNullOrEmpty()) return reply(msg, "Usage: !sethumanchannelid [id]")
						
						botFilter.setHumanChannelId(Snowflake.of(arguments))
						reply(msg, "Set $arguments ID as the human channel.", true, 10)
						
					}
					
					else -> auriel.info("'$contents' is not a valid command.")
					
				}*/
				
			}
		}
		
	}
	
	fun getBotFilter(): BotFilter {
		return botFilter
	}
	
	fun getSwearFilter(): SwearFilter {
		return swearFilter
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