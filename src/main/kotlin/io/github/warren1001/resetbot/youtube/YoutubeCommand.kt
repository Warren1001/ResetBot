package io.github.warren1001.resetbot.youtube

import discord4j.common.util.Snowflake
import io.github.warren1001.resetbot.Auriel
import io.github.warren1001.resetbot.command.CommandContext

class YoutubeCommand(private val auriel: Auriel): (CommandContext) -> Boolean {
	
	override fun invoke(ctx: CommandContext): Boolean {
		
		val arguments = ctx.arguments
		if (arguments.isEmpty()) return false
		
		val args = arguments.split(' ', limit = 2)
		
		if (args.size == 1) {
			
			if (args[0].equals("channel", true)) {
				
				auriel.getYoutubeManager().setChannel(ctx.msg.channel)
				ctx.msg.reply("Set this channel to the Youtube Announcements channel!", true, 10L)
				return true
				
			} else if (args[0].equals("check", true)) {
				
				auriel.getYoutubeManager().checkForUpload()
				ctx.msg.reply("Checked for new uploads, check the announcement channel.", true, 10L)
				return true
				
			} else if (args[0].equals("rolemsg", true) || args[0].equals("rolemessage", true)) {
			
				auriel.getYoutubeManager().sendRoleGiveMessage(ctx.msg.channel)
				ctx.msg.reply("Sent a new YouTube role-giving message in this channel. Be sure to delete old messages anywhere else as needed.", true, 15L)
				return true
			
			}
			
		} else if (args.size == 2) {
			
			if (args[0].equals("message", true)) {
				
				auriel.getYoutubeManager().setMessage(args[1])
				ctx.msg.reply("Updated the Youtube announcement message.", true, 10L)
				return true
				
			} else if (args[0].equals("role", true)) {
				
				val roleId = Snowflake.of(args[1])
				auriel.getYoutubeManager().setRoleId(roleId)
				ctx.msg.reply("Set the Youtube announcements role id to '${args[1]}'.", true, 10L)
				return true
				
			}
			
		}
		
		return false
	}
	
}