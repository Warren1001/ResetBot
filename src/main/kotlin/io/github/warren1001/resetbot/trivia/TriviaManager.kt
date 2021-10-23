package io.github.warren1001.resetbot.trivia

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.MessageChannel
import io.github.warren1001.resetbot.Auriel
import io.github.warren1001.resetbot.listener.ShallowMessage
import io.github.warren1001.resetbot.utils.FileUtils
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.timer
import kotlin.random.Random

class TriviaManager(private val auriel: Auriel) {
	
	private val regex = Regex("['-]")
	private val triviaQuestions = mutableListOf<TriviaQuestion>()
	private val triviaData = mutableMapOf<Snowflake, TriviaData>()
	private val triviaJsonObject: JsonObject = if (auriel.getJson().has("trivia") && auriel.getJson()["trivia"].isJsonObject) auriel.getJson()["trivia"].asJsonObject else JsonObject()
	private var triviaChannelId: Snowflake = Snowflake.of(if (triviaJsonObject.has("channel")) triviaJsonObject["channel"].asLong else 0L)
	private var triviaChannel: MessageChannel? = null
	private var questionsJsonObject = FileUtils.readJsonLines("trivia-questions.json")
	private var questionsJsonArray: JsonArray = if (questionsJsonObject.has("questions") && questionsJsonObject["questions"].isJsonArray) questionsJsonObject["questions"].asJsonArray else
		JsonArray()
	private val bestJsonObject = FileUtils.readJsonLines("trivia-best.json")
	private val fastestJsonObject = if (bestJsonObject.has("fastest-answer") && bestJsonObject["fastest-answer"].isJsonObject) bestJsonObject["fastest-answer"].asJsonObject else JsonObject()
	private val mostQJsonObject = if (bestJsonObject.has("most-questions") && bestJsonObject["most-questions"].isJsonObject) bestJsonObject["most-questions"].asJsonObject else JsonObject()
	private val longestJsonObject = if (bestJsonObject.has("longest-streak") && bestJsonObject["longest-streak"].isJsonObject) bestJsonObject["longest-streak"].asJsonObject else JsonObject()
	private val dataJsonObject = FileUtils.readJsonLines("trivia-data.json")
	private val usersJsonArray = if (dataJsonObject.has("users") && dataJsonObject["users"].isJsonArray) dataJsonObject["users"].asJsonArray else JsonArray()
	
	private val fastestAnswer = LazyTriviaObject(Snowflake.of(0L), "", Long.MAX_VALUE)
	private val mostAnsweredQuestions = LazyTriviaObject(Snowflake.of(0L), "", 0)
	private val longestStreak = LazyTriviaObject(Snowflake.of(0L), "", 0)
	private val currentStreak = LazyTriviaObject(Snowflake.of(0L), "", 0)
	
	private var stop = if (triviaJsonObject.has("stopped")) triviaJsonObject["stopped"].asBoolean else false
	private var activeQuestion = false
	private var timeAsked = 0L
	private var timer: Timer? = null
	private var previousQuestion: TriviaQuestion? = null
	private var currentQuestion: TriviaQuestion? = null
	private var revealingMessage: RevealingMessage? = null
	
