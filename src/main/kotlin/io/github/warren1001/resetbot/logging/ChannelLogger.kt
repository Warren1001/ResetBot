package io.github.warren1001.resetbot.logging

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.MessageChannel
import io.github.warren1001.resetbot.Auriel
import io.github.warren1001.resetbot.listener.ShallowMessage
import io.github.warren1001.resetbot.utils.FileUtils
import reactor.core.publisher.Flux

class ChannelLogger(private val auriel: Auriel) {
	
	private val channelsToLogTo = if (auriel.getJson().has("log.id.channels")) { FileUtils.gson.fromJson<MutableSet<Snowflake>>(auriel.getJson()["log.id.channels"].toString()) }
		else { mutableSetOf() }
	
	fun addLogChannel(id: Snowflake): Boolean {
		if (channelsToLogTo.contains(id)) return false
		channelsToLogTo.add(id)
		auriel.getJson().add("log.id.channels", FileUtils.gson.toJsonTreeType(channelsToLogTo))
		auriel.saveJson()
		return true
	}
	
	fun removeLogChannel(id: Snowflake): Boolean {
		if (channelsToLogTo.isEmpty() || !channelsToLogTo.contains(id)) return false
		channelsToLogTo.remove(id)
		auriel.getJson().add("log.id.channels", FileUtils.gson.toJsonTreeType(channelsToLogTo))
		auriel.saveJson()
		return true
	}
	
	private fun log(message: String) {
		Flux.merge(channelsToLogTo.map { auriel.getGateway().getChannelById(it) }).doOnError { auriel.getLogger().logError(it) }.cast(MessageChannel::class.java)
			.flatMap {
				var content = message
				if (content.length > 2000) content = content.substring(0, 2000)
				return@flatMap it.createMessage(content)
			}.subscribe()
	}
	
	fun logDelete(message: ShallowMessage, reason: String) {
		log("__**Deleted Message**__\n${message.author.mention} in ${message.channel.mention} for: $reason" +
				"\nMessage: ||${message.message.content.replace("\n", " **\\n**")}||")
	}
	
	fun logError(message: String) {
		log("__**Bot Error**__\n${auriel.getWarrenMention()}, fix me, dammit: $message")
	}
	
	inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object: TypeToken<T>() {}.type)
	inline fun <reified T> Gson.toJsonTreeType(t: T) = toJsonTree(t, object: TypeToken<T>() {}.type)
	
}