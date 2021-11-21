package net.codinux.log.elasticsearch

import net.codinux.log.elasticsearch.errorhandler.ErrorHandler
import net.codinux.log.elasticsearch.errorhandler.JBossLoggingErrorHandler
import kotlin.jvm.JvmOverloads
import org.jboss.logmanager.ExtHandler
import org.jboss.logmanager.ExtLogRecord
import org.jboss.logmanager.ExtFormatter


open class JBossLoggingElasticsearchLogHandler @JvmOverloads constructor(
    settings: LoggerSettings = PropertiesFilePropertiesProvider().extractSettings(),
    errorHandler: ErrorHandler = JBossLoggingErrorHandler()
) : ExtHandler() {

    protected open val elasticsearchLogHandler: ElasticsearchLogHandler

    init {
        formatter = object : ExtFormatter() {
            override fun format(record: ExtLogRecord): String {
                return formatMessage(record)
            }
        }

        elasticsearchLogHandler = ElasticsearchLogHandler(settings, errorHandler)
    }


    override fun doPublish(record: ExtLogRecord?) {
        if (record != null) {
            elasticsearchLogHandler.handle(mapRecord(record))
        }
    }

    protected open fun mapRecord(record: ExtLogRecord): LogRecord {
        var message = formatter.formatMessage(record)

        record.thrown?.let { exception -> message += ": " + exception.message }

        return LogRecord(message, record.instant, record.level.name, record.loggerName,
                record.threadName, record.hostName, record.thrown, record.mdcCopy)
    }

    override fun flush() {
        elasticsearchLogHandler.flush()
    }

    override fun close() {
        elasticsearchLogHandler.close()
    }

}