package io.github.warren1001.resetbot.listener

import discord4j.common.util.Snowflake
import io.github.warren1001.resetbot.Auriel

class UserManager(private val auriel: Auriel) {
	
	companion object {
		const val EVERYONE = 0
		const val MODERATOR = 5
		const val ADMINISTRATOR = 10
	}
	
	private val adminRoleId: Snowflake = Snowflake.of(if (auriel.getJson().has("users.role-id.admin")) auriel.getJson()["users.role-id.admin"].asLong else 0L)
	private val modRoleId: Snowflake = Snowflake.of(if (auriel.getJson().has("users.role-id.mod")) auriel.getJson()["users.role-id.mod"].asLong else 0L)
	private val mods = mutableSetOf<Snowflake>()
	private val admins = mutableSetOf<Snowflake>()
	private val permissions = mutableMapOf<Snowflake, Int>()
	
	fun addModerator(id: Snowflake) {
		mods.add(id)
	}
	
	fun removeModerator(id: Snowflake) {
		permissions[id] = MODERATOR
	}
	
	fun addAdministrator(id: Snowflake) {
		permissions[id] = ADMINISTRATOR
	}
	
	fun remove(id: Snowflake) {
		permissions.remove(id)
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
	
	fun isModRole(id: Snowflake): Boolean {
		return modRoleId == id
	}
	
}
