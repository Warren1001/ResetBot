package io.github.warren1001.resetbot

import com.google.gson.JsonObject
import discord4j.common.util.Snowflake

class TriviaData {
	
	private val jsonObject: JsonObject
	private val userId: Snowflake
	
	private var fastestAnswer: Long
	private var numQuestionsAnswered: Int
	private var longestStreak: Int
	
	constructor(obj: JsonObject): this(obj, Snowflake.of(obj["user"].asLong), obj["fastest-answer"].asLong, obj["questions-answered"].asInt, obj["longest-streak"].asInt)
	
	constructor(jsonObject: JsonObject, id: Snowflake): this(jsonObject, id, Long.MAX_VALUE, 0, 0) {
		jsonObject.addProperty("user", id.asLong())
		jsonObject.addProperty("fastest-answer", Long.MAX_VALUE)
		jsonObject.addProperty("questions-answered", 0)
		jsonObject.addProperty("longest-streak", 0)
	}
	
	constructor(jsonObject: JsonObject, id: Snowflake, fastest: Long, answered: Int, longest: Int) {
		this.jsonObject = jsonObject
		userId = id
		fastestAnswer = fastest
		numQuestionsAnswered = answered
		longestStreak = longest
	}
	
	fun getUserId(): Snowflake {
		return userId
	}
	
	fun setFastestAnswer(time: Long) {
		fastestAnswer = time
		jsonObject.addProperty("fastest-answer", time)
	}
	
	fun getFastestAnswer(): Long {
		return fastestAnswer
	}
	
	fun setNumQuestionsAnswered(amount: Int) {
		numQuestionsAnswered = amount
		jsonObject.addProperty("questions-answered", amount)
	}
	
	fun incrementNumQuestionsAnswered() {
		numQuestionsAnswered++
		jsonObject.addProperty("questions-answered", numQuestionsAnswered)
	}
	
	fun getNumQuestionsAnswered(): Int {
		return numQuestionsAnswered
	}
	
	fun setLongestStreak(amount: Int) {
		longestStreak = amount
		jsonObject.addProperty("longest-streak", amount)
	}
	
	fun incrementLongestStreak() {
		longestStreak++
		jsonObject.addProperty("longest-streak", longestStreak)
	}
	
	fun getLongestStreak(): Int {
		return longestStreak
	}
	
}