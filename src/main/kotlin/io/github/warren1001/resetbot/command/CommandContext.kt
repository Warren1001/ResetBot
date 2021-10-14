package io.github.warren1001.resetbot.command

import io.github.warren1001.resetbot.listener.ShallowMessage

class CommandContext(val msg: ShallowMessage, val arguments: String) {}