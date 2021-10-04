package io.github.warren1001.resetbot

import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.rest.util.PermissionSet

class ShallowMessage(private val message: Message) {
	
	private val author = message.authorAsMember.block()!!
	private val authorPermissions = author.basePermissions.block()!!
	private val channel = message.channel.block()!!
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
			message.delete().doOnSuccess { action.invoke(this) }.subscribe()
		} else action.invoke(this)
	}
	
}