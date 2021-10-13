package io.github.warren1001.resetbot

import discord4j.common.util.Snowflake

class UserManager(private val auriel: Auriel) {
	
	private val adminRoleId: Snowflake = Snowflake.of(if (auriel.getJson().has("users.role-id.admin")) auriel.getJson()["users.role-id.admin"].asLong else 0L)
	private val modRoleId: Snowflake = Snowflake.of(if (auriel.getJson().has("users.role-id.mod")) auriel.getJson()["users.role-id.mod"].asLong else 0L)
	private val mods = mutableSetOf<Snowflake>()
	private val admins = mutableSetOf<Snowflake>()
	
	fun addModerator(id: Snowflake) {
		mods.add(id)
	}
	
	fun addAdmin(id: Snowflake) {
		admins.add(id)
	}
	
	fun removeModerator(id: Snowflake) {
		mods.remove(id)
	}
	
	fun removeAdmin(id: Snowflake) {
		admins.remove(id)
	}
	
	fun isModerator(id: Snowflake): Boolean {
		return mods.contains(id)
	}
	
	fun isAdmin(id: Snowflake): Boolean {
		return admins.contains(id)
	}
	
	fun isModRole(id: Snowflake): Boolean {
		return modRoleId == id
	}
	
}
