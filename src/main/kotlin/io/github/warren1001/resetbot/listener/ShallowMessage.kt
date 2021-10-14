package io.github.warren1001.resetbot.listener

import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import io.github.warren1001.resetbot.Auriel

class ShallowMessage(private val auriel: Auriel, val message: Message, val channel: MessageChannel) {
	
	val author: User = message.author.get()
	
	var deleted = false
	
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