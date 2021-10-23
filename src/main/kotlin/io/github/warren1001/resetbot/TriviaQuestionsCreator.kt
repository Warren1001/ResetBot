package io.github.warren1001.resetbot

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.warren1001.resetbot.utils.FileUtils

private var questionsJsonArray: JsonArray? = null

fun main() {
	
	val questionsJsonObject = FileUtils.readJsonLines("trivia-questions.json")
	questionsJsonArray = if (questionsJsonObject.has("questions") && questionsJsonObject["questions"].isJsonArray) questionsJsonObject["questions"].asJsonArray else
		JsonArray()
	
	
	skillLevelReq("Magic Arrow", 1)
	
	
	questionsJsonObject.add("questions", questionsJsonArray)
	FileUtils.saveJsonLines("trivia-questions.json", questionsJsonObject)
	
}

fun skillLevelReq(skill: String, level: Int) {
	add("What is the level requirement of $skill?", level.toString())
}

fun add(question: String, answer: String) {
	val obj = JsonObject()
	obj.addProperty("question", question)
	obj.addProperty("answer", answer)
	questionsJsonArray!!.add(obj)
}

