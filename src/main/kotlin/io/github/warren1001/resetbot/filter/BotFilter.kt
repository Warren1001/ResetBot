package io.github.warren1001.resetbot.filter

import com.google.gson.JsonObject
import discord4j.common.util.Snowflake
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.component.Button
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import discord4j.core.spec.MessageEditSpec
import io.github.warren1001.resetbot.Auriel
import kotlin.concurrent.timer

class BotFilter(private val auriel: Auriel) {
	
	//private val captchas = mutableListOf<Captcha>()
	private val humanJsonObject = if (auriel.getJson().has("human") && auriel.getJson()["human"].isJsonObject) auriel.getJson()["human"].asJsonObject else JsonObject()
	private val idJsonObject = if (humanJsonObject.has("id") && humanJsonObject["id"].isJsonObject) humanJsonObject["id"].asJsonObject else JsonObject()
	private val msgJsonObject = if (humanJsonObject.has("messages") && humanJsonObject["messages"].isJsonObject) humanJsonObject["messages"].asJsonObject else JsonObject()
	//private var currentCaptcha: Captcha
	private var humanRoleId: Snowflake? = null
	private var humanChannelId: Snowflake? = null
	private var botMessageId: Snowflake? = null
	private var botMessage: Message? = null
	//private var setEarly = false
	
	private val buttonList = mutableListOf(Button.primary("captcha-correct", "CLICK ME"), Button.danger("captcha-wrong1", "NO"),
		Button.danger("captcha-wrong2", "NO"), Button.danger("captcha-wrong3", "NO"), Button.danger("captcha-wrong4", "NO"))
	
	private var successMsg: String = if (msgJsonObject.has("success")) msgJsonObject["success"].asString else "You are now in the server!"
	private var alreadyInMsg: String = if (msgJsonObject.has("already-in")) msgJsonObject["already-in"].asString else "You are already in the server!"
	
	init {
		auriel.getGateway().on(ButtonInteractionEvent::class.java).onErrorContinue { it, _ -> auriel.getLogger().logError(it) }.filter { it.customId.startsWith("captcha-") }.flatMap {
			
			val member = it.interaction.member.get()
			
			if (!member.roleIds.contains(humanRoleId!!)) {
				
				if (it.customId.equals("captcha-correct")) {
					member.addRole(humanRoleId!!).doOnError { auriel.getLogger().logError(it) }.subscribe()
					return@flatMap it.reply(successMsg).withEphemeral(true)
				} else {
					return@flatMap it.reply("Wrong button.").withEphemeral(true)
				}
				
			}
			
			return@flatMap it.reply(alreadyInMsg).withEphemeral(true)
			
		}.subscribe()
		
		// just gonna hardcode these
		/*captchas.add(
			Captcha(
				"__Which item was the last item MrLlama needed to complete his holy grail?__\n\n" +
						"Tyrael's Might\nDeath's Web\n***Mang Song's Lesson***\nTemplar's Might\nGriswold's Redemption", "mang", "song", "lesson"
			)
		)
		captchas.add(
			Captcha(
				"__What is MrLlama's first name?__\n\n" +
						"Steve\nLlama\nPete\nDavidson\n***Alex***", "alex", "pete", "davidson"
			)
		)
		captchas.add(
			Captcha(
				"__What is MrLlama's brand identity animal?__\n\n" +
						"Alpaca\n***Llama***\nCamel\nPete Davidson", "llama", "lamma", "lama", "camel", "pete", "davidson"
			)
		)
		captchas.add(
			Captcha(
				"__Does MrLlama have a YouTube voice?__\n\n" +
						"**Yes**\nNo", "yes"
			)
		)
		currentCaptcha = captchas[Random.nextInt(captchas.size)]*/
		
		if (idJsonObject.has("role")) {
			humanRoleId = Snowflake.of(idJsonObject["role"].asLong)
		}
		if (idJsonObject.has("channel")) {
			humanChannelId = Snowflake.of(idJsonObject["channel"].asLong)
		}
		if (idJsonObject.has("message")) {
			botMessageId = Snowflake.of(idJsonObject["message"].asLong)
			auriel.getGateway().getChannelById(humanChannelId!!).cast(MessageChannel::class.java).flatMap { it.getMessageById(botMessageId) }.doOnError { setupBotMessage() }
				.subscribe { trySetFirstMessage(it) }
		} else if (humanChannelId != null) setupBotMessage()
		
		//if (humanRoleId != null && humanChannelId != null) checkPreviousMessages()
		
	}
	
