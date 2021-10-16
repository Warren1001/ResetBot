package io.github.warren1001.resetbot

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import discord4j.common.util.Snowflake
import io.github.warren1001.resetbot.listener.ShallowMessage
import io.github.warren1001.resetbot.utils.FileUtils
import kotlin.random.Random

class TriviaManager(private val auriel: Auriel) {
	
	private val regex = Regex("['-]")
	private val triviaQuestions = mutableListOf<TriviaQuestion>()
	private val triviaData = mutableMapOf<Snowflake, TriviaData>()
	private val triviaJsonObject: JsonObject = if (auriel.getJson().has("trivia") && auriel.getJson()["trivia"].isJsonObject) auriel.getJson()["trivia"].asJsonObject else JsonObject()
	private var triviaChannelId: Snowflake = Snowflake.of(if (triviaJsonObject.has("channel") && triviaJsonObject["channel"].isJsonArray) triviaJsonObject["channel"].asLong else 0L)
	private val questionsJsonObject = FileUtils.readJsonLines("trivia-questions.json")
	private val questionsJsonArray: JsonArray = if (questionsJsonObject.has("questions") && questionsJsonObject["questions"].isJsonArray) questionsJsonObject["questions"].asJsonArray else
		JsonArray()
	private val bestJsonObject = FileUtils.readJsonLines("trivia-best.json")
	private val fastestJsonObject = if (bestJsonObject.has("fastest-answer") && bestJsonObject["fastest-answer"].isJsonObject) bestJsonObject["fastest-answer"].asJsonObject else JsonObject()
	private val mostQJsonObject = if (bestJsonObject.has("most-questions") && bestJsonObject["most-questions"].isJsonObject) bestJsonObject["most-questions"].asJsonObject else JsonObject()
	private val longestJsonObject = if (bestJsonObject.has("longest-streak") && bestJsonObject["longest-streak"].isJsonObject) bestJsonObject["longest-streak"].asJsonObject else JsonObject()
	private val dataJsonObject = FileUtils.readJsonLines("trivia-data.json")
	private val usersJsonArray = if (dataJsonObject.has("users") && dataJsonObject["users"].isJsonArray) dataJsonObject["users"].asJsonArray else JsonArray()
	
	private val fastestAnswer = MutablePair(Snowflake.of(0L), Long.MAX_VALUE)
	private val mostAnsweredQuestions = MutablePair(Snowflake.of(0L), Int.MAX_VALUE)
	private val longestStreak = MutablePair(Snowflake.of(0L), Int.MAX_VALUE)
	
	private var currentlyRunning = false
	private var timeAsked = 0L
	private var currentQuestion: TriviaQuestion? = null

	init {
		questionsJsonArray.map { it.asJsonObject }.map { TriviaQuestion(it["question"].asString, it["answer"].asString) }.toCollection(triviaQuestions)
		if (fastestJsonObject.has("user")) {
			fastestAnswer.first = Snowflake.of(fastestJsonObject["user"].asLong)
			fastestAnswer.second = fastestJsonObject["data"].asLong
		}
		if (mostQJsonObject.has("user")) {
			mostAnsweredQuestions.first = Snowflake.of(mostQJsonObject["user"].asLong)
			mostAnsweredQuestions.second = mostQJsonObject["data"].asInt
		}
		if (longestJsonObject.has("user")) {
			longestStreak.first = Snowflake.of(longestJsonObject["user"].asLong)
			longestStreak.second = longestJsonObject["data"].asInt
		}
		usersJsonArray.map { TriviaData(it.asJsonObject) }.forEach { data -> triviaData[data.getUserId()] = data }
		setRandomQuestion()
	}
	
	fun handle(msg: ShallowMessage): Boolean {
		if(!currentlyRunning || msg.message.channelId != triviaChannelId) return false
		if (msg.message.content.replace(regex, "").equals(currentQuestion!!.answer, true)) {
		
		}
		return true
	}
	
	fun answeredCorrectly(userId: Snowflake) {
	
	}
	
	fun setChannelId(id: Snowflake) {
		triviaChannelId = id
		triviaJsonObject.addProperty("channel", triviaChannelId.asLong())
		auriel.getJson().add("trivia", triviaJsonObject)
		auriel.saveJson()
	}
	
	fun addQuestion(question: String, answer: String) {
		return addQuestion(TriviaQuestion(question, answer.replace(regex, "")))
	}
	
	fun addQuestion(question: TriviaQuestion) {
		val isFirst = triviaQuestions.isEmpty()
		triviaQuestions.add(question)
		questionsJsonArray.add(question.serialize())
		if (isFirst) setRandomQuestion()
	}
	
	fun saveQuestions() {
		questionsJsonObject.add("questions", questionsJsonArray)
		FileUtils.saveJsonLines("trivia-questions", questionsJsonObject)
	}
	
	fun savePlayerData() {
	
	}
	
	fun updateFastestAnswer(id: Snowflake, time: Long): Boolean {
		if (time < fastestAnswer.second) {
			fastestAnswer.first = id
			fastestAnswer.second = time
			fastestJsonObject.addProperty("user", id.asLong())
			fastestJsonObject.addProperty("data", time)
			bestJsonObject.add("fastest-answer", fastestJsonObject)
			FileUtils.saveJsonLines("trivia-best.json", bestJsonObject)
			return true
		}
		return false
	}
	
	fun updateMostAnsweredQuestions(id: Snowflake, amount: Int): Boolean {
		if (amount > mostAnsweredQuestions.second) {
			mostAnsweredQuestions.first = id
			mostAnsweredQuestions.second = amount
			mostQJsonObject.addProperty("user", id.asLong())
			mostQJsonObject.addProperty("data", amount)
			bestJsonObject.add("most-questions", mostQJsonObject)
			FileUtils.saveJsonLines("trivia-best.json", bestJsonObject)
			return true
		}
		return false
	}
	
	fun updateLongestStreak(id: Snowflake, amount: Int): Boolean {
		if (amount > longestStreak.second) {
			longestStreak.first = id
			longestStreak.second = amount
			longestJsonObject.addProperty("user", id.asLong())
			longestJsonObject.addProperty("data", amount)
			bestJsonObject.add("longest-streak", longestJsonObject)
			FileUtils.saveJsonLines("trivia-best.json", bestJsonObject)
			return true
		}
		return false
	}
	
	fun setRandomQuestion() {
		if (triviaQuestions.isNotEmpty()) {
			currentQuestion = triviaQuestions[Random.nextInt(triviaQuestions.size)]
			// TODO
		}
	}

}