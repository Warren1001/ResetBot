package io.github.warren1001.resetbot

import com.google.gson.JsonObject
import discord4j.common.util.Snowflake

class TriviaData {
	
	private val userId: Snowflake
	
	private var fastestAnswer: Long
	private var numQuestionsAnswered: Int
	private var longestStreak: Int
	
	constructor(obj: JsonObject): this(Snowflake.of(obj["user"].asLong), obj["fastest-answer"].asLong, obj["questions-answered"].asInt, obj["longest-streak"].asInt)
	
	constructor(id: Snowflake): this(id, Long.MAX_VALUE, 0, 0)
	
	constructor(id: Snowflake, fastest: Long, answered: Int, longest: Int) {
		userId = id
		fastestAnswer = fastest
		numQuestionsAnswered = answered
		longestStreak = longest
	}
	
	fun answeredQuestionCorrectly(time: Long, streak: Int) {
		if (time < fastestAnswer) fastestAnswer = time
		if (streak > longestStreak) longestStreak = streak
		numQuestionsAnswered++
	}
	
	fun getUserId(): Snowflake {
		return userId
	}
	
	fun getFastestAnswer(): Long {
		return fastestAnswer
	}
	
	fun getNumQuestionsAnswered(): Int {
		return numQuestionsAnswered
	}
	
	fun getLongestStreak(): Int {
		return longestStreak
	}
	
	fun serialize(): JsonObject {
		val obj = JsonObject()
		obj.addProperty("user", userId.asLong())
		obj.addProperty("fastest-answer", fastestAnswer)
		obj.addProperty("questions-answered", numQuestionsAnswered)
		obj.addProperty("longest-streak", longestStreak)
		return obj
	}
	
}