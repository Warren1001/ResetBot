package io.github.warren1001.resetbot

class Command(private val auriel: Auriel, private val permission: Int, val action: (CommandContext) -> Unit) {
	
	fun hasPermission(msg: ShallowMessage): Boolean {
		return auriel.getUserManager().hasPermission(msg.author.id, permission)
	}
	
}