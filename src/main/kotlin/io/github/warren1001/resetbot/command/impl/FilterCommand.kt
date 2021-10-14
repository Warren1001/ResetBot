package io.github.warren1001.resetbot.command.impl

import io.github.warren1001.resetbot.Auriel
import io.github.warren1001.resetbot.command.CommandContext

class FilterCommand(private val auriel: Auriel): (CommandContext) -> Boolean {
	
	override fun invoke(ctx: CommandContext): Boolean {
		
		val arguments = ctx.arguments
		if (arguments.isEmpty() || !arguments.contains(' ')) return false
		
		val args = arguments.split(' ')
		
		if (args.size == 4) {
			
			if (args[0].equals("pattern", true)) {
				
				if (args[1].equals("add", true)) {
					
					val pattern = args[2]
					val replacement = args[3]
					
					if (auriel.getMessageListener().getSwearFilter().addSwearFilterPattern(pattern, replacement)) {
						auriel.getMessageListener().reply(ctx.msg, "Added '$pattern' pattern with '$replacement' to the swear filters list.")
					} else {
						auriel.getMessageListener().reply(ctx.msg, "'$pattern' pattern is already on the swear filters list.")
					}
					
					return true
					
				}
				
			} else if (args[0].equals("word", true)) {
				
				if (args[1].equals("add", true)) {
					
					val pattern = auriel.getMessageListener().getSwearFilter().constructBasicPattern(args[2])
					val replacement = args[3]
					
					if (auriel.getMessageListener().getSwearFilter().addSwearFilterPattern(pattern, replacement)) {
						auriel.getMessageListener().reply(ctx.msg, "Added '$pattern' pattern with '$replacement' to the swear filters list.")
					} else {
						auriel.getMessageListener().reply(ctx.msg, "'$pattern' pattern is already on the swear filters list.")
					}
					
					return true
					
				}
				
			}
			
		} else if (args.size == 3) {
			
			if (args[0].equals("pattern", true)) {
				
				if (args[1].equals("remove", true)) {
					
					val pattern = args[2]
					
					if (auriel.getMessageListener().getSwearFilter().removeSwearFilterPattern(pattern)) {
						auriel.getMessageListener().reply(ctx.msg, "Removed '$pattern' pattern from the swear filters list.")
					} else {
						auriel.getMessageListener().reply(ctx.msg, "'$pattern' pattern is not on the swear filters list.")
					}
					
					return true
				}
				
			}
			
		} else if (args[0].equals("word", true)) {
			
			if (args[1].equals("remove", true)) {
				
				val pattern = auriel.getMessageListener().getSwearFilter().constructBasicPattern(args[2])
				
				if (auriel.getMessageListener().getSwearFilter().removeSwearFilterPattern(pattern)) {
					auriel.getMessageListener().reply(ctx.msg, "Removed '$pattern' pattern from the swear filters list.")
				} else {
					auriel.getMessageListener().reply(ctx.msg, "'$pattern' pattern is not on the swear filters list.")
				}
				
				return true
			}
			
		}
		
		return false
	}
	
}