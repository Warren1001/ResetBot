package io.github.warren1001.resetbot

import java.util.regex.Pattern

class Filter(val pattern: Pattern, val replacement: String) {
	
	override fun equals(other: Any?): Boolean {
		return other is Filter && other.pattern == pattern
	}
	
	override fun hashCode(): Int {
		var result = pattern.hashCode()
		result = 31 * result + replacement.hashCode()
		return result
	}
	
}