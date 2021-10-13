package io.github.warren1001.resetbot

import java.io.File
import java.util.regex.Pattern

class SwearFilter(private val auriel: Auriel) {
	
	private val swearFilterFile = File("swearFilterPatterns.txt")
	private val patterns = mutableSetOf<Filter>()
	
	init {
		
		if (swearFilterFile.exists()) {
			swearFilterFile.forEachLine {
				val args = it.split(",", limit = 2)
				patterns.add(Filter(Pattern.compile(args[0]), args[1]))
			}
		} else {
			swearFilterFile.createNewFile()
			
			// default patterns
			patterns.add(Filter(Pattern.compile("(?i)([f]+[u]+[c]+[k]+)"), "flip"))
			patterns.add(Filter(Pattern.compile("(?i)([s]+[h]+[i]+[t]+)"), "crap"))
			swearFilterFile.writeText(patterns.joinToString(System.lineSeparator()) { "${it.pattern.pattern()},${it.replacement}" })
			
		}
	
	}
	
	fun addSwearFilterPattern(pattern: String, replacement: String): Boolean {
		if (containsPattern(pattern)) return false
		if (patterns.isEmpty()) {
			swearFilterFile.writeText("$pattern,$replacement")
		} else {
			swearFilterFile.appendText("${System.lineSeparator()}$pattern,$replacement")
		}
		patterns.add(Filter(Pattern.compile(pattern), replacement))
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
		swearFilterFile.writeText(patterns.joinToString(System.lineSeparator()) { "${it.pattern.pattern()},${it.replacement}" })
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
		val stringBuilder = StringBuilder("The message below triggered the following patterns with the accompanying examples:")
		var flagged = false
		var foundWords = ""
		
		patterns.forEach {
			
			val matcher = it.pattern.matcher(content)
			
			if (matcher.find()) {
				flagged = true
				
				stringBuilder.append("\n||${it.pattern.pattern()}||: ")
				for (i in 1..matcher.groupCount()) {
					val swear = matcher.group(i)
					stringBuilder.append("||$swear||")
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