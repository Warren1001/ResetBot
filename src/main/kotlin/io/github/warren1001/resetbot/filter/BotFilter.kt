package io.github.warren1001.resetbot.filter

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.MessageEditSpec
import io.github.warren1001.resetbot.Auriel
import java.time.Instant
import kotlin.concurrent.timer
import kotlin.random.Random

class BotFilter(private val auriel: Auriel) {
	
	private val captchas = mutableListOf<Captcha>()
	private var currentCaptcha: Captcha
	private var humanRoleId: Snowflake? = null
	private var humanChannelId: Snowflake? = null
	private var botMessageId: Snowflake? = null
	private var botMessage: Message? = null
	
	init {
		
		// just gonna hardcode these
		captchas.add(
			Captcha("__Which item was the last item MrLlama needed to complete his holy grail?__\n\n" +
						"Tyrael's Might\nDeath's Web\n***Mang Song's Lesson***\nTemplar's Might\nGriswold's Redemption", "mang", "song", "lesson")
		)
		captchas.add(
			Captcha("__What is MrLlama's first name?__\n\n" +
				"Steve\nLlama\nPete\nDavidson\n***Alex***", "alex", "pete", "davidson")
		)
		captchas.add(
			Captcha("__What is MrLlama's brand identity animal?__\n\n" +
				"Alpaca\n***Llama***\nCamel\nPete Davidson", "llama", "lamma", "lama", "camel", "pete", "davidson")
		)
		captchas.add(
			Captcha("__Does MrLlama have a YouTube voice?__\n\n" +
				"**Yes**\nNo", "yes")
		)
		currentCaptcha = captchas[Random.nextInt(captchas.size)]
		
		val role = auriel.getJson()["human.id.role"]
		if (role != null) humanRoleId = Snowflake.of(role.asLong)
		val channel = auriel.getJson()["human.id.channel"]
		if (channel != null) humanChannelId = Snowflake.of(channel.asLong)
		
		val message = auriel.getJson()["human.id.message"]
		if (message != null) {
			botMessageId = Snowflake.of(message.asLong)
			auriel.getGateway().getChannelById(humanChannelId!!).cast(MessageChannel::class.java).flatMap { it.getMessageById(botMessageId) }.subscribe { setBotMessage(it) }
		} else if (humanChannelId != null) setupBotMessage()
		
		if (humanRoleId != null && humanChannelId != null) checkPreviousMessages()
		
	}
	
	fun humanCheck(msg: Message): Boolean {
		if ((humanRoleId == null || humanChannelId == null)) {
			auriel.getLogger().getChannelLogger().logError("u fukin noob, setup the bot filter!!!!")
			return false
		}
		if (auriel.getUserManager().isAdministrator(msg.author.get().id)) return false
		if (msg.channelId == humanChannelId) {
			if (!msg.content.isNullOrEmpty() && currentCaptcha.matches(msg)) {
				msg.authorAsMember.doOnError { auriel.getLogger().logError(it) }.flatMap { it.addRole(humanRoleId!!) }.subscribe()
			}
			msg.delete().subscribe()
			return true
		}
		return false
	}
	
	fun setHumanRoleId(id: Snowflake) {
		humanRoleId = id
		if (humanChannelId != null) checkPreviousMessages()
		auriel.getJson().addProperty("human.id.role", id.asLong())
		auriel.saveJson()
	}
	
	fun setHumanChannelId(id: Snowflake) {
		humanChannelId = id
		setupBotMessage()
		if (humanRoleId != null) checkPreviousMessages()
		auriel.getJson().addProperty("human.id.channel", id.asLong())
		auriel.saveJson()
	}
	
	fun checkPreviousMessages() {
		
		auriel.getGateway().getChannelById(humanChannelId!!).doOnError { auriel.getLogger().logError(it) }.cast(MessageChannel::class.java)
			.flatMapMany { it.getMessagesBefore(Snowflake.of(Instant.now())) }.filter { it.author.isEmpty || !it.author.get().isBot }
			.subscribe {
				if (it.author.isEmpty) it.delete().subscribe()
				else humanCheck(it)
			}
	}
	
	fun setBotMessage(msg: Message) {
		botMessage = msg
		val interval = (1000 * 60 * 5).toLong()
		timer("randomizeCaptcha", true, interval, interval) { setRandomCaptcha() }
	}
	
	fun setRandomCaptcha() {
		if (botMessage == null) return
		var nextCaptcha = captchas[Random.nextInt(captchas.size)]
		while (nextCaptcha == currentCaptcha) {
			nextCaptcha = captchas[Random.nextInt(captchas.size)]
		}
		currentCaptcha = nextCaptcha
		val content = "__**ANSWER THE FOLLOWING CAPTCHA QUESTION TO GAIN ACCESS TO THE SERVER**__\n\n" +
				"${currentCaptcha.getMessage()}\n\n\n" +
				"If you know you are typing the right answer and you aren't given access to the server, check the channel list. " +
				"If you can see all the channels, just click any of those and you're good to go. If you can't see all the channels, restart Discord. " +
				"If you still can't see all the channels, message ${auriel.getWarrenMention()} or a mod.\nThe captcha will change every 5 minutes."
		botMessage!!.edit(MessageEditSpec.create().withContentOrNull(content)).subscribe()
	}
	
	fun setOfflineMessage() {
		if (botMessage == null) return
		botMessage!!.edit(MessageEditSpec.create().withContentOrNull("I am currently being restarted. Guess I wasn't good enough :( " +
				"Please wait up to a minute for me to return.")).subscribe()
	}
	
	fun setupBotMessage() {
		auriel.getGateway().getChannelById(humanChannelId!!).cast(MessageChannel::class.java).flatMap { it.createMessage("Setting up the captcha...") }.subscribe {
			setBotMessage(it)
			auriel.getJson().addProperty("human.id.message", it.id.asLong())
			auriel.saveJson()
		}
	}
	
}