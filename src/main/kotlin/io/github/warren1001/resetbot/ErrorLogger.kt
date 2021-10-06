package io.github.warren1001.resetbot

import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path

class ErrorLogger(private val auriel: Auriel) {
	
	private val file = File("logs", "errorLog.txt")
	private val writer: PrintWriter
	
	init {
		
		file.parentFile.mkdir()
		
		if (file.exists()) {
			Files.move(file.toPath(), Path.of("logs", "errorLog-${System.currentTimeMillis()}.txt"))
			file.writeText("")
		} else {
			file.createNewFile()
		}
		writer = file.printWriter()
		
	}
	
	fun logError(error: Throwable) {
		val curTime = auriel.getCurrentTime()
		writer.print("$curTime: ")
		error.printStackTrace(writer)
		writer.print(System.lineSeparator())
		writer.flush()
	}
	
	fun close() {
		writer.close()
	}
	
}