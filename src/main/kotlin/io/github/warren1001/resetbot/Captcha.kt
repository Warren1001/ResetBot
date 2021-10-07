package io.github.warren1001.resetbot

class Captcha(private val message: String, private vararg val answers: String) {

	fun getMessage(): String {
		return message
	}
	
	fun matches(msg: ShallowMessage): Boolean {
		return answers.any { msg.getMessage().content.contains(it, ignoreCase = true) }
	}

}