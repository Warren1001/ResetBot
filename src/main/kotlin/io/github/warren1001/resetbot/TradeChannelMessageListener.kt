package io.github.warren1001.resetbot

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.MessageChannel
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.function.Consumer
import java.util.regex.Pattern
import kotlin.concurrent.timer

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
	private val predicate: (ShallowMessage) -> Boolean = {
		!it.message.isPinned && !it.author.isBot && (it.message.content.isNullOrEmpty() ||
				(!it.isAdministrator() || it.message.content[0] != '!')) && !it.isModerator()
	}
	
	init {
		val daysAgo = Instant.now().minus(2, ChronoUnit.DAYS)
		val channel = auriel.getGateway().getChannelById(channelId).doOnError { auriel.getLogger().logError(it) }.cast(MessageChannel::class.java)
			.flatMapMany { it.getMessagesBefore(Snowflake.of(Instant.now())) }.onErrorContinue { error, _ -> auriel.getLogger().logError(error) }.filter { !it.isPinned }
			.subscribe { msg ->
				if (msg.content.isNullOrEmpty()) msg.delete().subscribe()
				else {
					if (msg.author.isPresent) {
						val author = msg.author.get()
						if (author.isBot || msg.timestamp.isBefore(daysAgo)) msg.delete().subscribe()
						else {
							if (usersLastMessage.containsKey(author.id)) msg.delete().subscribe()
							else usersLastMessage[author.id] = msg.id
						}
					} else msg.delete().subscribe()
				}
				
			}
		timer("checkOldPosts", true, 0, (1000 * 60 * 20).toLong()) {
			auriel.getGateway().getChannelById(channelId).doOnError { auriel.getLogger().logError(it) }.cast(MessageChannel::class.java)
				.flatMapMany { it.getMessagesBefore(Snowflake.of(Instant.now().minus(2, ChronoUnit.DAYS))) }.onErrorContinue { error, _ -> auriel.getLogger().logError(error) }
				.filter { !it.isPinned }.flatMap { it.delete() }.subscribe()
		}
	}
	
	override fun accept(message: ShallowMessage) {
		
		if (auriel.getMessageListener().getSwearFilter().checkMessage(message, false, 15)) return
		
		val type = message.message.data.type()
		if (type == 18 || type == 6) {
			message.delete()
			return
		}
		if (predicate.invoke(message)) {
			
			val blacklistMatcher = pattern.matcher(message.message.content)
			if (isBuy && blacklistMatcher.find()) {
				auriel.getMessageListener().reply(message, "I am sending you a private message, please check it for why your post was deleted.", true, 15)
				var content = "This is a buy only channel. Since your post contained the term '${blacklistMatcher.group(1)}'," +
						" it was deleted. Please make sure that posts in this channel are buy focused and not selling focused. " +
						"1 for 1 item trades are considered selling posts and should go in the respective selling channel. " +
						"You will have to wait the cooldown to post another message. \n```\n${message.message.content.replace("`", "\\`")}\n```"
				if (content.length > 2000) content = content.substring(0, 2000)
				message.author.privateChannel.flatMap { it.createMessage(content) }.subscribe()
				return
			} else if (message.message.content.split('\n').size > maximumLines) {
				auriel.getMessageListener().reply(message, "I am sending you a private message, please check it for why your post was deleted.", true, 15)
				var content = "You can have at most $maximumLines lines in your post. " +
						"You will have to wait the cooldown to post another message with $maximumLines or less lines.\n```\n${message.message.content.replace("`", "\\`")}\n```"
				if (content.length > 2000) content = content.substring(0, 2000)
				message.author.privateChannel.flatMap { it.createMessage(content) }.subscribe()
				return
			}
			
			if (usersLastMessage.containsKey(message.author.id) && usersLastMessage[message.author.id]!! != message.message.id) {
				auriel.getGateway().getMessageById(channelId, usersLastMessage[message.author.id]!!).filter { !it.isPinned }.flatMap { it.delete() }.subscribe()
			}
			usersLastMessage[message.author.id] = message.message.id
			
		}
		
	}
	
	fun setIsBuy(isBuy: Boolean): Boolean {
		if (this.isBuy != isBuy) {
			this.isBuy = isBuy
			return true
		}
		return false
	}
	
	fun removeMessage(id: Snowflake) {
		usersLastMessage.entries.removeIf { it.value == id }
	}
	
}