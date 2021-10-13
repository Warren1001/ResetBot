package io.github.warren1001.resetbot

import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel

class ShallowMessage {
	
	private val auriel: Auriel
	
	val message: Message
	val channel: MessageChannel
	val author: User
	
	var deleted = false
	
	constructor(auriel: Auriel, message: Message, channel: MessageChannel) {
		this.auriel = auriel
		this.message = message
		this.channel = channel
		this.author = message.author.get()
	}
	
	fun delete() {
		if (!deleted) {
			deleted = true
			message.delete().doOnError { auriel.getLogger().logError(it) }.subscribe()
		}
	}
	
	fun delete(action: (ShallowMessage) -> Unit) {
		if (!deleted) {
			deleted = true
			message.delete().doOnError { auriel.getLogger().logError(it) }.doOnSuccess { action.invoke(this) }.subscribe()
		} else action.invoke(this)
	}
	
	fun isModerator(): Boolean {
		return auriel.getUserManager().isModerator(author.id)
	}
	
	fun isAdministrator(): Boolean {
		return auriel.getUserManager().isAdministrator(author.id)
	}
	
}