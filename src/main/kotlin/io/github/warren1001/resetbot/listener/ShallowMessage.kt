package io.github.warren1001.resetbot.listener

import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.MessageCreateSpec
import io.github.warren1001.resetbot.Auriel
import java.time.Duration

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
	
	fun delete(reason: String) {
		delete { auriel.getLogger().logDelete(it, reason) }
	}
	
	fun isModerator(): Boolean {
		return auriel.getUserManager().isModerator(author.id)
	}
	
	fun isAdministrator(): Boolean {
		return auriel.getUserManager().isAdministrator(author.id)
	}
	
	fun replyDeleted(msg: String, duration: Long = -1L) {
		
		var content = "${author.mention}, $msg"
		if (content.length > 2000) content = content.substring(0, 2000)
		
		val specBuilder = MessageCreateSpec.builder().content(content)
		
		channel.createMessage(specBuilder.build()).subscribe {
			if (duration != -1L) it.delete().delaySubscription(Duration.ofSeconds(duration)).subscribe()
		}
		
	}
	
	fun reply(msg: String, delete: Boolean = false, duration: Long = -1L) {
		
		val specBuilder = MessageCreateSpec.builder()
		var content = msg
		
		if (delete) {
			
			content = "${author.mention}, $msg"
			deleted = true
			message.delete().subscribe()
			
		} else specBuilder.messageReference(message.id)
		
		if (content.length > 2000) content = content.substring(0, 2000)
		
		specBuilder.content(content)
		channel.createMessage(specBuilder.build()).doOnError { auriel.getLogger().logError(it) }.subscribe {
			if (duration != -1L) it.delete().doOnError { auriel.getLogger().logError(it) }.delaySubscription(Duration.ofSeconds(duration)).subscribe()
		}
		
	}
	
}