	fun setSuccessMessage(message: String) {
		successMsg = message
		msgJsonObject.addProperty("success", message)
		humanJsonObject.add("messages", msgJsonObject)
		auriel.getJson().add("human", humanJsonObject)
		auriel.saveJson()
	}
	
	fun setAlreadyInMessage(message: String) {
		alreadyInMsg = message
		msgJsonObject.addProperty("already-in", alreadyInMsg)
		humanJsonObject.add("messages", msgJsonObject)
		auriel.getJson().add("human", humanJsonObject)
		auriel.saveJson()
	}
	
	/*fun humanCheck(msg: Message): Boolean {
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
	}*/
	
	fun setHumanRoleId(id: Snowflake) {
		humanRoleId = id
		if (humanChannelId != null) {
			//checkPreviousMessages()
			setRandomCaptcha()
		}
		idJsonObject.addProperty("role", id.asLong())
		humanJsonObject.add("id", idJsonObject)
		auriel.getJson().add("human", humanJsonObject)
		auriel.saveJson()
	}
	
	fun setHumanChannelId(id: Snowflake) {
		humanChannelId = id
		setupBotMessage()
		//if (humanRoleId != null) checkPreviousMessages()
		idJsonObject.addProperty("channel", id.asLong())
		humanJsonObject.add("id", idJsonObject)
		auriel.getJson().add("human", humanJsonObject)
		auriel.saveJson()
	}
	
	/*fun checkPreviousMessages() {
		auriel.getGateway().getChannelById(humanChannelId!!).doOnError { auriel.getLogger().logError(it) }.cast(MessageChannel::class.java)
			.flatMapMany { it.getMessagesBefore(Snowflake.of(Instant.now())) }.filter { it.author.isEmpty || !it.author.get().isBot }
			.subscribe {
				if (it.author.isEmpty) it.delete().subscribe()
				else humanCheck(it)
			}
	}*/
	
	fun setBotMessage(msg: Message) {
		botMessage = msg
		val interval = (1000 * 30).toLong()
		timer("randomizeCaptcha", true, interval, interval) { setRandomCaptcha() }
	}
	
	fun isBotMessage(id: Snowflake): Boolean {
		return id == botMessageId
	}
	
	fun setRandomCaptcha() {
		if (botMessage == null/* || !auriel.hasWarrenMentionInit()*/) {
			//setEarly = true
			return
		}
		//setEarly = false
		randomizeButtons()
		// can get what i deleted from github
	}
	
	fun randomizeButtons() {
		buttonList.shuffle()
		botMessage!!.edit(MessageEditSpec.create().withComponents(ActionRow.of(buttonList))
			.withContentOrNull("Sometimes the Discord client doesn't update properly when you click the button. If this is the case for you, restart your Discord then look for the new channels."))
			.subscribe()
	}
	
	fun setOfflineMessage() {
		if (botMessage == null) return
		botMessage!!.edit(
			MessageEditSpec.create().withContentOrNull(
				"I am currently being restarted. Guess I wasn't good enough :( " +
						"Please wait up to a minute for me to return."
			).withComponentsOrNull(mutableListOf())
		).subscribe()
	}
	
	fun setupBotMessage() {
		auriel.getGateway().getChannelById(humanChannelId!!).cast(MessageChannel::class.java).flatMap { it.createMessage("Setting up the captcha...") }.subscribe {
			if (humanRoleId != null) trySetFirstMessage(it, true)
			idJsonObject.addProperty("message", it.id.asLong())
			humanJsonObject.add("id", idJsonObject)
			auriel.getJson().add("human", humanJsonObject)
			auriel.saveJson()
		}
	}
	
	fun trySetFirstMessage(msg: Message, notEarly: Boolean = false) {
		setBotMessage(msg)
		if (notEarly/* || setEarly*/) setRandomCaptcha()
	}
	
}