package io.github.warren1001.resetbot

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.rest.util.Permission
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.function.Consumer
import java.util.regex.Pattern

class TradeChannelMessageListener(private val auriel: Auriel, private val channelId: Snowflake, private var isBuy: Boolean) : Consumer<ShallowMessage> {
	
	companion object {
		
		private val blacklistWords = mutableListOf<String>()
		private val buyBlacklistFile = File("buyBlacklist.txt")
		var pattern: Pattern
		
		init {
			if (buyBlacklistFile.exists()) blacklistWords.addAll(buyBlacklistFile.readLines().map { it.lowercase() })
			else {
				buyBlacklistFile.createNewFile()
				blacklistWords.add("wts")
				blacklistWords.add("wtt")
				blacklistWords.add("selling")
				blacklistWords.add("sell")
				buyBlacklistFile.writeText(blacklistWords.joinToString(System.lineSeparator()))
			}
			pattern = Pattern.compile("(?im)(?:^|[ -=_*~:/\\\\])(${blacklistWords.joinToString("|")})(?:\$|[ -=_*~:/\\\\])")
		}
		
		fun addBlacklistWord(word: String): Boolean {
			val w = word.lowercase()
			if (blacklistWords.contains(w)) return false
			if (blacklistWords.isEmpty()) {
				buyBlacklistFile.writeText(w)
			} else {
				buyBlacklistFile.appendText(System.lineSeparator() + w)
			}
			blacklistWords.add(w)
			pattern = Pattern.compile("(?im)(?:^|[ -=_*~:/\\\\])(${blacklistWords.joinToString("|")})(?:\$|[ -=_*~:/\\\\])")
			return true
		}
		
		fun removeBlacklistWord(word: String): Boolean {
			val w = word.lowercase()
			if (blacklistWords.isEmpty() || !blacklistWords.contains(w)) return false
			blacklistWords.remove(w)
			buyBlacklistFile.writeText(blacklistWords.joinToString(System.lineSeparator()))
			pattern = Pattern.compile("(?im)(?:^|[ -=_*~:/\\\\])(${blacklistWords.joinToString("|")})(?:\$|[ -=_*~:/\\\\])")
			return true
		}
		
	}
	
	private val maximumLines = 45
	
	private val usersLastMessage = mutableMapOf<Snowflake, Snowflake>()
	private val predicate: (ShallowMessage) -> Boolean = { !it.getMessage().isPinned && !it.getAuthor().isBot && (it.getMessage().content.isNullOrEmpty() ||
			(!it.getAuthorPermissions().contains(Permission.ADMINISTRATOR) || it.getMessage().content[0] != '!')) &&
			!it.getAuthorPermissions().contains(Permission.MANAGE_MESSAGES) }
	
	init {
		val threeDaysAgo = Instant.now().minus(3, ChronoUnit.DAYS)
		auriel.getGateway().getChannelById(channelId).doOnError { auriel.getLogger().logError(it) }.filter { it is MessageChannel }.map { it as MessageChannel }.flatMapMany { it
			.getMessagesBefore(Snowflake.of(Instant.now())) }
			.map { ShallowMessage(auriel, it) }.filter(predicate).subscribe { msg ->
				if (threeDaysAgo.isAfter(msg.getMessage().timestamp) || usersLastMessage.containsKey(msg.getAuthor().id)) msg.delete()
				else usersLastMessage[msg.getAuthor().id] = msg.getMessage().id
			}
	}
	
	override fun accept(message: ShallowMessage) {
		
		val type = message.getMessage().data.type()
		if (type == 18 || type == 6) {
			message.delete()
			return
		}
		if (predicate.invoke(message)) {
			
			val blacklistMatcher = pattern.matcher(message.getMessage().content)
			if (isBuy && blacklistMatcher.find()) {
				auriel.getMessageListener().reply(message, "This is a buy only channel. Since your post contained the term '${blacklistMatcher.group(1)}'," +
						" it was deleted. Please make sure that posts in this channel are buy focused and not selling focused. " +
						"1 for 1 item trades are considered selling posts and should go in the respective selling channel. " +
						"You will have to wait the cooldown to post another message.", true, 25)
				return
			} else if (message.getMessage().content.split('\n').size > maximumLines) {
				auriel.getMessageListener().reply(message, "You can have at most $maximumLines lines in your post. You will have to wait the cooldown to post " +
						"another message with $maximumLines or less lines.", true,	20)
				return
			}
			
			if (usersLastMessage.containsKey(message.getAuthor().id) && usersLastMessage[message.getAuthor().id]!! != message.getMessage().id) {
				auriel.getGateway().getMessageById(channelId, usersLastMessage[message.getAuthor().id]!!).filter { !it.isPinned }.flatMap { it.delete() }.subscribe()
			}
			usersLastMessage[message.getAuthor().id] = message.getMessage().id
			
		}
		
	}
	
	fun setIsBuy(isBuy: Boolean): Boolean {
		if (this.isBuy != isBuy) {
			this.isBuy = isBuy;
			return true
		}
		return false
	}
	
	fun removeMessage(id: Snowflake) {
		usersLastMessage.entries.removeIf { it.value == id }
	}
	
}