package io.github.warren1001.resetbot

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.MessageChannel
import reactor.core.publisher.Flux
import java.io.File

class ChannelLogger(private val gateway: GatewayDiscordClient) {
	
	private val logChannelsFile = File("logChannels.txt")
	private val channelsToLogTo = mutableSetOf<Snowflake>()
	
	init {
		if (logChannelsFile.exists()) {
			logChannelsFile.forEachLine { channelsToLogTo.add(Snowflake.of(it)) }
		} else {
			logChannelsFile.createNewFile()
		}
	}
	
	fun addLogChannel(id: Snowflake): Boolean {
		if (channelsToLogTo.contains(id)) return false
		if (channelsToLogTo.isEmpty()) {
			logChannelsFile.writeText(id.asString())
		} else {
			logChannelsFile.appendText(System.lineSeparator() + id.asString())
		}
		channelsToLogTo.add(id)
		return true
	}
	
	fun removeLogChannel(id: Snowflake): Boolean {
		if (channelsToLogTo.isEmpty() || !channelsToLogTo.contains(id)) return false
		channelsToLogTo.remove(id)
		val list = logChannelsFile.readLines().toMutableList()
		list.remove(id.asString())
		logChannelsFile.writeText(list.joinToString(separator = System.lineSeparator()))
		return true
	}
	
	fun log(message: String) {
		Flux.merge(channelsToLogTo.map { gateway.getChannelById(it) }).map { it as MessageChannel }.flatMap { it.createMessage(message) }.subscribe()
	}
	
	fun logDelete(message: ShallowMessage, reason: String) {
		log("__**Deleted Message**__\nPosted by ${message.getAuthor().mention} in ${message.getChannel().mention}\n" +
				"Reason: $reason\nMessage: ||${message.getMessage().content.replace("\n", " **\\n**")}||")
	}
	
}