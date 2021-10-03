package io.github.warren1001.resetbot

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Message
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

class MessageListener(private val gateway: GatewayDiscordClient) : Consumer<MessageEvent> {
	
	private val tradeListeners = mutableMapOf<Snowflake, TradeChannelMessageListener>()
	private val tradeChannelsFile = File("tradeChannels.txt")
	private val teams = mutableMapOf<String, Team>()
	private var lastTeamNumberSC: Int = 1
	private var lastTeamNumberHC: Int = 1
	
	init {
		
		// TODO store the teams somewhere
		
		if (teams.isEmpty()) {
			var teamName = "SC-" + lastTeamNumberSC++
			teams[teamName.lowercase()] = Team(teamName, true)
			teamName = "HC-" + lastTeamNumberHC++
			teams[teamName.lowercase()] = Team(teamName, false)
		}
		
		if (tradeChannelsFile.exists()) tradeChannelsFile.forEachLine {
			val args = it.split(',', limit = 2)
			gateway.getChannelById(Snowflake.of(args[0])).filter { it is MessageChannel }.map { it as MessageChannel }.map { it.id }
				.subscribe { tradeListeners[it] = TradeChannelMessageListener(gateway, it, args[1] == "true") }
		}
		else tradeChannelsFile.createNewFile()
		
	}
	
	override fun accept(e: MessageEvent) {
		
		//ResetBot.debug("content: ${e.message.content}")
		//ResetBot.debug("data: ${e.message.data}")
		
		// todo anything not related to commands
		
		if (e is MessageDeleteEvent) {
			
			if (tradeListeners.containsKey(e.channelId)) {
				tradeListeners[e.channelId]?.removeMessage(e.messageId)
			}
			
		} else if (e is MessageUpdateEvent) {
			
			e.message.subscribe { msg ->
				msg.authorAsMember.filter { !it.isBot }.map { msg.channelId }.filter { tradeListeners.containsKey(it) }.subscribe{ tradeListeners[it]?.accept(e) }
			}
			
		} else if (e is MessageCreateEvent) {
			
			val optionalMember = e.member
			if (optionalMember.isEmpty) return ResetBot.error("No member found in MessageListener?")
			val member = optionalMember.get()
			
			if (member.isBot) return;
			
			if (tradeListeners.containsKey(e.message.channelId)) tradeListeners[e.message.channelId]?.accept(e)
			
			if (e.message.content.isNullOrEmpty() || e.message.content[0] != '!' || !member.basePermissions.block()?.contains(Permission.ADMINISTRATOR)!!) return
			
			val contents = e.message.content.substring(1)
			var args = contents.split(' ', limit = 2)
			val command = args[0]
			val arguments = if (args.size == 1) null else args[1]
			val channelId = e.message.channelId
			
			when (command.lowercase()) {
				
				"stop" -> {
					gateway.logout().subscribe()
					reply(e.message, "Bye!")
				}
				
				"join" -> {
					
					if (arguments == null) return reply(e.message, "Usage: !join [team_name] [build name]")
					
					args = arguments.split(' ', limit = 2)
					
					if (args.size < 2) return reply(e.message, "Usage: !join [team_name] [build name]")
					
					val team = teams[args[0].lowercase()] ?: return reply(e.message, "There is no team named '${args[0]}' (case not sensitive).");
					
					if (team.isFull()) return reply(e.message, "Team '${team.teamName}' is full, choose another team.")
					
					team.addUser(member.id, args[1])
					reply(e.message, "You have joined Team ${team.teamName}!")
					createNewTeamIfNeeded(team.softcore)
					
				}
				
				"leave" -> {
					
					if (arguments == null) return reply(e.message, "Usage: !leave [team_name]")
					
					val team = teams[arguments.lowercase()] ?: return reply(e.message, "There is no team named '$arguments' (case not sensitive).");
					
					if (team.removeUser(member.id)) {
						reply(e.message, "You have been removed from Team '${team.teamName}'.")
					} else {
						reply(e.message, "You are not in Team '${team.teamName}'.")
					}
					
				}
				
				"list" -> reply(e.message, createPrettyTeamList())
				
				"settradechannel" -> {
					tradeListeners[channelId] = TradeChannelMessageListener(gateway, channelId, false)
					if (tradeChannelsFile.readLines().isEmpty()) tradeChannelsFile.writeText("${channelId.asString()},false")
					else tradeChannelsFile.appendText(System.lineSeparator() + "${channelId.asString()},false")
					reply(e.message, "This channel has been set as a trade channel.", true, 5)
				}
				
				"removetradechannel" -> {
					tradeListeners.remove(channelId)
					val list = tradeChannelsFile.readLines().toMutableList()
					list.removeIf { it.startsWith(channelId.asString()) }
					tradeChannelsFile.writeText(list.joinToString(separator = System.lineSeparator()))
					reply(e.message, "This channel has been removed as a trade channel.", true, 5)
				}
				
				"setbuychannel" -> {
					
					if (tradeListeners.containsKey(channelId)) {
						
						if (tradeListeners[channelId]?.setIsBuy(true) == true) {
							
							val list = tradeChannelsFile.readLines().toMutableList()
							list.remove("${channelId.asString()},false")
							list.add("${channelId.asString()},true")
							tradeChannelsFile.writeText(list.joinToString(separator = System.lineSeparator()))
							reply(e.message, "This channel has been set as a buy channel.", true, 5)
							
						} else reply(e.message, "This channel is already a buy channel!", true, 5)
						
					} else {
						
						tradeListeners[channelId] = TradeChannelMessageListener(gateway, channelId, true)
						if (tradeChannelsFile.readLines().isEmpty()) tradeChannelsFile.writeText("${channelId.asString()},true")
						else tradeChannelsFile.appendText(System.lineSeparator() + "${channelId.asString()},true")
						reply(e.message, "This channel has been set as a buy channel.", true, 5)
						
					}
					
				}
				
				"removebuychannel" -> {
					
					if (tradeListeners.containsKey(channelId) && tradeListeners[channelId]?.setIsBuy(false) == true) {
						
						val list = tradeChannelsFile.readLines().toMutableList()
						list.remove("${channelId.asString()},true")
						list.add("${channelId.asString()},false")
						tradeChannelsFile.writeText(list.joinToString(separator = System.lineSeparator()))
						reply(e.message, "This channel has been removed as a buy channel.", true, 5)
						
					} else reply(e.message, "This channel is already not a buy channel!", true, 5)
					
				}
				
				"ping" -> {
					reply(e.message, "Pong.", true, 5)
				}
				
				else -> ResetBot.info("'$contents' is not a valid command.")
				
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
	
	private fun createNewTeamIfNeeded(softcore: Boolean) {
		if (teams.filter { it.value.softcore == softcore }.all { it.value.isFull() }) {
			val teamName = ( if (softcore) "SC" else "HC" ) + "-" + ( if (softcore) lastTeamNumberSC++ else lastTeamNumberHC++ )
			teams[teamName.lowercase()] = Team(teamName, softcore)
		}
	}
	
	private fun createPrettyTeamList() : String {
		return teams.map { it.value.teamName }.joinToString(separator = ", ")
	}
	
	
}