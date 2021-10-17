package io.github.warren1001.resetbot

import kotlin.random.Random

class RevealingMessage(private val word: String) {
	
	private val chosenIndexes = mutableSetOf<Int>()
	private val charArray = CharArray(word.length)
	
	init {
		charArray.fill('_')
	}
	
	fun reveal(): String {
		var index = Random.nextInt(word.length)
		while (chosenIndexes.contains(index)) {
			index = Random.nextInt(word.length)
		}
		chosenIndexes.add(index)
		charArray[index] = word[index]
		return charArray.concatToString()
	}
	
	fun wouldBeFullyRevealed(): Boolean {
		return chosenIndexes.size + 1 == charArray.size
	}
	
}