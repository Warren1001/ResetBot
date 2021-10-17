package io.github.warren1001.resetbot

import discord4j.common.util.Snowflake

class LazyTriviaObject<T>(var userId: Snowflake, var username: String, var data: T)