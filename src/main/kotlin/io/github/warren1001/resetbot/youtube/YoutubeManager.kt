package io.github.warren1001.resetbot.youtube

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeRequestInitializer
import com.google.gson.JsonObject
import discord4j.common.util.Snowflake
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.component.Button
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import discord4j.core.spec.MessageCreateSpec
import io.github.warren1001.resetbot.Auriel
import kotlin.concurrent.timer

class YoutubeManager(private val auriel: Auriel, key: String) {
	
	private val youtubeJsonObject: JsonObject = if (auriel.getJson().has("youtube") && auriel.getJson()["youtube"].isJsonObject) auriel.getJson()["youtube"].asJsonObject else JsonObject()
	private var youtubeLastUpdate: Long = if (youtubeJsonObject.has("last-update")) youtubeJsonObject["last-update"].asLong else 0L
	private var youtubeChannelId: Snowflake = Snowflake.of(if (youtubeJsonObject.has("channel")) youtubeJsonObject["channel"].asLong else 0L)
	private var youtubeChannel: MessageChannel? = null
	private var message: String = if (youtubeJsonObject.has("message")) youtubeJsonObject["message"].asString else "{title} {link}"
	private var roleId: Snowflake = Snowflake.of(if (youtubeJsonObject.has("role")) youtubeJsonObject["role"].asLong else 0L)
	
	private val buttonList = mutableListOf(Button.primary("youtube-role-give", "Give me the role."), Button.danger("youtube-role-remove", "I don't want the role anymore."))
	
	private val playlistItemsRequest: YouTube.PlaylistItems.List;
	private val playlistItemsRequestLimit: YouTube.PlaylistItems.List;
	
	private var early = false
	
	init {
		
		val youtube = YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance()) {}.setApplicationName("mrllamasc-video-checker")
			.setYouTubeRequestInitializer(YouTubeRequestInitializer(key)).build()
		
		playlistItemsRequest = youtube.PlaylistItems().list(mutableListOf("snippet"))
		playlistItemsRequest.playlistId = "UUZcV8AWZ_nQvx0Yvnmy7aHw"
		
		playlistItemsRequestLimit = youtube.PlaylistItems().list(mutableListOf("snippet"))
		playlistItemsRequestLimit.playlistId = "UUZcV8AWZ_nQvx0Yvnmy7aHw"
		playlistItemsRequestLimit.maxResults = 1
		
		if (youtubeChannelId.asLong() != 0L) {
			auriel.getGateway().getChannelById(youtubeChannelId).cast(MessageChannel::class.java).subscribe {
				youtubeChannel = it
				if (early) {
					checkForUpload()
					early = false
				}
			}
		}
		
		auriel.getGateway().on(ButtonInteractionEvent::class.java).onErrorContinue { it, _ -> auriel.getLogger().logError(it) }.filter { it.customId.startsWith("youtube-role-") }.flatMap {
			
			val member = it.interaction.member.get()
			
			if (it.customId.equals("youtube-role-give")) {
				
				if (member.roleIds.contains(roleId)) {
					return@flatMap it.reply("You already have the role to receive notifications for new YouTube videos.").withEphemeral(true)
				} else {
					member.addRole(roleId).doOnError { auriel.getLogger().logError(it) }.subscribe()
					return@flatMap it.reply("You will now receive notifications for new YouTube videos! They will be announced in ${youtubeChannel!!.mention}.").withEphemeral(true)
				}
				
			} else {
				
				if (member.roleIds.contains(roleId)) {
					member.removeRole(roleId).doOnError { auriel.getLogger().logError(it) }.subscribe()
					return@flatMap it.reply("You will no longer receive notifications for new YouTube videos.").withEphemeral(true)
				} else {
					return@flatMap it.reply("You do not have the role to receive notifications for new YouTube videos.").withEphemeral(true)
				}
				
			}
			
		}.subscribe()
		
		timer("youtubeUploadChecker", true, 0L, (1000 * 60 * 1).toLong()) { checkForUpload() }
		
	}
	
	fun checkForUpload() {
		if (youtubeChannel == null) {
			early = true
			return
		}
		val playlistItems = if (youtubeLastUpdate == 0L) playlistItemsRequestLimit.execute() else playlistItemsRequest.execute()
		playlistItems.items.filter { it.snippet.resourceId.kind == "youtube#video" && it.snippet.publishedAt.value > youtubeLastUpdate }
			.sortedWith(Comparator.comparingLong { it.snippet.publishedAt.value }).forEach {
				val videoId = it.snippet.resourceId.videoId
				val time = it.snippet.publishedAt.value
				val title = it.snippet.title
				updateLastUpdate(time)
				youtubeChannel!!.createMessage(
					message.replace("{title}", title).replace("{link}", "https://www.youtube.com/watch?v=$videoId")
						.replace("{url}", "https://www.youtube.com/watch?v=$videoId")
				).subscribe()
			}
		
	}
	
	fun updateLastUpdate(lastUpdate: Long) {
		if (lastUpdate < youtubeLastUpdate) {
			println("we got some whacko stuff")
			return
		}
		youtubeLastUpdate = lastUpdate
		youtubeJsonObject.addProperty("last-update", lastUpdate)
		auriel.getJson().add("youtube", youtubeJsonObject)
		auriel.saveJson()
	}
	
	fun setChannelId(id: Snowflake) {
		youtubeChannelId = id
		youtubeJsonObject.addProperty("channel", youtubeChannelId.asLong())
		auriel.getJson().add("youtube", youtubeJsonObject)
		auriel.saveJson()
		auriel.getGateway().getChannelById(youtubeChannelId).cast(MessageChannel::class.java).subscribe {
			youtubeChannel = it
			if (early) {
				checkForUpload()
				early = false
			}
		}
	}
	
	fun setChannel(channel: MessageChannel) {
		youtubeChannelId = channel.id
		youtubeJsonObject.addProperty("channel", youtubeChannelId.asLong())
		auriel.getJson().add("youtube", youtubeJsonObject)
		auriel.saveJson()
		youtubeChannel = channel
		if (early) {
			checkForUpload()
			early = false
		}
	}
	
	fun setMessage(message: String) {
		this.message = message
		youtubeJsonObject.addProperty("message", message)
		auriel.getJson().add("youtube", youtubeJsonObject)
		auriel.saveJson()
	}
	
	fun setRoleId(id: Snowflake) {
		roleId = id
		youtubeJsonObject.addProperty("role", id.asLong())
		auriel.getJson().add("youtube", youtubeJsonObject)
		auriel.saveJson()
	}
	
	fun sendRoleGiveMessage(channel: MessageChannel): Boolean {
		if (roleId.asLong() == 0L || youtubeChannel == null) return false
		channel.createMessage(MessageCreateSpec.builder().content("If you would like to receive a role for notifications for new YouTube videos here on Discord, " +
				"click the button labeled `Give me the role`.\nThe announcements for new YouTube videos will be in ${youtubeChannel!!.mention}.").components(ActionRow.of(buttonList)).build())
			.subscribe()
		return true
	}
	
}