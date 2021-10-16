package io.github.warren1001.resetbot

import com.google.gson.JsonObject

class TriviaQuestion(val question: String, val answer: String) {
	
	fun serialize(): JsonObject {
		val obj = JsonObject()
		obj.addProperty("question", question)
		obj.addProperty("answer", answer)
		return obj
	}
	
}