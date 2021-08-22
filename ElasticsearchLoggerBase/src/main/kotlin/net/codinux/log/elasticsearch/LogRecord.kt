package net.codinux.log.elasticsearch

import java.time.Instant


open class LogRecord(
    open val message: String,

    open val timestamp: Instant,

    open val level: String,

    open val logger: String,

    open val threadName: String,

    open val host: String,

    open val exception: Throwable?,

    open val mdc: Map<String, String>?
)