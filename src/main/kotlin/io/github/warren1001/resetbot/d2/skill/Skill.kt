package io.github.warren1001.resetbot.d2.skill

import io.github.warren1001.resetbot.trivia.TriviaQuestion
import java.util.*

class Skill(val name: String, val owner: String, val level: Int, vararg aliases: String) {
	
	companion object {
		
		private val nameLookup = mutableMapOf<String, Skill>()
		
		fun getSkill(name: String): Optional<Skill> = Optional.ofNullable(nameLookup[format(name)])
		
		private fun format(string: String): String = string.lowercase().replace("'", "").replace("-", "")
		
		val MAGIC_ARROW = Skill("Magic Arrow", "Amazon", 1)
		
	}
	
	init {
		nameLookup[format(name)] = this
		aliases.forEach { nameLookup[format(it)] = this }
	}
	
	fun getTriviaQuestions(): Set<TriviaQuestion> {
		val questions = mutableSetOf<TriviaQuestion>()
		questions.add(TriviaQuestion("What is the level requirement of the skill $name?", level.toString()))
		questions.add(TriviaQuestion("Which character has the skill $name?", owner))
		return questions
	}
	
}