package io.github.warren1001.resetbot

import discord4j.common.util.Snowflake
import io.github.warren1001.resetbot.command.CommandContext

class TriviaCommand(private val auriel: Auriel): (CommandContext) -> Boolean {
	
	override fun invoke(ctx: CommandContext): Boolean {
		
		val arguments = ctx.arguments
		if (arguments.isEmpty()) return false
		
		val args = arguments.split(' ', limit = 3)
		
		if (!arguments.contains(' ')) {
			
			if (arguments.equals("setchannel", true)) {
				
				auriel.getMessageListener().getTriviaManager().setChannel(ctx.msg.channel)
				ctx.msg.reply("This channel is now the trivia channel!", true, 10L)
				
			} else if (arguments.equals("start", true)) {
				
				if (auriel.getMessageListener().getTriviaManager().start()) {
					ctx.msg.reply("Starting the trivia up.", true, 10L)
				} else {
					ctx.msg.reply("The trivia was not stopped!", true, 10L)
				}
				
			} else if (arguments.equals("stop", true)) {
				
				if (auriel.getMessageListener().getTriviaManager().stop()) {
					ctx.msg.reply("Stopping the trivia.", true, 10L)
				} else {
					ctx.msg.reply("The trivia was not start!", true, 10L)
				}
				
			} else if (arguments.equals("reload", true)) {
				
				auriel.getMessageListener().getTriviaManager().reloadQuestions()
				ctx.msg.reply("Reloaded the trivia questions.", true, 10L)
				
			} else return false
			
		} else if (args.size == 2) {
			
			if (args[0].equals("setchannel", true)) {
				
				val channelId = Snowflake.of(args[1].toLong())
				auriel.getMessageListener().getTriviaManager().setChannelId(channelId)
				ctx.msg.reply("The channel has been set as the trivia channel.")
				
			} else return false
			
		} else if (args.size == 3) {
			
			if (args[0].equals("question", true)) {
				
				if (args[1].equals("add", true)) {
					
					val content = args[2]
					val contentArgs = content.split('|', limit = 2)
					if (contentArgs.size != 2) return false
					val question = contentArgs[0]
					val answer = contentArgs[1]
					auriel.getMessageListener().getTriviaManager().addQuestion(question, answer)
					ctx.msg.reply("Added the question $question with the answer $answer to the trivia question pool.")
					
				} else if (args[1].equals("remove", true)) {
					
					val content = args[2]
					if (auriel.getMessageListener().getTriviaManager().removeQuestion(content)) {
						ctx.msg.reply("Removed the question containing '$content'.", true, 10L)
					} else {
						ctx.msg.reply("There was no question containing '$content'.", true, 10L)
					}
					
				} else return false
				
			} else return false
			
		} else return false
		
		return true
	}
	
	
}