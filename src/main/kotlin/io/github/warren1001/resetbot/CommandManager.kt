package io.github.warren1001.resetbot

class CommandManager(private val auriel: Auriel) {
	
	private var prefix = "!"
	private val commands = mutableMapOf<String, Command>()
	
	fun registerCommand(name: String, permission: Int = UserManager.ADMINISTRATOR, action: (CommandContext) -> Unit) {
		if (commands.containsKey(name.lowercase())) return auriel.error("Command '$name' is already registered!")
		commands[name.lowercase()] = Command(auriel, permission, action)
	}
	
	fun handle(msg: ShallowMessage): Boolean {
		var message = msg.message.content
		if (!message.startsWith(prefix, ignoreCase = true)) return false
		message = message.substring(prefix.length)
		val args = if (message.contains(' ')) message.split(' ', limit = 2) else mutableListOf(message)
		val commandName = args[0].lowercase()
		if (!commands.containsKey(commandName)) return false
		val command = commands[commandName]!!
		if (!command.hasPermission(msg)) return false
		command.action.invoke(CommandContext(msg, if (args.size == 1) "" else args[1]))
		return true
	}
	
}