	init {
		questionsJsonArray.map { it.asJsonObject }.map { TriviaQuestion(it["question"].asString, it["answer"].asString) }.toCollection(triviaQuestions)
		if (fastestJsonObject.has("user")) {
			fastestAnswer.userId = Snowflake.of(fastestJsonObject["user"].asLong)
			fastestAnswer.data = fastestJsonObject["data"].asLong
			auriel.getGateway().getUserById(fastestAnswer.userId).subscribe { fastestAnswer.username = it.username }
		}
		if (mostQJsonObject.has("user")) {
			mostAnsweredQuestions.userId = Snowflake.of(mostQJsonObject["user"].asLong)
			mostAnsweredQuestions.data = mostQJsonObject["data"].asInt
			auriel.getGateway().getUserById(mostAnsweredQuestions.userId).subscribe { mostAnsweredQuestions.username = it.username }
		}
		if (longestJsonObject.has("user")) {
			longestStreak.userId = Snowflake.of(longestJsonObject["user"].asLong)
			longestStreak.data = longestJsonObject["data"].asInt
			auriel.getGateway().getUserById(longestStreak.userId).subscribe { longestStreak.username = it.username }
		}
		usersJsonArray.map { TriviaData(it.asJsonObject) }.forEach { data -> triviaData[data.getUserId()] = data }
		timer("triviaUserDataSave", true, (1000 * 60 * 5).toLong(), (1000 * 60 * 5).toLong()) { savePlayerData() }
		if (triviaChannelId.asLong() != 0L) {
			auriel.getGateway().getChannelById(triviaChannelId).cast(MessageChannel::class.java).subscribe {
				triviaChannel = it
				if (!stop) setRandomQuestion()
			}
		}
	}
	
	fun handle(msg: ShallowMessage): Boolean {
		if (stop || !activeQuestion || msg.message.channelId != triviaChannelId) return false
		if (msg.message.content.replace(regex, "").equals(currentQuestion!!.answer.replace(regex, ""), true)) {
			activeQuestion = false
			timer!!.cancel()
			val author = msg.author
			val data = triviaData.computeIfAbsent(author.id) {
				val jsonObject = JsonObject()
				usersJsonArray.add(jsonObject)
				return@computeIfAbsent TriviaData(jsonObject, it)
			}
			//val time = System.currentTimeMillis() - timeAsked
			val time = msg.message.timestamp.toEpochMilli() - timeAsked
			var content = "Correct!"
			if (currentStreak.userId != author.id) {
				if (currentStreak.data > 1) {
					content += " You broke ${currentStreak.username}'s streak of ${currentStreak.data}!"
				}
				updateCurrentStreak(author.id, author.username, 1)
			} else {
				currentStreak.data++
			}
			data.incrementNumQuestionsAnswered()
			val streak = currentStreak.data
			content += " You answered in ${convertToSecs(time)} seconds."
			if (time < data.getFastestAnswer()) {
				if (data.getFastestAnswer() != Long.MAX_VALUE) content += " New personal best! Your previous best time was ${convertToSecs(data.getFastestAnswer())} seconds."
				data.setFastestAnswer(time)
			}
			if (isFastestAnswer(time)) {
				val oldTime = fastestAnswer.data
				val oldUser = fastestAnswer.username
				val oldUserId = fastestAnswer.userId
				updateFastestAnswer(author.id, author.username, time)
				if (oldTime != Long.MAX_VALUE && oldUserId != author.id) content += " New record! The previous record was ${convertToSecs(oldTime)} seconds, held by $oldUser."
			}
			if (streak > 1) {
				content += " You are on a $streak question streak!"
				if (streak > data.getLongestStreak()) {
					if (data.getLongestStreak() > 1) content += " New personal best! Your previous best was ${data.getLongestStreak()} questions."
					data.setLongestStreak(streak)
				}
				if (isLongestStreak(streak)) {
					val oldStreak = longestStreak.data
					val oldUser = longestStreak.username
					val oldUserId = longestStreak.userId
					updateLongestStreak(author.id, author.username, streak)
					if (oldUserId != author.id) content += " New record! The previous record was $oldStreak questions, held by $oldUser."
				}
			}
			if (isMostAnsweredQuestions(data.getNumQuestionsAnswered())) {
				if (data.getNumQuestionsAnswered() > 10 && mostAnsweredQuestions.userId != author.id) content += " You have answered ${data.getNumQuestionsAnswered()} questions, " +
						"passing ${mostAnsweredQuestions.username}'s record of ${mostAnsweredQuestions.data} questions answered!"
				updateMostAnsweredQuestions(author.id, author.username, data.getNumQuestionsAnswered())
			}
			msg.reply(content)
			savePlayerData()
			Timer("nextTriviaQuestionHandle", true).schedule((1000 * 15).toLong()) { if (!stop) setRandomQuestion() }
		}
		return true
	}
	
