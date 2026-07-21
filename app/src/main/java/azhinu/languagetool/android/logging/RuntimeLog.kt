package azhinu.languagetool.android.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

data class LogEntry(
    val timestamp: Instant,
    val level: LogLevel,
    val message: String,
    val details: String? = null
)

object RuntimeLog {
    private const val MAX_ENTRIES = 500
    private val lock = Any()
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    fun debug(message: String, details: String? = null) = add(LogLevel.DEBUG, message, details)
    fun info(message: String, details: String? = null) = add(LogLevel.INFO, message, details)
    fun warn(message: String, details: String? = null) = add(LogLevel.WARN, message, details)
    fun error(message: String, throwable: Throwable? = null) = add(
        LogLevel.ERROR,
        message,
        throwable?.let { "${it::class.java.simpleName}: ${it.message.orEmpty()}" }
    )

    fun clear() = synchronized(lock) { _entries.value = emptyList() }

    private fun add(level: LogLevel, message: String, details: String?) = synchronized(lock) {
        val entry = LogEntry(Instant.now(), level, message.replaceFirstChar { it.uppercase() }, details)
        _entries.value = (_entries.value + entry).takeLast(MAX_ENTRIES)
    }
}
