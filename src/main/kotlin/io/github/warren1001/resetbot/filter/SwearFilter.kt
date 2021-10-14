package io.github.warren1001.resetbot.filter

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.warren1001.resetbot.Auriel
import io.github.warren1001.resetbot.listener.ShallowMessage
import java.util.regex.Pattern

class SwearFilter(private val auriel: Auriel) {
	
	private val patterns = mutableSetOf<Filter>()
	private val jsonArray: JsonArray
	
	init {
		
		if (auriel.getJson().has("filter.filters") && auriel.getJson()["filter.filters"].isJsonArray) {
			
			jsonArray = auriel.getJson()["filter.filters"].asJsonArray
			jsonArray.forEach {
				patterns.add(Filter(Pattern.compile(it.asJsonObject["pattern"].asString), it.asJsonObject["replacement"].asString))
			}
			
		} else jsonArray = JsonArray()
	
	}
	
	fun addSwearFilterPattern(pattern: String, replacement: String): Boolean {
		if (containsPattern(pattern)) return false
		patterns.add(Filter(Pattern.compile(pattern), replacement))
		val patternObj = JsonObject()
		patternObj.addProperty("pattern", pattern)
		patternObj.addProperty("replacement", replacement)
		jsonArray.add(patternObj)
		auriel.getJson().add("filter.filters", jsonArray)
		auriel.saveJson()
		return true
	}
	
	fun constructBasicPattern(word: String): String {
		val builder = StringBuilder("(?i)(")
		word.forEach { builder.append('[').append(it).append(']').append('+') }
		builder.append(')')
		return builder.toString()
	}
	
	fun removeSwearFilterPattern(pattern: String): Boolean {
		if (patterns.isEmpty() || !containsPattern(pattern)) return false
		patterns.removeIf { it.pattern.pattern() == pattern }
		jsonArray.removeAll { it.asJsonObject["pattern"].asString == pattern }
		auriel.getJson().add("filter.filters", jsonArray)
		auriel.saveJson()
		return true
	}
	
	fun containsPattern(pattern: String): Boolean {
		return patterns.any { it.pattern.pattern() == pattern }
	}
	
	fun getListOfPatterns() : String {
		return patterns.joinToString("\n") { it.pattern.pattern() }
	}
	
	fun checkMessage(message: ShallowMessage, repost: Boolean = true, duration: Long = -1): Boolean {
		
		if (message.author.isBot || message.isModerator()) return false
		
		var content = message.message.content
		val stringBuilder = StringBuilder("swearing")
		var flagged = false
		var foundWords = ""
		
		patterns.forEach {
			
			val matcher = it.pattern.matcher(content)
			
			if (matcher.find()) {
				flagged = true
				
				stringBuilder.append("\n${it.pattern.pattern()}: ")
				for (i in 1..matcher.groupCount()) {
					val swear = matcher.group(i)
					stringBuilder.append(swear)
					foundWords += "$swear "
				}
				
				content = matcher.replaceAll(it.replacement)
			}
			
		}
		
		if (!flagged) return false
		
		auriel.getMessageListener().delete(message, stringBuilder.toString())
		
		val replyContent = "Your message contained a swear or censored word in it, so it was deleted. Remember that this is a family friendly community. :)"
		
		if (repost) {
			auriel.getMessageListener().replyDeleted(message, "$replyContent\n\n${message.author.mention} said: $content")
		} else {
			auriel.getMessageListener().replyDeleted(message, "I am sending you a private message, please check it for why your post was deleted.", duration)
			var pmMsg = "Your message contained a swear or censored word in it, so it was deleted. Remember that this is a family friendly community. :)\n" +
					"These are the words (or parts of words) that need to be removed: $foundWords\n```\n${message.message.content.replace("`", "\\`")}\n```"
			if (pmMsg.length > 2000) pmMsg = pmMsg.substring(0, 2000)
			message.author.privateChannel.flatMap { it.createMessage(pmMsg) }.subscribe()
		}
		
		
		return true
	}

}