	fun stop(): Boolean {
		if (!stop) {
			stop = true
			activeQuestion = false
			previousQuestion = currentQuestion
			if (timer != null) timer!!.cancel()
			if (triviaChannel != null) triviaChannel!!.createMessage("Trivia has been stopped. Blame ${auriel.getWarren().mention} :rage:").subscribe()
			triviaJsonObject.addProperty("stopped", stop)
			auriel.getJson().add("trivia", triviaJsonObject)
			auriel.saveJson()
			return true
		}
		return false
	}
	
	fun start(): Boolean {
		if (stop) {
			stop = false
			setRandomQuestion()
			triviaJsonObject.addProperty("stopped", stop)
			auriel.getJson().add("trivia", triviaJsonObject)
			auriel.saveJson()
			return true
		}
		return false
	}
	
	fun reloadQuestions() {
		if (triviaChannel != null) triviaChannel!!.createMessage("Reloading the question pool, have to restart trivia really quick.").subscribe()
		stop()
		questionsJsonObject = FileUtils.readJsonLines("trivia-questions.json")
		questionsJsonArray = if (questionsJsonObject.has("questions") && questionsJsonObject["questions"].isJsonArray) questionsJsonObject["questions"].asJsonArray else JsonArray()
		triviaQuestions.clear()
		questionsJsonArray.map { it.asJsonObject }.map { TriviaQuestion(it["question"].asString, it["answer"].asString) }.toCollection(triviaQuestions)
		start()
	}
	
	fun setChannelId(id: Snowflake) {
		triviaChannelId = id
		triviaJsonObject.addProperty("channel", triviaChannelId.asLong())
		auriel.getJson().add("trivia", triviaJsonObject)
		auriel.saveJson()
		auriel.getGateway().getChannelById(triviaChannelId).cast(MessageChannel::class.java).subscribe {
			triviaChannel = it
			if (triviaQuestions.isNotEmpty() && !stop) setRandomQuestion()
		}
	}
	
	fun setChannel(channel: MessageChannel) {
		triviaChannelId = channel.id
		triviaJsonObject.addProperty("channel", triviaChannelId.asLong())
		auriel.getJson().add("trivia", triviaJsonObject)
		auriel.saveJson()
		triviaChannel = channel
		if (triviaQuestions.isNotEmpty() && !stop) {
			setRandomQuestion()
		}
	}
	
	fun addQuestion(question: String, answer: String) {
		return addQuestion(TriviaQuestion(question, answer))
	}
	
	fun addQuestion(question: TriviaQuestion) {
		val isFirst = triviaQuestions.isEmpty()
		triviaQuestions.add(question)
		questionsJsonArray.add(question.serialize())
		if (isFirst && triviaChannel != null) setRandomQuestion()
		saveQuestions()
	}
	
	fun removeQuestion(questionPartial: String): Boolean {
		val b1 = triviaQuestions.removeIf { it.question.contains(questionPartial) }
		val b2 = questionsJsonArray.removeAll { it.asJsonObject["question"].asString.contains(questionPartial) }
		return b1 && b2
	}
	
	fun saveQuestions() {
		questionsJsonObject.add("questions", questionsJsonArray)
		FileUtils.saveJsonLines("trivia-questions.json", questionsJsonObject)
	}
	
	fun savePlayerData() {
		dataJsonObject.add("users", usersJsonArray)
		FileUtils.saveJsonLines("trivia-data.json", dataJsonObject)
	}
	
	fun isFastestAnswer(time: Long): Boolean {
		return time < fastestAnswer.data
	}
	
	fun isMostAnsweredQuestions(amount: Int): Boolean {
		return amount > mostAnsweredQuestions.data
	}
	
	fun isLongestStreak(amount: Int): Boolean {
		return amount > longestStreak.data
	}
	
