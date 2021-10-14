package io.github.warren1001.resetbot.command

import io.github.warren1001.resetbot.Auriel

class Command(private val auriel: Auriel, private val name: String, val permission: Int, val usage: String, val action: (CommandContext) -> Boolean)