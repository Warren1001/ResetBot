package io.github.warren1001.resetbot

class CommandManager(private val auriel: Auriel) {
	
	private var prefix = "!"
	private val commands = mutableMapOf<String, (CommandContext) -> Unit>()
	
	fun registerCommand(name: String, action: (CommandContext) -> Unit) {
		if (commands.containsKey(name.lowercase())) return auriel.error("Command '$name' is already registered!")
		commands[name.lowercase()] = action
	}
	
	fun handle(msg: ShallowMessage): Boolean {
		var message = msg.message.content
		if (!message.startsWith(prefix, ignoreCase = true)) return false
		message = message.substring(prefix.length)
		val args = if (message.contains(' ')) message.split(' ', limit = 2) else mutableListOf(message)
		val command = args[0].lowercase()
		if (!commands.containsKey(command)) return false
		commands[command]!!.invoke(CommandContext(msg, if (args.size == 1) "" else args[1]))
		return true
	}
	
}