package net.codinux.log.elasticsearch

import net.codinux.log.elasticsearch.kubernetes.KubernetesInfo
import net.codinux.log.elasticsearch.kubernetes.KubernetesInfoRetriever
import net.codinux.log.elasticsearch.errorhandler.ErrorHandler
import net.codinux.log.elasticsearch.errorhandler.OnlyOnceErrorHandler
import net.codinux.log.elasticsearch.errorhandler.StdErrErrorHandler
import org.slf4j.MDC
import java.lang.Exception
import java.util.concurrent.CopyOnWriteArrayList


open class ElasticsearchLogHandler @JvmOverloads constructor(
        settings: LoggerSettings,
        protected open val errorHandler: ErrorHandler = OnlyOnceErrorHandler(StdErrErrorHandler())
) {

    protected open val logWriter: LogWriter = createLogWriter(settings)

    protected open var kubernetesInfo: KubernetesInfo? = null

    protected open val unhandledLogs = CopyOnWriteArrayList<LogRecord>()

    protected open var handleLogs = false


    init {
        if (settings.includeKubernetesInfo) {
            retrieveKubernetesInfo()
        } else {
            handleLogs = true
        }
    }

    protected open fun createLogWriter(settings: LoggerSettings): ElasticsearchLogWriter {
        return ElasticsearchLogWriter(settings, errorHandler)
    }

    protected open fun retrieveKubernetesInfo() {
        // do this non blocking as retrieving kubernetes info takes approximately 4 seconds -> we cannot block application startup for that long time
        KubernetesInfoRetriever().retrieveKubernetesInfoAsync {
            this.kubernetesInfo = it

            startLogRecordProcessing()
        }
    }

    protected open fun startLogRecordProcessing() {
        this.handleLogs = true

        unhandledLogs.forEach { handleRecord(it) } // do not call handle() as otherwise may a wrong MDC gets added to record

        unhandledLogs.clear()
    }


    open fun handle(record: LogRecord) {
        if (record.mdc == null) { // if logging framework does not support MDC, set it here (is that a valid use case?)
            val mdc = MDC.getCopyOfContextMap()
            record.mdc = if (mdc.isEmpty()) null else mdc // do not set mdc to an empty map, LogWriter will not filter that
        }

        if (handleLogs) {
            handleRecord(record)
        } else {
            unhandledLogs.add(record)
        }
    }

    protected open fun handleRecord(record: LogRecord) {
        try {
            record.kubernetesInfo = kubernetesInfo

            logWriter.writeRecord(record)
        } catch (e: Exception) {
            showError("Could not process log record $record", e)
        }
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