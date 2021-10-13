package io.github.warren1001.resetbot

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.MessageEditSpec
import java.io.File
import java.time.Instant
import kotlin.concurrent.timer
import kotlin.random.Random

class BotFilter(private val auriel: Auriel) {
	
	private val captchas = mutableListOf<Captcha>()
	private var currentCaptcha: Captcha
	private val humanRoleIdFile = File("humanRoleId.txt")
	private var humanRoleId: Snowflake? = null
	private val humanChannelIdFile = File("humanChannelId.txt")
	private var humanChannelId: Snowflake? = null
	private val botMessageFile = File("botFilterMessageId.txt")
	private var botMessageId: Snowflake? = null
	private var botMessage: ShallowMessage? = null
	
	init {
		
		// just gonna hardcode these
		captchas.add(Captcha("__Which item was the last item MrLlama needed to complete his holy grail?__\n\n" +
						"Tyrael's Might\nDeath's Web\n***Mang Song's Lesson***\nTemplar's Might\nGriswold's Redemption", "mang", "song", "lesson"))
		captchas.add(Captcha("__What is MrLlama's first name?__\n\n" +
				"Steve\nLlama\nPete\nDavidson\n***Alex***", "alex", "pete", "davidson"))
		captchas.add(Captcha("__What is MrLlama's brand identity animal?__\n\n" +
				"Alpaca\n***Llama***\nCamel\nPete Davidson", "llama", "lamma", "lama", "camel", "pete", "davidson"))
		captchas.add(Captcha("__Does MrLlama have a YouTube voice?__\n\n" +
				"**Yes**\nNo", "yes"))
		currentCaptcha = captchas[Random.nextInt(captchas.size)]
		
		if (humanRoleIdFile.exists()) {
			humanRoleId = Snowflake.of(humanRoleIdFile.readLines()[0])
		}
		if (humanChannelIdFile.exists()) {
			humanChannelId = Snowflake.of(humanChannelIdFile.readLines()[0])
			humanChannel = auriel.getGateway().getChannelById(humanChannelId!!).doOnError { auriel.getLogger().logError(it) }.block() as MessageChannel
		}
		
		if (botMessageFile.exists()) {
			botMessageId = Snowflake.of(botMessageFile.readLines()[0])
			if (humanChannel != null) setBotMessage(ShallowMessage(auriel, humanChannel!!.getMessageById(botMessageId).block()!!))
		} else if (humanChannel != null) setupBotMessage()
		
		if (humanRoleId != null && humanChannelId != null) {
			checkPreviousMessages()
		}
		
	}
	
	fun humanCheck(msg: ShallowMessage): Boolean {
		if ((humanRoleId == null || humanChannelId == null)) {
			auriel.getLogger().getChannelLogger().logError("u fukin noob, setup the bot filter!!!!")
			return false
		}
		if (/*msg.getAuthorPermissions().contains(Permission.ADMINISTRATOR)*/msg.author.id == Snowflake.of(164118147073310721)) return false
		if (msg.getChannel().id == humanChannelId) {
			if (!msg.getMessage().content.isNullOrEmpty() && currentCaptcha.matches(msg)) {
				msg.getMessage().authorAsMember.doOnError { auriel.getLogger().logError(it) }.flatMap { it.addRole(humanRoleId!!) }.subscribe()
			}
			msg.delete()
			return true
		}
		return false
	}
	
	fun setHumanRoleId(id: Snowflake) {
		humanRoleId = id
		if (humanChannelId != null) checkPreviousMessages()
		humanRoleIdFile.writeText(id.asString())
	}
	
	fun setHumanChannelId(id: Snowflake) {
		humanChannelId = id
		humanChannel = auriel.getGateway().getChannelById(humanChannelId!!).doOnError { auriel.getLogger().logError(it) }.block() as MessageChannel
		if (humanRoleId != null) checkPreviousMessages()
		setupBotMessage()
		humanChannelIdFile.writeText(id.asString())
	}
	
	fun checkPreviousMessages() {
		humanChannel!!.getMessagesBefore(Snowflake.of(Instant.now())).doOnError { auriel.getLogger().logError(it) }.map { ShallowMessage(auriel, it) }.filter { !it.author.isBot }
			.subscribe {
				if (it.getMessage().author.isEmpty) it.delete()
				else humanCheck(it)
			}
	}
	
	fun setBotMessage(msg: ShallowMessage) {
		botMessage = msg
		timer("randomizeCaptcha", true, 0, (1000 * 60 * 5).toLong()) { setRandomCaptcha() }
	}
	
	fun setRandomCaptcha() {
		if (botMessage == null) return
		var nextCaptcha = captchas[Random.nextInt(captchas.size)]
		while (nextCaptcha == currentCaptcha) {
			nextCaptcha = captchas[Random.nextInt(captchas.size)]
		}
		currentCaptcha = nextCaptcha
		val content = "__**ANSWER THE FOLLOWING QUESTION TO GAIN ACCESS TO THE SERVER**__\n\n" +
				"${currentCaptcha.getMessage()}\n\n\n" +
				"If you know you are typing the right answer and you aren't given access to the server, check the channel list. " +
				"If you can see all the channels, just click any of those and you're good to go. If you can't see all the channels, restart Discord. " +
				"If you still can't see all the channels, message ${auriel.getWarrenMention()} or a mod.\nThe captcha will change every 5 minutes."
		botMessage!!.getMessage().edit(MessageEditSpec.create().withContentOrNull(content)).subscribe()
	}
	
	fun setOfflineMessage() {
		if (botMessage == null) return
		botMessage!!.getMessage().edit(MessageEditSpec.create().withContentOrNull("I am currently being restarted. Guess I wasn't good enough :( " +
				"Please wait up to a minute for me to return.")).subscribe()
	}
	
	fun setupBotMessage() {
		setBotMessage(ShallowMessage(auriel, humanChannel!!.createMessage("Setting up the captcha...").block()!!))
		botMessageFile.writeText(botMessage!!.getMessage().id.asString())
	}
	
}