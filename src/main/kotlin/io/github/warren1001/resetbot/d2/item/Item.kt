package io.github.warren1001.resetbot.d2.item

import io.github.warren1001.resetbot.trivia.TriviaQuestion
import java.util.*

class Item(val name: String, val type: String, val base: String, val isOnly: Boolean = true, vararg aliases: String) {
	
	companion object {
		
		const val SET: String = "SET"
		const val UNIQUE: String = "UNIQUE"
		
		private val nameLookup = mutableMapOf<String, Item>()
		
		fun getItem(name: String): Optional<Item> = Optional.ofNullable(nameLookup[format(name)])
		
		private fun format(string: String): String = string.lowercase().replace("'", "").replace("-", "")
		
		val TAL_RASHAS_GUARDIANSHIP = Item("Tal Rasha's Guardianship", SET, "Lacquered Plate", true, "tals chest", "tals armor", "tal chest", "tal armor", "tal rashas chest", "tal rashas armor")
	
	}
	
	init {
		nameLookup[format(name)] = this
		aliases.forEach { nameLookup[format(it)] = this }
	}
	
	fun getTriviaQuestions(): Set<TriviaQuestion> {
		val questions = mutableSetOf<TriviaQuestion>()
		questions.add(TriviaQuestion("What base item is $name?", base))
		if (isOnly) questions.add(TriviaQuestion("What set item has the base item $base?", name))
		return questions
	}
	
}