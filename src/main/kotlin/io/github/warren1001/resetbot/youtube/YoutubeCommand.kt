package io.github.warren1001.resetbot.youtube

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
				
			}
			
		} else if (args.size == 2) {
			
			if (args[0].equals("message", true)) {
				
				auriel.getYoutubeManager().setMessage(args[1])
				ctx.msg.reply("Updated the Youtube announcement message.", true, 10L)
				return true
				
			}
			
		}
		
		return false
	}
	
}