package io.github.warren1001.resetbot

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.MessageCreateSpec
import discord4j.rest.util.Permission
import java.io.File
import java.time.Duration
import java.util.function.Consumer

class MessageListener(private val gateway: GatewayDiscordClient) : Consumer<MessageCreateEvent> {
	
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
		
		if (tradeChannelsFile.exists()) tradeChannelsFile.forEachLine { gateway.getChannelById(Snowflake.of(it)).filter { it is MessageChannel }.map { it as MessageChannel }.map { it.id }
			.subscribe { tradeListeners[it] = TradeChannelMessageListener(gateway, it) } }
		
	}
	
	override fun accept(e: MessageCreateEvent) {
		
		if (e.message.content.isNullOrEmpty()) return;
		
		e.message.channel.map { it.id }.filter { tradeListeners.containsKey(it) }.subscribe{ tradeListeners[it]?.accept(e) }
		
		if (e.message.content[0] != '!') return
		
		val contents = e.message.content.substring(1)
		var args = contents.split(' ', limit = 2)
		val command = args[0]
		val arguments = if (args.size == 1) null else args[1]
		
		val optionalMember = e.member
		if (optionalMember.isEmpty) return ResetBot.error("No member found in MessageListener?")
		val member = optionalMember.get()
		
		if (!e.message.authorAsMember.block()?.basePermissions?.block()?.contains(Permission.ADMINISTRATOR)!!) return
		
		when (command.lowercase()) {
			
			"stop" -> {
				gateway.logout().subscribe()
				reply(e, "Bye!")
			}
			
			"join" -> {
				if (arguments == null) return reply(e, "Usage: !join [team_name] [build name]")
				
				args = arguments.split(' ', limit = 2)
				
				if (args.size < 2) return reply(e, "Usage: !join [team_name] [build name]")
				
				val team = teams[args[0].lowercase()] ?: return reply(e, "There is no team named '${args[0]}' (case not sensitive).");
				
				if (team.isFull()) return reply(e, "Team '${team.teamName}' is full, choose another team.")
				
				team.addUser(member.id, args[1])
				reply(e, "You have joined Team ${team.teamName}!")
				createNewTeamIfNeeded(team.softcore)
				
			}
			
			"leave" -> {
				
				if (arguments == null) return reply(e, "Usage: !leave [team_name]")
				
				val team = teams[arguments.lowercase()] ?: return reply(e, "There is no team named '$arguments' (case not sensitive).");
				
				if (team.removeUser(member.id)) {
					reply(e, "You have been removed from Team '${team.teamName}'.")
				} else {
					reply(e, "You are not in Team '${team.teamName}'.")
				}
				
			}
			
			"list" -> reply(e, createPrettyTeamList())
			
			"settradechannel" -> {
				e.message.channel.map { it.id }.subscribe {
					tradeListeners[it] = TradeChannelMessageListener(gateway, it)
					tradeChannelsFile.appendText(it.asString())
				}
				reply(e, "This channel has been set as a trade channel.", true, 5)
			}
			
			"removetradechannel" -> {
				e.message.channel.map { it.id }.subscribe {
					tradeListeners.remove(it)
					val list = tradeChannelsFile.readLines().toMutableList()
					list.remove(it.asString())
					tradeChannelsFile.writeText(list.joinToString(separator = System.lineSeparator()))
				}
				reply(e, "This channel has been removed as a trade channel.", true, 5)
			}
			
			else -> ResetBot.info("'$contents' is not a valid command.")
			
		}
		
		
	}
	
	private fun reply(e: MessageCreateEvent, msg: String, delete: Boolean = false, duration: Long = -1L) {
		val specBuilder = MessageCreateSpec.builder()
		if (delete) {
			if (delete) e.message.delete().subscribe()
			specBuilder.content("${e.message.authorAsMember.block()?.mention}, $msg")
		} else {
			specBuilder.messageReference(e.message.id).content(msg)
		}
		e.message.channel.flatMap { it.createMessage(specBuilder.build()) }.subscribe{
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