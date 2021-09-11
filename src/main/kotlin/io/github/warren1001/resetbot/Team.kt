package io.github.warren1001.resetbot

import discord4j.common.util.Snowflake

class Team(val teamName: String, val softcore: Boolean) {
	
	private val userInfos = mutableListOf<UserInfo>()
	
	fun addUser(id: Snowflake, build: String) {
		addUser(UserInfo(id, build))
	}
	
	fun addUser(info: UserInfo) {
		userInfos.add(info)
	}
	
	fun removeUser(id: Snowflake) : Boolean {
		return userInfos.removeIf { it.id == id }
	}
	
	fun isFull(): Boolean {
		return userInfos.size == 8
	}
	
}