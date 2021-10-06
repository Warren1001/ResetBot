package io.github.warren1001.resetbot

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.MessageDeleteEvent
import discord4j.core.event.domain.message.MessageEvent
import discord4j.core.event.domain.message.MessageUpdateEvent
import discord4j.core.spec.MessageCreateSpec
import discord4j.rest.util.Permission
import java.io.File
import java.time.Duration
import java.util.function.Consumer

class MessageListener(private val auriel: Auriel) : Consumer<MessageEvent> {
	
	private val swearFilter = SwearFilter(auriel)
	private val botFilter = BotFilter(auriel)
	
	private val tradeListeners = mutableMapOf<Snowflake, TradeChannelMessageListener>()
	private val tradeChannelsFile = File("tradeChannels.txt")
	
	/*private val teams = mutableMapOf<String, Team>()
	private var lastTeamNumberSC: Int = 1
	private var lastTeamNumberHC: Int = 1*/
	
	init {
		
		/* TODO store the teams somewhere
		if (teams.isEmpty()) {
			var teamName = "SC-" + lastTeamNumberSC++
			teams[teamName.lowercase()] = Team(teamName, true)
			teamName = "HC-" + lastTeamNumberHC++
			teams[teamName.lowercase()] = Team(teamName, false)
		}*/
		
		if (tradeChannelsFile.exists()) tradeChannelsFile.forEachLine {
			val args = it.split(',', limit = 2)
			auriel.getGateway().getChannelById(Snowflake.of(args[0])).doOnError { auriel.getLogger().logError(it) }.filter { it is MessageChannel }.map { it as MessageChannel }.map { it.id }
				.subscribe { tradeListeners[it] = TradeChannelMessageListener(auriel, it, args[1] == "true") }
		}
		else tradeChannelsFile.createNewFile()
		
	}
	
	override fun accept(e: MessageEvent) {
		
		if (e is MessageDeleteEvent && tradeListeners.containsKey(e.channelId)) {
			
			tradeListeners[e.channelId]?.removeMessage(e.messageId)
			
		} else if (e is MessageUpdateEvent) {
			
			val msg = ShallowMessage(auriel, e.message.block()!!)
			
			if (swearFilter.checkMessage(msg)) {
				replyDeleted(msg, "Your message contained a swear or censored word in it, so it was deleted. Remember that this is a family friendly community. :)")
			} else {
				val channelId = msg.getMessage().channelId
				if (!msg.getAuthor().isBot && tradeListeners.containsKey(channelId)) {
					tradeListeners[channelId]?.accept(msg)
				}
			}
			
		} else if (e is MessageCreateEvent) {
			
			val msg = ShallowMessage(auriel, e.message)
			
			if (msg.getAuthor().isBot || msg.getMessage().content.isNullOrEmpty()) return;
			
			if (botFilter.humanCheck(msg)) return
			
			if (swearFilter.checkMessage(msg)) {
				replyDeleted(msg, "Your message contained a swear or censored word in it, so it was deleted. Remember that this is a family friendly community. :)")
				return
			}
			
			if (tradeListeners.containsKey(msg.getMessage().channelId)) tradeListeners[msg.getMessage().channelId]?.accept(msg)
			
			if (msg.getMessage().content[0] != '!' || !msg.getAuthorPermissions().contains(Permission.ADMINISTRATOR)) return
			
			val contents = msg.getMessage().content.substring(1)
			val args = contents.split(' ', limit = 2)
			val command = args[0]
			val arguments = if (args.size == 1) null else args[1]
			val channelId = msg.getMessage().channelId
			
			when (command.lowercase()) {
				
				"stop" -> {
					reply(msg, "Bye!")
					auriel.stop()
				}
				
				/*"join" -> {
					
					if (arguments == null) return reply(msg, "Usage: !join [team_name] [build name]")
					
					args = arguments.split(' ', limit = 2)
					
					if (args.size < 2) return reply(msg, "Usage: !join [team_name] [build name]")
					
					val team = teams[args[0].lowercase()] ?: return reply(msg, "There is no team named '${args[0]}' (case not sensitive).");
					
					if (team.isFull()) return reply(msg, "Team '${team.teamName}' is full, choose another team.")
					
					team.addUser(msg.getAuthor().id, args[1])
					reply(msg, "You have joined Team ${team.teamName}!")
					createNewTeamIfNeeded(team.softcore)
					
				}
				
				"leave" -> {
					
					if (arguments == null) return reply(msg, "Usage: !leave [team_name]")
					
					val team = teams[arguments.lowercase()] ?: return reply(msg, "There is no team named '$arguments' (case not sensitive).");
					
					if (team.removeUser(msg.getAuthor().id)) {
						reply(msg, "You have been removed from Team '${team.teamName}'.")
					} else {
						reply(msg, "You are not in Team '${team.teamName}'.")
					}
					
				}
				
				"list" -> reply(msg, createPrettyTeamList())*/
				
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
					
					if (arguments.isNullOrEmpty()) return reply(msg, "Usage: !addswearfilter [filter]")
					
					if (swearFilter.addSwearFilterPattern(arguments)) {
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
					
					if (arguments.isNullOrEmpty()) return reply(msg, "Usage: !addswearword [word]")
					
					val pattern = swearFilter.constructBasicPattern(arguments)
					
					if (swearFilter.addSwearFilterPattern(pattern)) {
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
				
				"ping" -> {
					reply(msg, "Pong.", true, 5)
				}
				
				else -> auriel.info("'$contents' is not a valid command.")
				
			}
			
		}
		
	}
	
	fun replyDeleted(message: ShallowMessage, msg: String, duration: Long = -1L) {
		
		val specBuilder = MessageCreateSpec.builder().content("${message.getAuthor().mention}, $msg")
		
		message.getChannel().createMessage(specBuilder.build()).subscribe {
			if (duration != -1L) it.delete().delaySubscription(Duration.ofSeconds(duration)).subscribe()
		}
		
	}
	
	fun reply(message: ShallowMessage, msg: String, delete: Boolean = false, duration: Long = -1L) {
		
		val specBuilder = MessageCreateSpec.builder()
		
		if (delete) {
			
			specBuilder.content("${message.getAuthor().mention}, $msg")
			message.delete()
			
		} else specBuilder.messageReference(message.getMessage().id).content(msg)
		
		message.getChannel().createMessage(specBuilder.build()).subscribe {
			if (duration != -1L) it.delete().delaySubscription(Duration.ofSeconds(duration)).subscribe()
		}
		
	}
	
	/*private fun createNewTeamIfNeeded(softcore: Boolean) {
		if (teams.filter { it.value.softcore == softcore }.all { it.value.isFull() }) {
			val teamName = (if (softcore) "SC" else "HC") + "-" + (if (softcore) lastTeamNumberSC++ else lastTeamNumberHC++)
			teams[teamName.lowercase()] = Team(teamName, softcore)
		}
	}
	
	private fun createPrettyTeamList(): String {
		return teams.map { it.value.teamName }.joinToString(separator = ", ")
	}*/
	
	fun delete(message: ShallowMessage, reason: String) {
		message.delete { auriel.getLogger().logDelete(it, reason) }
	}
	
}