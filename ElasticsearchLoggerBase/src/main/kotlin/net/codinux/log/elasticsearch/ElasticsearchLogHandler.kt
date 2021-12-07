package net.codinux.log.elasticsearch

import net.codinux.log.elasticsearch.errorhandler.ErrorHandler
import net.codinux.log.elasticsearch.errorhandler.OnlyOnceErrorHandler
import net.codinux.log.elasticsearch.errorhandler.StdErrErrorHandler
import java.lang.Exception


open class ElasticsearchLogHandler @JvmOverloads constructor(
        settings: LoggerSettings,
        protected open val errorHandler: ErrorHandler = OnlyOnceErrorHandler(StdErrErrorHandler())
) {

    protected open val logWriter: LogWriter


    init {
        logWriter = createLogWriter(settings)
    }

    protected open fun createLogWriter(settings: LoggerSettings): ElasticsearchLogWriter {
        return ElasticsearchLogWriter(settings, errorHandler)
    }


    open fun handle(record: LogRecord) {
        try {
            handleRecord(record)
        } catch (e: Exception) {
            showError("Could not add log record $record to log queue", e)
        }
    }

    protected open fun handleRecord(record: LogRecord) {
        logWriter.writeRecord(record)
    }

    open fun flush() {
        // nothing to do anymore
    }

    open fun close() {
        try {
            logWriter.close()
        } catch (e: Exception) {
            showError("Could not stop Log Writer $logWriter", e)
        }

        errorHandler.logInfo("Closed ${javaClass.simpleName}")
    }

    protected open fun showError(message: String, e: Throwable?) {
        errorHandler.logError(message, e)
    }

}