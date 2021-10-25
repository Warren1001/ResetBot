package io.github.warren1001.resetbot.filter

import com.google.gson.JsonObject
import discord4j.common.util.Snowflake
import io.github.warren1001.resetbot.Auriel
import io.github.warren1001.resetbot.listener.ShallowMessage

class MuteFilter(private val auriel: Auriel) {
	
	private val filterJsonObject: JsonObject = if (auriel.getJson().has("filter") && auriel.getJson()["filter"].isJsonObject) auriel.getJson()["filter"].asJsonObject else JsonObject()
	private var muteRoleId: Snowflake = Snowflake.of(if (filterJsonObject.has("mute-role")) filterJsonObject["mute-role"].asLong else 0L)
	private val httpsLinkPattern: Regex = Regex("http[s]?://")
	
	fun check(msg: ShallowMessage): Boolean {
		if (muteRoleId.asLong() == 0L || msg.isModerator()) return false
		val content = msg.message.content
		if (content.contains("nitro", ignoreCase = true) && content.contains(httpsLinkPattern) && content.contains("@everyone", true)) {
			msg.message.authorAsMember.flatMap { it.addRole(muteRoleId, "Suspected scammer") }.subscribe()
			msg.delete("suspected scammer, muted")
			msg.dm("You have been muted on MrLlamaSC's Discord for posting what is seemingly a scam website link.\n" +
					"If this is an error, please contact ${auriel.getWarren().mention} or an online moderator to be unmuted.")
			return true
		}
		return false
	}
	
	fun setMuteRoleId(id: Snowflake) {
		muteRoleId = id
		filterJsonObject.addProperty("mute-role", muteRoleId.asLong())
		auriel.getJson().add("filter", filterJsonObject)
		auriel.saveJson()
	}

}