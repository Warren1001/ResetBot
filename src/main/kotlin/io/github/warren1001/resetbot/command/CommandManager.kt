package io.github.warren1001.resetbot.command

import io.github.warren1001.resetbot.Auriel
import io.github.warren1001.resetbot.listener.ShallowMessage
import io.github.warren1001.resetbot.listener.UserManager

class CommandManager(private val auriel: Auriel) {
	
	var prefix = "!"
	
	private val commands = mutableMapOf<String, Command>()
	
	fun registerCommand(name: String, permission: Int = UserManager.ADMINISTRATOR,
	                    usage: String = "Usage: ${auriel.getMessageListener().getCommandManager().prefix}$name", action: (CommandContext) -> Boolean) {
		if (commands.containsKey(name.lowercase())) return auriel.error("Command '$name' is already registered!")
		commands[name.lowercase()] = Command(auriel, name.lowercase(), permission, usage, action)
	}
	
	fun registerSimpleCommand(name: String, permission: Int = UserManager.ADMINISTRATOR,
	                    usage: String = "Usage: ${auriel.getMessageListener().getCommandManager().prefix}$name", action: (CommandContext) -> Unit) {
		if (commands.containsKey(name.lowercase())) return auriel.error("Command '$name' is already registered!")
		commands[name.lowercase()] = Command(auriel, name.lowercase(), permission, usage) {
			action.invoke(it)
			return@Command true
		}
	}
	
	fun handle(msg: ShallowMessage): Boolean {
		var message = msg.message.content
		if (!message.startsWith(prefix, ignoreCase = true)) return false
		message = message.substring(prefix.length)
		val args = if (message.contains(' ')) message.split(' ', limit = 2) else mutableListOf(message)
		val commandName = args[0].lowercase()
		if (!commands.containsKey(commandName)) return false
		val command = commands[commandName]!!
		if (!auriel.getUserManager().hasPermission(msg.author.id, command.permission)) return false
		if (!command.action.invoke(CommandContext(msg, if (args.size == 1) "" else args[1]))) auriel.getMessageListener().reply(msg, command.usage)
		return true
	}
	
}