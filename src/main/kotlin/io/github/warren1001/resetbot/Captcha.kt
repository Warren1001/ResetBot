package io.github.warren1001.resetbot

import discord4j.core.`object`.entity.Message

class Captcha(private val message: String, private vararg val answers: String) {

	fun getMessage(): String {
		return message
	}
	
	fun matches(msg: Message): Boolean {
		return answers.any { msg.content.contains(it, ignoreCase = true) }
	}

}