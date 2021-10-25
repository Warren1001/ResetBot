package io.github.warren1001.resetbot.filter

import discord4j.common.util.Snowflake
import io.github.warren1001.resetbot.Auriel
import io.github.warren1001.resetbot.command.CommandContext

class FilterCommand(private val auriel: Auriel): (CommandContext) -> Boolean {
	
	override fun invoke(ctx: CommandContext): Boolean {
		
		val arguments = ctx.arguments
		if (arguments.isEmpty() || !arguments.contains(' ')) return false
		
		val extendedArgs = arguments.split(' ', limit = 2)
		
		if (extendedArgs.size == 2) {
			
			if (extendedArgs[0].equals("message", true)) {
				
				val msg = extendedArgs[1]
				auriel.getMessageListener().getSwearFilter().setCensoredMessage(msg)
				ctx.msg.reply("Set the basic repost censored message to '$msg'.", true, 10L)
				return true
				
			}
			
		}
		
		val args = arguments.split(' ')
		
		if (args.size == 4) {
			
			if (args[0].equals("pattern", true)) {
				
				if (args[1].equals("add", true)) {
					
					val pattern = args[2]
					val replacement = args[3]
					
					if (auriel.getMessageListener().getSwearFilter().addSwearFilterPattern(pattern, replacement)) {
						ctx.msg.reply("Added '$pattern' pattern with '$replacement' replacement to the swear filters list.")
					} else {
						ctx.msg.reply("'$pattern' pattern is already on the swear filters list.")
					}
					
					return true
					
				}
				
			} else if (args[0].equals("word", true)) {
				
				if (args[1].equals("add", true)) {
					
					val pattern = auriel.getMessageListener().getSwearFilter().constructBasicPattern(args[2])
					val replacement = args[3]
					
					if (auriel.getMessageListener().getSwearFilter().addSwearFilterPattern(pattern, replacement)) {
						ctx.msg.reply("Added '$pattern' pattern with '$replacement' replacement to the swear filters list.")
					} else {
						ctx.msg.reply("'$pattern' pattern is already on the swear filters list.")
					}
					
					return true
					
				}
				
			}
			
		} else if (args.size == 3) {
			
			if (args[0].equals("pattern", true)) {
				
				if (args[1].equals("remove", true)) {
					
					val pattern = args[2]
					
					if (auriel.getMessageListener().getSwearFilter().removeSwearFilterPattern(pattern)) {
						ctx.msg.reply("Removed '$pattern' pattern from the swear filters list.")
					} else {
						ctx.msg.reply("'$pattern' pattern is not on the swear filters list.")
					}
					
					return true
				}
				
			} else if (args[0].equals("word", true)) {
				
				if (args[1].equals("remove", true)) {
					
					val pattern = auriel.getMessageListener().getSwearFilter().constructBasicPattern(args[2])
					
					if (auriel.getMessageListener().getSwearFilter().removeSwearFilterPattern(pattern)) {
						ctx.msg.reply("Removed '$pattern' pattern from the swear filters list.")
					} else {
						ctx.msg.reply("'$pattern' pattern is not on the swear filters list.")
					}
					
					return true
				}
				
			}
			
		} else if (args.size == 2) {
			
			if (args[0].equals("muterole", true) || args[0].equals("mute-role", true)) {
				
				val id = Snowflake.of(args[1])
				auriel.getMessageListener().getMuteFilter().setMuteRoleId(id)
				ctx.msg.reply("Set the mute role id to '${args[1]}'.", true, 10L)
				return true
				
			}
			
		}
		
		return false
	}
	
}