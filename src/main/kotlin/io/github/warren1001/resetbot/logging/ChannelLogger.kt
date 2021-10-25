package io.github.warren1001.resetbot.logging

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import discord4j.common.util.Snowflake
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.component.Button
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.MessageCreateSpec
import io.github.warren1001.resetbot.Auriel
import io.github.warren1001.resetbot.createMessageWithLimit
import io.github.warren1001.resetbot.listener.ShallowMessage
import io.github.warren1001.resetbot.utils.JsonObjectBuilder
import reactor.core.publisher.Flux

class ChannelLogger(private val auriel: Auriel) {
	
	private val channelsToLogTo = mutableSetOf<Snowflake>()
	private val jsonObject: JsonObject = if (auriel.getJson().has("log") && auriel.getJson()["log"].isJsonObject) auriel.getJson()["log"].asJsonObject else JsonObject()
	private val channelsJsonArray: JsonArray = if (jsonObject.has("channels") && jsonObject["channels"].isJsonArray) jsonObject["channels"].asJsonArray else JsonArray()
	
	init {
		channelsJsonArray.map { it.asJsonObject }.forEach { channelsToLogTo.add(Snowflake.of(it["id"].asLong)) }
	}
	
	fun addLogChannel(id: Snowflake): Boolean {
		if (channelsToLogTo.contains(id)) return false
		channelsToLogTo.add(id)
		channelsJsonArray.add(JsonObjectBuilder().addProperty("id", id.asLong()).build())
		jsonObject.add("channels", channelsJsonArray)
		auriel.getJson().add("log", jsonObject)
		auriel.saveJson()
		return true
	}
	
	fun removeLogChannel(id: Snowflake): Boolean {
		if (channelsToLogTo.isEmpty() || !channelsToLogTo.contains(id)) return false
		channelsToLogTo.remove(id)
		channelsJsonArray.removeAll { it.asJsonObject["id"].asLong == id.asLong() }
		jsonObject.add("channels", channelsJsonArray)
		auriel.getJson().add("log", jsonObject)
		auriel.saveJson()
		return true
	}
	
	private fun log(spec: MessageCreateSpec) {
		Flux.merge(channelsToLogTo.map { auriel.getGateway().getChannelById(it) }).doOnError { auriel.getLogger().logError(it) }.cast(MessageChannel::class.java)
			.flatMap { it.createMessageWithLimit(spec) }.subscribe()
	}
	
	fun logDelete(message: ShallowMessage, reason: String) {
		log(MessageCreateSpec.builder().content("__**Deleted Message**__\n${message.author.mention} in ${message.channel.mention} for: $reason" +
				"\n```${message.message.content.replace("\n", "\\n").replace("```", "\\`\\`\\`")}```").build())
	}
	
	fun logDelete(message: ShallowMessage, repostReference: Snowflake, reason: String) {
		log(MessageCreateSpec.builder().content("__**Deleted Message**__\n${message.author.mention} in ${message.channel.mention} for: $reason" +
				"\n```${message.message.content.replace("\n", "**\\n**").replace("```", "``\\`")}```").addComponent(
			ActionRow.of(Button.link("https://discord.com/channels/${message.message.guildId.get().asLong()}/${message.channel.id.asLong()}/${repostReference.asLong()}", "Jump to repost"))).build())
	}
	
	fun logError(message: String) {
		log(MessageCreateSpec.builder().content("__**Bot Error**__\n${auriel.getWarren().mention}, fix me, dammit: $message").build())
	}
	
}