	fun updateFastestAnswer(id: Snowflake, username: String, time: Long): Boolean {
		if (isFastestAnswer(time)) {
			fastestAnswer.userId = id
			fastestAnswer.username = username
			fastestAnswer.data = time
			fastestJsonObject.addProperty("user", id.asLong())
			fastestJsonObject.addProperty("data", time)
			bestJsonObject.add("fastest-answer", fastestJsonObject)
			FileUtils.saveJsonLines("trivia-best.json", bestJsonObject)
			return true
		}
		return false
	}
	
	fun updateMostAnsweredQuestions(id: Snowflake, username: String, amount: Int): Boolean {
		if (isMostAnsweredQuestions(amount)) {
			mostAnsweredQuestions.userId = id
			mostAnsweredQuestions.username = username
			mostAnsweredQuestions.data = amount
			mostQJsonObject.addProperty("user", id.asLong())
			mostQJsonObject.addProperty("data", amount)
			bestJsonObject.add("most-questions", mostQJsonObject)
			FileUtils.saveJsonLines("trivia-best.json", bestJsonObject)
			return true
		}
		return false
	}
	
	fun updateLongestStreak(id: Snowflake, username: String, amount: Int): Boolean {
		if (isLongestStreak(amount)) {
			longestStreak.userId = id
			longestStreak.username = username
			longestStreak.data = amount
			longestJsonObject.addProperty("user", id.asLong())
			longestJsonObject.addProperty("data", amount)
			bestJsonObject.add("longest-streak", longestJsonObject)
			FileUtils.saveJsonLines("trivia-best.json", bestJsonObject)
			return true
		}
		return false
	}
	
	fun updateCurrentStreak(id: Snowflake, username: String, amount: Int) {
		currentStreak.userId = id
		currentStreak.username = username
		currentStreak.data = amount
	}
	
	fun setRandomQuestion() {
		if (triviaQuestions.isNotEmpty()) {
			previousQuestion = currentQuestion
			currentQuestion = triviaQuestions[Random.nextInt(triviaQuestions.size)]
			while (currentQuestion == previousQuestion) {
				currentQuestion = triviaQuestions[Random.nextInt(triviaQuestions.size)]
			}
			revealingMessage = RevealingMessage(currentQuestion!!.answer)
			triviaChannel!!.createMessage("**${currentQuestion!!.question}**").subscribe {
				activeQuestion = true
				timeAsked = it.timestamp.toEpochMilli()
			}
			val answerLength = currentQuestion!!.answer.length
			var secondsInterval = 15
			if (answerLength > 30) secondsInterval -= 7
			else if (answerLength > 25) secondsInterval -= 5
			else if (answerLength > 20) secondsInterval -= 3
			else if (answerLength > 15) secondsInterval -= 1
			val interval = (1000 * secondsInterval).toLong()
			timer = timer("triviaHintTimer", true, interval, interval) {
				val count = (answerLength / 10).coerceAtLeast(1)
				if (revealingMessage!!.wouldBeFullyRevealed(count)) {
					activeQuestion = false
					timer!!.cancel()
					triviaChannel!!.createMessage("__${currentQuestion!!.question}__\nThe answer was: ${currentQuestion!!.answer}").subscribe()
					currentStreak.userId = Snowflake.of(0L)
					currentStreak.username = ""
					currentStreak.data = 0
					Timer("nextTriviaQuestionRecursive", true).schedule((1000 * 10).toLong()) { if (!stop) setRandomQuestion() }
					return@timer
				}
				triviaChannel!!.createMessage("__${currentQuestion!!.question}__\nHint:  ${revealingMessage!!.reveal(count).replace("_", "\\_").replace(" ", "  ")}").subscribe()
			}
		}
	}
	
	fun convertToSecs(time: Long): String {
		val seconds = time.toDouble() / 1000.0
		return if (seconds < 1) seconds.format(4) else seconds.format(3)
	}
	
	fun Double.format(digits: Int) = "%.${digits}f".format(this)
	
}