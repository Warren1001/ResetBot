package io.github.warren1001.resetbot.listener

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import discord4j.common.util.Snowflake
import io.github.warren1001.resetbot.utils.FileUtils
import io.github.warren1001.resetbot.utils.JsonObjectBuilder

class UserManager {
	
	companion object {
		const val EVERYONE = 0
		const val MODERATOR = 5
		const val ADMINISTRATOR = 10
	}
	
	private val permissions = mutableMapOf<Snowflake, Int>()
	private val jsonObject: JsonObject = FileUtils.readJsonLines("users.json")
	private val jsonArray: JsonArray = if (jsonObject.has("permissions") && jsonObject["permissions"].isJsonArray) jsonObject["permissions"].asJsonArray else JsonArray()
	
	init {
		jsonArray.forEach { permissions[Snowflake.of(it.asJsonObject["id"].asLong)] = it.asJsonObject["permission"].asInt }
	}
	
	fun addModerator(id: Snowflake) {
		setPermission(id, MODERATOR)
	}
	
	fun addAdministrator(id: Snowflake) {
		setPermission(id, ADMINISTRATOR)
	}
	
	fun remove(id: Snowflake): Boolean {
		if (permissions.containsKey(id)) {
			permissions.remove(id)
			jsonArray.removeAll { it.asJsonObject["id"].asLong == id.asLong() }
			savePermissions()
			return true
		}
		return false
	}
	
	fun hasPermission(id: Snowflake, permission: Int): Boolean {
		return permissions.getOrDefault(id, 0) >= permission
	}
	
	fun isModerator(id: Snowflake): Boolean {
		return hasPermission(id, MODERATOR)
	}
	
	fun isAdministrator(id: Snowflake): Boolean {
		return hasPermission(id, ADMINISTRATOR)
	}
	
	private fun setPermission(id: Snowflake, permission: Int) {
		if (permissions.containsKey(id)) {
			jsonArray.filter { it.asJsonObject["id"].asLong == id.asLong() }.forEach { it.asJsonObject.addProperty("permission", permission) }
		} else {
			jsonArray.add(JsonObjectBuilder().addProperty("id", id.asLong()).addProperty("permission", permission).build())
		}
		permissions[id] = permission
		savePermissions()
	}
	
	private fun savePermissions() {
		jsonObject.add("permissions", jsonArray)
		saveJson()
	}
	
	fun saveJson() {
		FileUtils.saveJsonLines("users.json", jsonObject)
	}
	
}
