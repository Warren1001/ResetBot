package io.github.warren1001.resetbot.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class FileUtils {

	companion object {
		
		val gson: Gson = GsonBuilder().setPrettyPrinting().create()
		
		fun readJsonLines(path: String): JsonObject {
			val element = JsonParser.parseString(try { Files.readString(Paths.get(path)) } catch (e: IOException) { "" })
			return if (element != null && element.isJsonObject) element.asJsonObject else JsonObject()
		}
		
		fun readBasicLine(path: String): String {
			return try { Files.readString(Paths.get(path)) } catch (e: IOException) { "" }
		}
		
		fun readBasicLines(path: String): List<String> {
			return try { Files.readAllLines(Paths.get(path)) } catch (e: IOException) { mutableListOf() }
		}
		
		fun saveJsonLines(path: String, jsonObject: JsonObject) {
			val path = Paths.get(path)
			mkdirs(path)
			try {
				Files.write(path, gson.toJson(jsonObject).toByteArray())
			} catch (e: IOException) {
				e.printStackTrace()
			}
		}
		
		fun saveBasicLine(path: String, line: String) {
			val path = Paths.get(path)
			mkdirs(path)
			try {
				Files.writeString(path, line)
			} catch (e: IOException) {
				e.printStackTrace()
			}
		}
		
		fun saveBasicLines(path: String, lines: List<String>) {
			val path = Paths.get(path)
			mkdirs(path)
			try {
				Files.write(path, lines)
			} catch (e: IOException) {
				e.printStackTrace()
			}
		}
		
		private fun mkdirs(path: Path): Boolean {
			return path.toFile().parentFile?.mkdirs() ?: true
		}
		
	}

}