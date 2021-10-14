package io.github.warren1001.resetbot.command.impl

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import discord4j.common.util.Snowflake
import io.github.warren1001.resetbot.Auriel
import io.github.warren1001.resetbot.command.CommandContext
import io.github.warren1001.resetbot.listener.ShallowMessage
import io.github.warren1001.resetbot.listener.TradeChannelMessageListener

class TradeChannelCommand(private val auriel: Auriel): (CommandContext) -> Boolean {
	
	private val tradeListeners = mutableMapOf<Snowflake, TradeChannelMessageListener>()
	private val jsonArray: JsonArray
	
	init {
		val jsonObject = auriel.getJson()["trade.channels"]
		if (jsonObject != null && jsonObject.isJsonArray) {
			jsonArray = jsonObject.asJsonArray
			jsonArray.forEach {
				val channelId = Snowflake.of(it.asJsonObject["id"].asLong)
				tradeListeners[channelId] = TradeChannelMessageListener(auriel, channelId, it.asJsonObject["is-buy"].asBoolean)
			}
		} else jsonArray = JsonArray()
		if (auriel.getJson().has("trade.maximum-lines")) TradeChannelMessageListener.maximumLines = auriel.getJson()["trade.maximum-lines"].asInt
	}
	
	override fun invoke(ctx: CommandContext): Boolean {
		
		val arguments = ctx.arguments
		if (arguments.isEmpty()) return false
		
		val args = if (arguments.contains(' ')) arguments.split(' ') else mutableListOf(arguments)
		val channelId = ctx.msg.message.channelId
		
		if (args.size == 4) {
			
			if (args[0].equals("blacklist", true)) {
				
				if (args[1].equals("add", true)) {
					
					val word = args[2].lowercase()
					
					if (args[3].equals("true", true) || args[4].equals("false", true)) {
						
						val isBuy = args[3].toBoolean()
						
						if (isBuy) {
							
							if (TradeChannelMessageListener.addBuyBlacklist(word)) {
								val jsonArray = JsonArray()
								TradeChannelMessageListener.buyBlacklist.forEach { jsonArray.add(it) }
								auriel.getJson().add("trade.blacklist.buy", jsonArray)
								auriel.getMessageListener().reply(ctx.msg, "Added '$word' as a blacklist word to the buy list.", true, 10L)
								auriel.saveJson()
							} else {
								auriel.getMessageListener().reply(ctx.msg, "The word '$word' was already blacklisted in the buy list.", true, 10L)
							}
							
							
						} else {
							
							if (TradeChannelMessageListener.addSellBlacklist(word)) {
								val jsonArray = JsonArray()
								TradeChannelMessageListener.sellBlacklist.forEach { jsonArray.add(it) }
								auriel.getJson().add("trade.blacklist.sell", jsonArray)
								auriel.getMessageListener().reply(ctx.msg, "Added '$word' as a blacklist word to the sell list.", true, 10L)
								auriel.saveJson()
							} else {
								auriel.getMessageListener().reply(ctx.msg, "The word '$word' was already blacklisted in the sell list.", true, 10L)
							}
							
						}
						
						return true
					}
					
				} else if (args[1].equals("remove", true)) {
					
					val word = args[2].lowercase()
					
					if (args[3].equals("true", true) || args[4].equals("false", true)) {
						
						val isBuy = args[3].toBoolean()
						
						if (isBuy) {
							
							if (TradeChannelMessageListener.removeBuyBlacklist(word)) {
								val jsonArray = JsonArray()
								TradeChannelMessageListener.buyBlacklist.forEach { jsonArray.add(it) }
								auriel.getJson().add("trade.blacklist.buy", jsonArray)
								auriel.getMessageListener().reply(ctx.msg, "Removed '$word' as a blacklist word from the buy list.", true, 10L)
								auriel.saveJson()
							} else {
								auriel.getMessageListener().reply(ctx.msg, "The word '$word' was not blacklisted in the buy list.", true, 10L)
							}
							
							
						} else {
							
							if (TradeChannelMessageListener.removeSellBlacklist(word)) {
								val jsonArray = JsonArray()
								TradeChannelMessageListener.sellBlacklist.forEach { jsonArray.add(it) }
								auriel.getJson().add("trade.blacklist.sell", jsonArray)
								auriel.getMessageListener().reply(ctx.msg, "Removed '$word' as a blacklist word from the sell list.", true, 10L)
								auriel.saveJson()
							} else {
								auriel.getMessageListener().reply(ctx.msg, "The word '$word' was not blacklisted in the sell list.", true, 10L)
							}
							
						}
						
						return true
					}
					
				}
				
			}
			
		} else if (args.size == 3) {
			
			if (args[0].equals("blacklist", true)) {
				
				if (args[1].equals("add", true)) {
					
					val word = args[2].lowercase()
					var removed = false
					
					if (TradeChannelMessageListener.addBuyBlacklist(word)) {
						removed = true
						val jsonArray = JsonArray()
						TradeChannelMessageListener.buyBlacklist.forEach { jsonArray.add(it) }
						auriel.getJson().add("trade.blacklist.buy", jsonArray)
						auriel.saveJson()
					}
					if (TradeChannelMessageListener.addSellBlacklist(word)) {
						removed = true
						val jsonArray = JsonArray()
						TradeChannelMessageListener.sellBlacklist.forEach { jsonArray.add(it) }
						auriel.getJson().add("trade.blacklist.sell", jsonArray)
						auriel.saveJson()
					}
					
					if (removed) {
						auriel.getMessageListener().reply(ctx.msg, "Added '$word' as a blacklist word to both lists.", true, 10L)
					} else {
						auriel.getMessageListener().reply(ctx.msg, "The word '$word' was already blacklisted in both lists.", true, 10L)
					}
					
					return true
					
				} else if (args[1].equals("remove", true)) {
					
					val word = args[2].lowercase()
					var removed = false
					
					if (TradeChannelMessageListener.removeBuyBlacklist(word)) {
						removed = true
						val jsonArray = JsonArray()
						TradeChannelMessageListener.buyBlacklist.forEach { jsonArray.add(it) }
						auriel.getJson().add("trade.blacklist.buy", jsonArray)
						auriel.saveJson()
					}
					if (TradeChannelMessageListener.removeSellBlacklist(word)) {
						removed = true
						val jsonArray = JsonArray()
						TradeChannelMessageListener.sellBlacklist.forEach { jsonArray.add(it) }
						auriel.getJson().add("trade.blacklist.sell", jsonArray)
						auriel.saveJson()
					}
					
					if (removed) {
						auriel.getMessageListener().reply(ctx.msg, "Removed '$word' as a blacklist word from both lists.", true, 10L)
					} else {
						auriel.getMessageListener().reply(ctx.msg, "The word '$word' was not blacklisted in either list.", true, 10L)
					}
					
					return true
				}
				
			}
			
		} else if (args.size == 2) {
			
			if (args[0].equals("set", true)) {
				
				if (args[1].equals("true", true) || args[1].equals("false", true)) {
					
					val isBuy = args[1].toBoolean()
					
					if (tradeListeners.containsKey(channelId)) {
						
						if (tradeListeners[channelId]!!.setIsBuy(isBuy)) {
							
							jsonArray.filter { it.asJsonObject["id"].asLong == channelId.asLong() }.forEach { it.asJsonObject.addProperty("is-buy", isBuy) }
							auriel.getJson().add("trade.channels", jsonArray)
							auriel.getMessageListener().reply(ctx.msg, "Set this channel as a " + (if (isBuy) "buying" else "selling") + " channel.", true, 10L)
							auriel.saveJson()
							
						} else {
							auriel.getMessageListener().reply(ctx.msg, "This channel was already a " + (if (isBuy) "buying" else "selling") + " channel.", true, 10L)
						}
						
					} else {
						tradeListeners[channelId] = TradeChannelMessageListener(auriel, channelId, isBuy)
						val channelObj = JsonObject()
						channelObj.addProperty("id", channelId.asLong())
						channelObj.addProperty("is-buy", isBuy)
						jsonArray.add(channelObj)
						auriel.getJson().add("trade.channels", jsonArray)
						auriel.getMessageListener().reply(ctx.msg, "Set this channel as a " + (if (isBuy) "buying" else "selling") + " channel.", true, 10L)
						auriel.saveJson()
					}
					
					return true
				}
				
			} else if (args[0].equals("lines", true)) {
				
				val maximumLines = args[1].toIntOrNull()
				if (maximumLines != null) {
					
					TradeChannelMessageListener.maximumLines = maximumLines
					auriel.getJson().addProperty("trade.maximum-lines", maximumLines)
					auriel.getMessageListener().reply(ctx.msg, "Set the maximum lines in a trade channel to $maximumLines.", true, 10L)
					auriel.saveJson()
					
					return true
				}
				
			}
			
		} else if (args.size == 1) {
			
			if (args[0].equals("remove", true)) {
				
				tradeListeners.remove(channelId)
				jsonArray.removeAll { it.asJsonObject["id"].asLong == channelId.asLong() }
				auriel.getJson().add("trade.channels", jsonArray)
				auriel.getMessageListener().reply(ctx.msg, "Removed this channel as a trading channel.", true, 10L)
				auriel.saveJson()
				
				return true
			}
			
		}
		
		
		return false
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