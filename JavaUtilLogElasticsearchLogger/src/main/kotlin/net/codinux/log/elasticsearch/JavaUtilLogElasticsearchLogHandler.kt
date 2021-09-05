package net.codinux.log.elasticsearch

import net.codinux.log.elasticsearch.errorhandler.ErrorHandler
import net.codinux.log.elasticsearch.errorhandler.JavaUtilLogErrorHandler
import java.net.InetAddress
import java.util.logging.Handler


open class JavaUtilLogElasticsearchLogHandler @JvmOverloads constructor(
    settings: LoggerSettings = JavaUtilLogPropertyProvider().extractSettings(),
    errorHandler: ErrorHandler = JavaUtilLogErrorHandler()
) : Handler() {

    protected open val elasticsearchLogHandler = ElasticsearchLogHandler(settings, errorHandler)


    override fun publish(record: java.util.logging.LogRecord?) {
        if (record != null && isLoggable(record)) {
            elasticsearchLogHandler.handle(mapRecord(record))
        }
    }

    protected open fun mapRecord(record: java.util.logging.LogRecord): LogRecord {
        var message = if (record.message == null) "" else record.message
        val threadName = Thread.currentThread().name
        val hostName = InetAddress.getLocalHost().hostName // HostName.getQualifiedHostName(); // TODO

        if (record.parameters != null) {
            message = String.format(record.message, *record.parameters)
        }

        record.thrown?.let { exception -> message += ": " + exception.message }

        return LogRecord(message, record.instant, record.level.name, record.loggerName,
                threadName, hostName, record.thrown, null)
    }

    override fun flush() {
        elasticsearchLogHandler.flush()
    }

    override fun close() {
        elasticsearchLogHandler.close()
    }

}