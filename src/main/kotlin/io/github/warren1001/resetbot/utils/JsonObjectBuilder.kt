package io.github.warren1001.resetbot.utils

import com.google.gson.JsonElement
import com.google.gson.JsonObject

class JsonObjectBuilder {
	
	private val jsonObject = JsonObject()
	
	fun add(key: String, value: JsonElement): JsonObjectBuilder {
		jsonObject.add(key, value)
		return this
	}
	
	fun addProperty(key: String, value: Number): JsonObjectBuilder {
		jsonObject.addProperty(key, value)
		return this
	}
	
	fun addProperty(key: String, value: String): JsonObjectBuilder {
		jsonObject.addProperty(key, value)
		return this
	}
	
	fun addProperty(key: String, value: Char): JsonObjectBuilder {
		jsonObject.addProperty(key, value)
		return this
	}
	
	fun addProperty(key: String, value: Boolean): JsonObjectBuilder {
		jsonObject.addProperty(key, value)
		return this
	}
	
	fun build(): JsonObject {
		return jsonObject
	}
	
}