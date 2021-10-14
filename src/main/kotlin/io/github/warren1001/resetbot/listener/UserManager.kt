package io.github.warren1001.resetbot.listener

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import discord4j.common.util.Snowflake
import io.github.warren1001.resetbot.utils.FileUtils

class UserManager {
	
	companion object {
		const val EVERYONE = 0
		const val MODERATOR = 5
		const val ADMINISTRATOR = 10
	}
	
	private val jsonObject = FileUtils.readJsonLines("users.json")
	private val permissions = mutableMapOf<Snowflake, Int>()
	private val jsonArray: JsonArray
	
	init {
		if (jsonObject.has("permissions") && jsonObject["permissions"].isJsonArray) {
			jsonArray = jsonObject["permissions"].asJsonArray
			jsonArray.forEach { permissions[Snowflake.of(it.asJsonObject["id"].asLong)] = it.asJsonObject["permission"].asInt }
		} else jsonArray = JsonArray()
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
			jsonObject.add("permissions", jsonArray)
			saveJson()
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
			val permObj = JsonObject()
			permObj.addProperty("id", id.asLong())
			permObj.addProperty("permission", ADMINISTRATOR)
			jsonArray.add(permObj)
		}
		permissions[id] = permission
		jsonObject.add("permissions", jsonArray)
		saveJson()
	}
	
	fun saveJson() {
		FileUtils.saveJsonLines("users.json", jsonObject)
	}
	
}
