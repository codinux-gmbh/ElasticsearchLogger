package net.codinux.log.elasticsearch

import java.time.Instant


open class LogRecord @JvmOverloads constructor(
    open val message: String,

    open val timestamp: Instant,

    open val level: String,

    open val logger: String,

    open val threadName: String,

    open val host: String,

    open val exception: Throwable? = null,

    open val mdc: Map<String, String>? = null
) {

    override fun toString(): String {
        return "$timestamp $level $message"
    }

}