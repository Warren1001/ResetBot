package io.github.warren1001.resetbot.listener

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.MessageChannel
import io.github.warren1001.resetbot.Auriel
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.function.Consumer
import java.util.regex.Pattern
import kotlin.concurrent.timer

class TradeChannelMessageListener(private val auriel: Auriel, private val channelId: Snowflake, private var isBuy: Boolean) : Consumer<ShallowMessage> {
	
	companion object {
		
		val buyBlacklist = mutableListOf<String>()
		val sellBlacklist = mutableListOf<String>()
		
		var buyPattern: Pattern? = null
		var sellPattern: Pattern? = null
		
		var maximumLines: Int = 45
		
		fun addBuyBlacklist(word: String): Boolean {
			val w = word.lowercase()
			if (buyBlacklist.contains(w)) return false
			buyBlacklist.add(w)
			buyPattern = Pattern.compile("(?im)(?:^|[ -=_*~:/\\\\])(${buyBlacklist.joinToString("|")})(?:\$|[ -=_*~:/\\\\])")
			return true
		}
		
		fun removeBuyBlacklist(word: String): Boolean {
			val w = word.lowercase()
			if (buyBlacklist.isEmpty() || !buyBlacklist.contains(w)) return false
			buyBlacklist.remove(w)
			buyPattern = Pattern.compile("(?im)(?:^|[ -=_*~:/\\\\])(${buyBlacklist.joinToString("|")})(?:\$|[ -=_*~:/\\\\])")
			return true
		}
		
		fun addSellBlacklist(word: String): Boolean {
			val w = word.lowercase()
			if (sellBlacklist.contains(w)) return false
			sellBlacklist.add(w)
			sellPattern = Pattern.compile("(?im)(?:^|[ -=_*~:/\\\\])(${sellBlacklist.joinToString("|")})(?:\$|[ -=_*~:/\\\\])")
			return true
		}
		
		fun removeSellBlacklist(word: String): Boolean {
			val w = word.lowercase()
			if (sellBlacklist.isEmpty() || !sellBlacklist.contains(w)) return false
			sellBlacklist.remove(w)
			sellPattern = Pattern.compile("(?im)(?:^|[ -=_*~:/\\\\])(${sellBlacklist.joinToString("|")})(?:\$|[ -=_*~:/\\\\])")
			return true
		}
		
	}
	
	private val usersLastMessage = mutableMapOf<Snowflake, Snowflake>()
	private val predicate: (ShallowMessage) -> Boolean = {
		!it.message.isPinned && !it.author.isBot && (it.message.content.isNullOrEmpty() ||
				(!it.isAdministrator() || it.message.content[0] != '!')) && !it.isModerator() // TODO handle commands in the trade channels the right way
	}
	
	init {
		val daysAgo = Instant.now().minus(2, ChronoUnit.DAYS)
		auriel.getGateway().getChannelById(channelId).doOnError { auriel.getLogger().logError(it) }.cast(MessageChannel::class.java)
			.flatMapMany { it.getMessagesBefore(Snowflake.of(Instant.now())) }.onErrorContinue { error, _ -> auriel.getLogger().logError(error) }.filter { !it.isPinned }
			.subscribe { msg ->
				if (msg.content.isNullOrEmpty()) msg.delete().subscribe()
				else {
					if (msg.author.isPresent) {
						val author = msg.author.get()
						if (msg.timestamp.isBefore(daysAgo)) msg.delete().subscribe()
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
			
			if (isBuy && buyPattern != null) {
				val blacklistMatcher = buyPattern!!.matcher(message.message.content)
				if (blacklistMatcher.find()) {
					message.reply("I am sending you a private message, please check it for why your post was deleted.", true, 15)
					var content = "This is a buy only channel. Since your post contained the term '${blacklistMatcher.group(1)}'," +
							" it was deleted. Please make sure that posts in this channel are buy focused and not selling focused. " +
							"1 for 1 item trades are considered selling posts and should go in the respective selling channel. " +
							"You will have to wait the cooldown to post another message. \n```\n${message.message.content.replace("`", "\\`")}\n```"
					if (content.length > 2000) content = content.substring(0, 2000)
					message.author.privateChannel.flatMap { it.createMessage(content) }.subscribe()
					return
				}
			} else if (!isBuy && sellPattern != null) {
				val blacklistMatcher = sellPattern!!.matcher(message.message.content)
				if (blacklistMatcher.find()) {
					message.reply("I am sending you a private message, please check it for why your post was deleted.", true, 15)
					var content = "This is a sell only channel. Since your post contained the term '${blacklistMatcher.group(1)}'," +
							" it was deleted. \n```\n${message.message.content.replace("`", "\\`")}\n```"
					if (content.length > 2000) content = content.substring(0, 2000)
					message.author.privateChannel.flatMap { it.createMessage(content) }.subscribe()
					return
				}
			} else if (message.message.content.split('\n').size > maximumLines) {
				message.reply("I am sending you a private message, please check it for why your post was deleted.", true, 15)
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