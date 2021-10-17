package io.github.warren1001.resetbot.command.impl

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import discord4j.common.util.Snowflake
import io.github.warren1001.resetbot.Auriel
import io.github.warren1001.resetbot.command.CommandContext
import io.github.warren1001.resetbot.listener.ShallowMessage
import io.github.warren1001.resetbot.listener.TradeChannelMessageListener
import io.github.warren1001.resetbot.utils.JsonObjectBuilder

class TradeChannelCommand(private val auriel: Auriel): (CommandContext) -> Boolean {
	
	private val tradeListeners = mutableMapOf<Snowflake, TradeChannelMessageListener>()
	private val tradeJsonObject = if (auriel.getJson().has("trade") && auriel.getJson()["trade"].isJsonObject) auriel.getJson()["trade"].asJsonObject else JsonObject()
	private val blacklistJsonObject = if (tradeJsonObject.has("blacklist") && tradeJsonObject["blacklist"].isJsonObject) tradeJsonObject["blacklist"].asJsonObject else JsonObject()
	private val channelsJsonArray: JsonArray = if (tradeJsonObject.has("channels") && tradeJsonObject["channels"].isJsonArray) tradeJsonObject["channels"].asJsonArray else JsonArray()
	
	init {
		channelsJsonArray.map { it.asJsonObject }.forEach {
			val channelId = Snowflake.of(it["id"].asLong)
			tradeListeners[channelId] = TradeChannelMessageListener(auriel, channelId, it["is-buy"].asBoolean)
		}
		if (blacklistJsonObject.has("buy") && blacklistJsonObject["buy"].isJsonArray) {
			blacklistJsonObject["buy"].asJsonArray.map { it.asString }.forEach { TradeChannelMessageListener.addBuyBlacklist(it) }
		}
		if (blacklistJsonObject.has("sell") && blacklistJsonObject["sell"].isJsonArray) {
			blacklistJsonObject["sell"].asJsonArray.map { it.asString }.forEach { TradeChannelMessageListener.addSellBlacklist(it) }
		}
		if (tradeJsonObject.has("maximum-lines")) TradeChannelMessageListener.maximumLines = tradeJsonObject["maximum-lines"].asInt
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
								saveBlacklist(true)
								ctx.msg.reply("Added '$word' as a blacklist word to the buy list.", true, 10L)
							} else {
								ctx.msg.reply("The word '$word' was already blacklisted in the buy list.", true, 10L)
							}
							
							
						} else {
							
							if (TradeChannelMessageListener.addSellBlacklist(word)) {
								saveBlacklist(false)
								ctx.msg.reply("Added '$word' as a blacklist word to the sell list.", true, 10L)
							} else {
								ctx.msg.reply("The word '$word' was already blacklisted in the sell list.", true, 10L)
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
								saveBlacklist(true)
								ctx.msg.reply("Removed '$word' as a blacklist word from the buy list.", true, 10L)
							} else {
								ctx.msg.reply("The word '$word' was not blacklisted in the buy list.", true, 10L)
							}
							
							
						} else {
							
							if (TradeChannelMessageListener.removeSellBlacklist(word)) {
								saveBlacklist(false)
								ctx.msg.reply("Removed '$word' as a blacklist word from the sell list.", true, 10L)
							} else {
								ctx.msg.reply("The word '$word' was not blacklisted in the sell list.", true, 10L)
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
						saveBlacklist(true)
					}
					if (TradeChannelMessageListener.addSellBlacklist(word)) {
						removed = true
						saveBlacklist(false)
					}
					
					if (removed) {
						ctx.msg.reply("Added '$word' as a blacklist word to both lists.", true, 10L)
					} else {
						ctx.msg.reply("The word '$word' was already blacklisted in both lists.", true, 10L)
					}
					
					return true
					
				} else if (args[1].equals("remove", true)) {
					
					val word = args[2].lowercase()
					var removed = false
					
					if (TradeChannelMessageListener.removeBuyBlacklist(word)) {
						removed = true
						saveBlacklist(true)
					}
					if (TradeChannelMessageListener.removeSellBlacklist(word)) {
						removed = true
						saveBlacklist(false)
					}
					
					if (removed) {
						ctx.msg.reply("Removed '$word' as a blacklist word from both lists.", true, 10L)
					} else {
						ctx.msg.reply("The word '$word' was not blacklisted in either list.", true, 10L)
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
							
							channelsJsonArray.filter { it.asJsonObject["id"].asLong == channelId.asLong() }.forEach { it.asJsonObject.addProperty("is-buy", isBuy) }
							saveChannels()
							ctx.msg.reply("Set this channel as a " + (if (isBuy) "buying" else "selling") + " channel.", true, 10L)
							
						} else {
							ctx.msg.reply("This channel was already a " + (if (isBuy) "buying" else "selling") + " channel.", true, 10L)
						}
						
					} else {
						tradeListeners[channelId] = TradeChannelMessageListener(auriel, channelId, isBuy)
						channelsJsonArray.add(JsonObjectBuilder().addProperty("id", channelId.asLong()).addProperty("is-buy", isBuy).build())
						saveChannels()
						ctx.msg.reply("Set this channel as a " + (if (isBuy) "buying" else "selling") + " channel.", true, 10L)
					}
					
					return true
				}
				
			} else if (args[0].equals("lines", true)) {
				
				val maximumLines = args[1].toIntOrNull()
				if (maximumLines != null) {
					
					TradeChannelMessageListener.maximumLines = maximumLines
					tradeJsonObject.addProperty("maximum-lines", maximumLines)
					auriel.getJson().add("trade", tradeJsonObject)
					ctx.msg.reply("Set the maximum lines in a trade channel to $maximumLines.", true, 10L)
					auriel.saveJson()
					
					return true
				}
				
			}
			
		} else if (args.size == 1) {
			
			if (args[0].equals("remove", true)) {
				
				if (tradeListeners.containsKey(channelId)) {
					tradeListeners.remove(channelId)
					channelsJsonArray.removeAll { it.asJsonObject["id"].asLong == channelId.asLong() }
					saveChannels()
					ctx.msg.reply("Removed this channel as a trading channel.", true, 10L)
				} else {
					ctx.msg.reply("This channel is not a trading channel.", true, 10L)
				}
				
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
	
	private fun saveBlacklist(type: Boolean) {
		val jsonArray = JsonArray()
		if (type) TradeChannelMessageListener.buyBlacklist.forEach { jsonArray.add(it) }
		else TradeChannelMessageListener.sellBlacklist.forEach { jsonArray.add(it) }
		blacklistJsonObject.add(if (type) "buy" else "sell", jsonArray)
		tradeJsonObject.add("blacklist", blacklistJsonObject)
		auriel.getJson().add("trade", tradeJsonObject)
		auriel.saveJson()
	}
	
	private fun saveChannels() {
		tradeJsonObject.add("channels", channelsJsonArray)
		auriel.getJson().add("trade", tradeJsonObject)
		auriel.saveJson()
	}
	
}