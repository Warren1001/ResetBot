package io.github.warren1001.resetbot

import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.MessageCreateSpec
import java.util.function.Consumer

class MessageListener(private val gateway: GatewayDiscordClient) : Consumer<MessageCreateEvent> {
	
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
		
	}
	
	override fun accept(e: MessageCreateEvent) {
		
		if (e.message.content[0] != '!') return
		
		val contents = e.message.content.substring(1)
		var args = contents.split(' ', limit = 2)
		val command = args[0]
		val arguments = if (args.size == 1) null else args[1]
		
		val optionalMember = e.member
		if (optionalMember.isEmpty) return ResetBot.error("No member found in MessageListener?")
		val member = optionalMember.get()
		
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
			
			else -> ResetBot.info("'$contents' is not a valid command.")
			
		}
		
		
	}
	
	fun reply(e: MessageCreateEvent, msg: String) {
		val spec = MessageCreateSpec.builder().messageReference(e.message.id).content(msg).build()
		e.message.channel.flatMap { it.createMessage(spec) }.subscribe()
	}
	
	fun createNewTeamIfNeeded(softcore: Boolean) {
		if (teams.filter { it.value.softcore == softcore }.all { it.value.isFull() }) {
			val teamName = ( if (softcore) "SC" else "HC" ) + "-" + ( if (softcore) lastTeamNumberSC++ else lastTeamNumberHC++ )
			teams[teamName.lowercase()] = Team(teamName, softcore)
		}
	}
	
	fun createPrettyTeamList() : String {
		return teams.map { it.value.teamName }.joinToString(separator = ", ")
	}
	
	
}