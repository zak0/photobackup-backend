package jaaska.jaakko.photosapp.server

object Logger {

    enum class LogLevel {
        VERBOSE,
        INFO,
        WARNING,
        ERROR,
        SILENT
    }

    private enum class LogType {
        DEBUG,
        INFO,
        WARNING,
        ERROR
    }

    var debugLogging = false
    var logLevel = LogLevel.INFO

    fun d(message: String, throwable: Throwable? = null) {
        if (debugLogging) {
            log(LogType.DEBUG, message, throwable)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (logLevel != LogLevel.SILENT) {
            log(LogType.ERROR, message, throwable)
        }
    }

    fun i(message: String, throwable: Throwable? = null) {
        if (logLevel in listOf(LogLevel.VERBOSE, LogLevel.INFO)) {
            log(LogType.INFO, message, throwable)
        }
    }

    fun w(message: String, throwable: Throwable? = null) {
        if (logLevel in listOf(LogLevel.VERBOSE, LogLevel.INFO, LogLevel.WARNING)) {
            log(LogType.WARNING, message, throwable)
        }
    }

    private fun log(type: LogType, message: String, throwable: Throwable?) {
        val prefix = when (type) {
            LogType.DEBUG -> "🍀 DEBUG"
            LogType.ERROR -> "❌ ERROR"
            LogType.INFO -> "ℹ INFO"
            LogType.WARNING -> "⚠ WARNING"
        }

        println("$prefix - $message")
        throwable?.message?.also { println(it) }
    }

}