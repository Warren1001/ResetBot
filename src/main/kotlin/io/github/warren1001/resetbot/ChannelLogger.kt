package io.github.warren1001.resetbot

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.MessageChannel
import reactor.core.publisher.Flux
import java.io.File

class ChannelLogger(private val auriel: Auriel) {
	
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
	
	private fun log(message: String) {
		Flux.merge(channelsToLogTo.map { auriel.getGateway().getChannelById(it) }).doOnError { auriel.getLogger().logError(it) }.map { it as MessageChannel }
			.flatMap {
				var content = message
				if (content.length > 2000) content = content.substring(0, 2000)
				return@flatMap it.createMessage(content)
			}.subscribe()
	}
	
	fun logDelete(message: ShallowMessage, reason: String) {
		log("__**Deleted Message**__\nPosted by ${message.author.mention} in ${message.getChannel().mention}\n" +
				"Reason: $reason\nMessage: ||${message.getMessage().content.replace("\n", " **\\n**")}||")
	}
	
	fun logError(message: String) {
		log("__**Bot Error**__\n${auriel.getWarrenMention()}, fix me, dammit: $message")
	}
	
}