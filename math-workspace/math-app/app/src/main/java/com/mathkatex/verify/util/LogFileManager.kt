package com.mathkatex.verify.util

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 文件日志工具 - 写入 APP 内部存储
 */
object LogFileManager {
    private const val LOG_DIR = "logs"
    private const val LOG_FILE = "math_app.log"
    private const val MAX_SIZE = 2 * 1024 * 1024 // 2MB

    private val queue = ConcurrentLinkedQueue<String>()
    private var logFile: File? = null
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val dir = File(context.filesDir, LOG_DIR)
        if (!dir.exists()) dir.mkdirs()
        logFile = File(dir, LOG_FILE)
        // Truncate if too big
        logFile?.let { f ->
            if (f.exists() && f.length() > MAX_SIZE) {
                f.writeText("")
            }
        }
        initialized = true
        // Write startup marker
        log("INFO", "LogFileManager initialized, app start")
    }

    fun log(tag: String, message: String) {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val line = "$ts $tag $message"
        queue.offer(line)
        flush()
    }

    fun logRequest(tag: String, message: String) {
        log(tag, ">>> $message")
    }

    fun logResponse(tag: String, message: String) {
        log(tag, "<<< $message")
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val sw = StringWriter()
        throwable?.printStackTrace(PrintWriter(sw))
        val line = "$ts $tag ERROR: $message${if (throwable != null) "\n${sw.toString()}" else ""}"
        queue.offer(line)
        flush()
    }

    private fun flush() {
        if (queue.isEmpty()) return
        val lines = mutableListOf<String>()
        while (true) {
            val l = queue.poll() ?: break
            lines.add(l)
        }
        if (lines.isEmpty()) return
        logFile?.appendText(lines.joinToString("\n") + "\n")
    }

    fun getLogFile(): File? = logFile

    fun readLastLines(n: Int = 100): String {
        val f = logFile ?: return ""
        if (!f.exists()) return ""
        val lines = f.readLines()
        return lines.takeLast(n).joinToString("\n")
    }
}
