package io.github.warren1001.resetbot

import discord4j.common.util.Snowflake

class TradeChannelCommand(private val auriel: Auriel): (CommandContext) -> Unit {
	
	private val tradeListeners = mutableMapOf<Snowflake, TradeChannelMessageListener>()
	
	init {
		val array = auriel.getJson()["trade.channels"]
		if (array != null && array.isJsonArray) {
			array.asJsonArray.map { it.asJsonObject }.forEach { jsonObject ->
				val id = Snowflake.of(jsonObject["id"].asLong)
				tradeListeners[id] = TradeChannelMessageListener(auriel, id, jsonObject["isBuy"].asBoolean)
			}
		}
	}
	
	override fun invoke(ctx: CommandContext) {
		TODO("Not yet implemented")
	}
	
	fun isTradeChannel(id: Snowflake): Boolean {
		return tradeListeners.containsKey(id)
	}
	
	fun handle(msg: ShallowMessage) {
		tradeListeners[msg.message.channelId]?.accept(msg)
	}
	
	fun remove(channelId: Snowflake, messageId: Snowflake) {
		if (tradeListeners.containsKey(channelId)) tradeListeners[channelId]!!.removeMessage(messageId)
	}
	
}