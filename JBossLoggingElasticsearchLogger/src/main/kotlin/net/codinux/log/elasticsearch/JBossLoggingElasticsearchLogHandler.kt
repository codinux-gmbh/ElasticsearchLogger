package net.codinux.log.elasticsearch

import net.codinux.log.elasticsearch.errorhandler.ErrorHandler
import net.codinux.log.elasticsearch.errorhandler.JBossLoggingErrorHandler
import kotlin.jvm.JvmOverloads
import org.jboss.logmanager.ExtHandler
import org.jboss.logmanager.ExtLogRecord
import org.jboss.logmanager.ExtFormatter


open class JBossLoggingElasticsearchLogHandler @JvmOverloads constructor(
    errorHandler: ErrorHandler = JBossLoggingErrorHandler(),
    settings: LoggerSettings = PropertiesFilePropertiesProvider().extractSettings(errorHandler)
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
        val message = formatter.formatMessage(record)

        return LogRecord(message, record.instant, record.level.name, record.loggerName,
                record.threadName, record.hostName, record.thrown, record.mdcCopy, record.marker?.toString())
    }

    override fun flush() {
        elasticsearchLogHandler.flush()
    }

    override fun close() {
        elasticsearchLogHandler.close()
    }

}