package net.codinux.log.elasticsearch

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.lang.Exception
import kotlin.concurrent.thread


open class ElasticsearchLogHandler(settings: LoggerSettings, protected open val errorHandler: ErrorHandler = StdErrErrorHandler()) {

    protected val recordQueue = LinkedBlockingQueue<LogRecord>()

    protected val logWriter: LogWriter

    protected val workerThread: Thread

    protected val isRunning = AtomicBoolean(true)


    init {
        logWriter = createLogWriter(settings)

        workerThread = thread(name =  "Elasticsearch Logger") { loggerConsumerThread() }
    }

    protected open fun createLogWriter(settings: LoggerSettings): ElasticsearchLogWriter {
        return ElasticsearchLogWriter(settings)
    }


    open fun handle(record: LogRecord) {
        try {
            recordQueue.add(record)
        } catch (e: Exception) {
            showError("Could not add log record $record to log queue", e)
        }
    }

    open fun flush() {
        val remainingRecords = mutableListOf<LogRecord>()
        recordQueue.drainTo(remainingRecords)

        remainingRecords.forEach { handleRecordSafely(it) }
    }

    open fun close() {
        try {
            isRunning.set(false)
            workerThread.join()

            flush()
        } catch (e: Exception) {
            showError("Could not stop worker thread", e)
        }
    }

    protected open fun loggerConsumerThread() {
        try {
            while (isRunning.get()) {
                val record = recordQueue.take()
                handleRecordSafely(record)
            }
        } catch (e: Exception) {
            showError("Error in consumer thread. What to do, continue or stop logging?", e)
        }
    }

    protected open fun handleRecordSafely(record: LogRecord?) {
        try {
            record?.let { handleRecord(it) }
        } catch (e: Exception) {
            showError("Could not handle log record $record", e)
        }
    }

    protected open fun handleRecord(record: LogRecord) {
        logWriter.writeRecord(record)
    }

    protected open fun showError(message: String, e: Throwable?) {
        errorHandler.showError(message, e)
    }

}