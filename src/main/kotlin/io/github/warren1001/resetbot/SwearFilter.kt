package io.github.warren1001.resetbot

import discord4j.rest.util.Permission
import java.io.File
import java.util.regex.Pattern

class SwearFilter(private val messageListener: MessageListener) {
	
	private val swearFilterFile = File("swearFilterPatterns.txt")
	private val patterns = mutableSetOf<Pattern>()
	
	init {
		
		if (swearFilterFile.exists()) {
			swearFilterFile.forEachLine { patterns.add(Pattern.compile(it)) }
		} else {
			swearFilterFile.createNewFile()
			
			// default patterns
			patterns.add(Pattern.compile("(?i)([f]+[u]+[c]+[k]+)"))
			patterns.add(Pattern.compile("(?i)([s]+[h]+[i]+[t]+)"))
			swearFilterFile.writeText(patterns.joinToString(System.lineSeparator()) { it.pattern() })
			
		}
	
	}
	
	fun addSwearFilterPattern(pattern: String): Boolean {
		if (containsPattern(pattern)) return false
		if (patterns.isEmpty()) {
			swearFilterFile.writeText(pattern)
		} else {
			swearFilterFile.appendText(System.lineSeparator() + pattern)
		}
		patterns.add(Pattern.compile(pattern))
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
		patterns.removeIf { it.pattern() == pattern }
		val list = swearFilterFile.readLines().toMutableList()
		list.remove(pattern)
		swearFilterFile.writeText(list.joinToString(separator = System.lineSeparator()))
		return true
	}
	
	fun containsPattern(pattern: String): Boolean {
		return patterns.any { it.pattern() == pattern }
	}
	
	fun getListOfPatterns() : String {
		return patterns.joinToString("\n") { it.pattern() }
	}
	
	fun checkMessage(message: ShallowMessage): Boolean {
		
		if (message.getAuthorPermissions().contains(Permission.MANAGE_MESSAGES)) return false
		
		val stringBuilder = StringBuilder("The message below triggered the following patterns with the accompanying examples:\n ")
		var flagged = false
		
		patterns.map { it.matcher(message.getMessage().content) }.filter { it.find() }.forEach {
			flagged = true
			
			stringBuilder.append("`${it.pattern().pattern()}`: ")
			for (i in 1..it.groupCount()) {
				stringBuilder.append("||${it.group(i)}||")
				if (i != it.groupCount()) stringBuilder.append(", ")
			}
			
		}
		
		if (!flagged) return false
		
		messageListener.delete(message, stringBuilder.toString())
		
		return true
	}

}