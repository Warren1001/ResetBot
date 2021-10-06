package io.github.warren1001.resetbot

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.rest.util.Permission
import java.io.File
import java.time.Instant

class BotFilter(private val auriel: Auriel) {
	
	private val humanRoleIdFile = File("humanRoleId.txt")
	private var humanRoleId: Snowflake? = null
	private val humanChannelIdFile = File("humanChannelId.txt")
	private var humanChannelId: Snowflake? = null
	private var humanChannel: MessageChannel? = null
	private var lastAnnoyWarrenTime = 0L
	
	init {
		
		if (humanRoleIdFile.exists()) {
			humanRoleId = Snowflake.of(humanRoleIdFile.readLines()[0])
		}
		if (humanChannelIdFile.exists()) {
			humanChannelId = Snowflake.of(humanChannelIdFile.readLines()[0])
			humanChannel = auriel.getGateway().getChannelById(humanChannelId!!).doOnError { auriel.getLogger().logError(it) }.block() as MessageChannel
		}
		
		if (humanRoleId != null && humanChannelId != null) {
			checkPreviousMessages()
		}
		
	}
	
	fun humanCheck(msg: ShallowMessage): Boolean {
		if ((humanRoleId == null || humanChannelId == null) && System.currentTimeMillis() - lastAnnoyWarrenTime > (1000 * 60 * 60) /* every goddamn hour */ ) {
			if (lastAnnoyWarrenTime == 0L) auriel.getMessageListener().reply(msg, "dammit ${auriel.getWarrenMention()}, setup the bot filter channel!! go complain to him")
			auriel.getLogger().getChannelLogger().logError("u fukin noob, setup the bot filter!!!!")
			lastAnnoyWarrenTime = System.currentTimeMillis()
			return false
		}
		if (msg.getAuthorPermissions().contains(Permission.ADMINISTRATOR)) return false
		if (msg.getChannel().id == humanChannelId) {
			if (!msg.getMessage().content.isNullOrEmpty() && msg.getMessage().content.contains("human", ignoreCase = true)) {
				msg.getAuthor().addRole(humanRoleId!!).doOnError { auriel.getLogger().logError(it) }.subscribe()
			}
			msg.delete()
			return true
		}
		return false
	}
	
	fun setHumanRoleId(id: Snowflake) {
		humanRoleId = id
		if (humanChannelId != null) checkPreviousMessages()
		humanRoleIdFile.writeText(id.asString())
	}
	
	fun setHumanChannelId(id: Snowflake) {
		humanChannelId = id
		humanChannel = auriel.getGateway().getChannelById(humanChannelId!!).doOnError { auriel.getLogger().logError(it) }.block() as MessageChannel
		if (humanRoleId != null) checkPreviousMessages()
		humanChannelIdFile.writeText(id.asString())
	}
	
	fun checkPreviousMessages() {
		humanChannel!!.getMessagesBefore(Snowflake.of(Instant.now())).doOnError { auriel.getLogger().logError(it) }.map { ShallowMessage(auriel, it) }
			.subscribe {
				if (it.getMessage().author.isEmpty) it.delete()
				else humanCheck(it)
			}
	}
	
}