package io.github.warren1001.resetbot.trivia

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.warren1001.resetbot.utils.FileUtils

private var questionsJsonArray: JsonArray? = null

fun main() {
	
	val questionsJsonObject = FileUtils.readJsonLines("trivia-questions.json")
	questionsJsonArray = if (questionsJsonObject.has("questions") && questionsJsonObject["questions"].isJsonArray) questionsJsonObject["questions"].asJsonArray else
		JsonArray()
	
	
	skillLevelReq("Magic Arrow", 1)
	skillLevelReq("Fire Arrow", 1)
	skillLevelReq("Inner Sight", 1)
	skillLevelReq("Critical Strike", 1)
	skillLevelReq("Jab", 1)
	skillLevelReq("Power Strike", 6)
	skillLevelReq("Poison Javelin", 6)
	skillLevelReq("Dodge", 6)
	skillLevelReq("Cold Arrow", 6)
	skillLevelReq("Multiple Shot", 6)
	skillLevelReq("Impale", 12)
	skillLevelReq("Lightning Bolt", 12)
	
	
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

