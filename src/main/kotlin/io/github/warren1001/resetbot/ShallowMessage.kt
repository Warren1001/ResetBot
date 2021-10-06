package io.github.warren1001.resetbot

import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.rest.util.PermissionSet

class ShallowMessage(private val auriel: Auriel, private val message: Message) {
	
	private val author = message.authorAsMember.doOnError {
		auriel.getLogger().logError(it)
		message.author.ifPresentOrElse({
			auriel.getLogger().getChannelLogger().logError("Good news... the author exists, their name is ${it.username}, go see if they exist in the server")
		}, {
			auriel.getLogger().getChannelLogger().logError("You were hoping for good news but nope. the author doesnt exist")
		})
	}.block()!!
	private val authorPermissions = author.basePermissions.doOnError { auriel.getLogger().logError(it) }.block()!!
	private val channel = message.channel.doOnError { auriel.getLogger().logError(it) }.block()!!
	private var deleted = false
	
	fun getAuthor(): Member {
		return author
	}
	
	fun getAuthorPermissions(): PermissionSet {
		return authorPermissions
	}
	
	fun getMessage(): Message {
		return message
	}
	
	fun getChannel(): MessageChannel {
		return channel
	}
	
	fun delete() {
		message.delete().subscribe()
	}
	
	fun delete(action: (ShallowMessage) -> Unit) {
		if (!deleted) {
			deleted = true
			message.delete().doOnError { auriel.getLogger().logError(it) }.doOnSuccess { action.invoke(this) }.subscribe()
		} else action.invoke(this)
	}
	
}