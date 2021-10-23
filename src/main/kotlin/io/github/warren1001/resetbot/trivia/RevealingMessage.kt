package io.github.warren1001.resetbot.trivia

import kotlin.random.Random

class RevealingMessage(private val word: String) {
	
	private val chosenIndexes = mutableSetOf<Int>()
	private val charArray = CharArray(word.length)
	
	init {
		charArray.fill('_')
		word.forEachIndexed { index, c ->
			if (c == ' ' || c == '\'' || c == '-') {
				charArray[index] = word[index]
				chosenIndexes.add(index)
			}
		}
	}
	
	fun reveal(count: Int = 1): String {
		for (i in 1..count) revealRandomChar()
		return charArray.concatToString()
	}
	
	fun wouldBeFullyRevealed(count: Int = 1): Boolean {
		return chosenIndexes.size + count >= charArray.size
	}
	
	private fun revealRandomChar() {
		var index = Random.nextInt(word.length)
		while (chosenIndexes.contains(index)) {
			index = Random.nextInt(word.length)
		}
		chosenIndexes.add(index)
		charArray[index] = word[index]
	}
	
}