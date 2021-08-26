package net.codinux.log.elasticsearch

import com.fasterxml.jackson.databind.ObjectMapper
import net.codinux.log.elasticsearch.errorhandler.ErrorHandler
import net.codinux.log.elasticsearch.errorhandler.OnlyOnceErrorHandler
import net.codinux.log.elasticsearch.errorhandler.StdErrErrorHandler
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.client.RequestOptions
import java.io.PrintWriter
import java.time.ZonedDateTime
import java.time.ZoneOffset
import org.elasticsearch.client.RestClient
import org.apache.http.HttpHost
import org.elasticsearch.action.bulk.BulkRequest
import java.io.StringWriter
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.Exception
import kotlin.concurrent.thread


open class ElasticsearchLogWriter(
        protected open val settings: LoggerSettings,
        protected open val errorHandler: ErrorHandler = OnlyOnceErrorHandler(StdErrErrorHandler())
) : LogWriter {

    companion object {
        val TimestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSZ")
    }


    protected open val recordsQueue = CopyOnWriteArrayList<String>()

    protected open val restClient: RestHighLevelClient = RestHighLevelClient(RestClient.builder(HttpHost.create(settings.host)))

    protected open val mapper = ObjectMapper()

    protected open val handleRecords = AtomicBoolean(true)

    protected open val workerThread = thread(name = "Send logs to Elasticsearch") {
        sendData()
    }


    init {
        println("Logging to index '${settings.indexName}' on host ${settings.host}")
    }


    private fun sendData() {
        // TODO: may simple use a REST client and create POST oneself

        while (handleRecords.get()) {
            if (recordsQueue.isNotEmpty()) {
                try {
                    val recordsToSend = calculateRecordsToSend()
                    val bulkRequest = createBulkRequest(recordsToSend)

                    val response = restClient.bulk(bulkRequest, RequestOptions.DEFAULT)
                    if (response.hasFailures()) {
                        recordsQueue.addAll(recordsQueue.size, recordsToSend) // append again at end of queue
                        errorHandler.showError("Could not send log records to Elasticsearch: ${response.status()} ${response.buildFailureMessage()}", null)
                    }
                } catch (e: Exception) {
                    errorHandler.showError("Could not send calculate next batch to send to Elasticsearch", e)
                }
            }

            TimeUnit.MILLISECONDS.sleep(settings.sendLogRecordsPeriodMillis)
        }
    }

    private fun createBulkRequest(recordsToSend: List<String>): BulkRequest {
        val bulkRequest = BulkRequest(settings.indexName)

        recordsToSend.forEach { recordJson ->
            val request = IndexRequest(settings.indexName)
            request.source(recordJson, XContentType.JSON)
            bulkRequest.add(request)
        }

        return bulkRequest
    }

    private fun calculateRecordsToSend(): List<String> {
        val size = recordsQueue.size

        if (size <= settings.maxLogRecordsPerBatch) {
            val recordsToSend = ArrayList(recordsQueue)

            recordsQueue.clear()

            return recordsToSend
        }
        else {
            val fromIndex = size - settings.maxLogRecordsPerBatch
            val recordsToSend = ArrayList(recordsQueue.subList(fromIndex, size)) // make a copy

            while (recordsQueue.size > fromIndex) {
                recordsQueue.removeAt(fromIndex) // do not call removeAll() as if other records have the same JSON string than all matching strings get removed
            }

            return recordsToSend;
        }
    }


    override fun writeRecord(record: LogRecord) {
        try {
            val esRecord = createEsRecord(record)

            val recordJson = mapper.writeValueAsString(esRecord)

            recordsQueue.add(recordJson)

            while (recordsQueue.size > settings.maxBufferedLogRecords) {
                recordsQueue.removeLast()
            }
        } catch (e: Exception) {
            errorHandler.showError("Could not queue record $record to send to Elasticsearch", e)
        }
    }


    override fun close() {
        try {
            handleRecords.set(false)

            workerThread.join(100)
        } catch (e: Exception) {
            System.err.println("Could not stop ElasticsearchLogWriter")
        }
    }


    protected open fun createEsRecord(record: LogRecord): Map<String, Any?> {
        val esRecord = mutableMapOf<String, Any>()

        esRecord[settings.messageFieldName] = record.message
        esRecord[settings.timestampFieldName] = formatTimestamp(record.timestamp)

        conditionallyAdd(esRecord, settings.includeLogLevel, settings.logLevelFieldName, record.level)
        conditionallyAdd(esRecord, settings.includeLogger, settings.loggerFieldName, record.logger)

        if (settings.includeLoggerName) { // loggerName is in most cases full qualified class name including packages, try to extract only name of class
            var loggerName = record.logger
            val indexOfDot = loggerName.lastIndexOf('.')
            if (indexOfDot >= 0) {
                loggerName = loggerName.substring(indexOfDot + 1)
            }
            esRecord[settings.loggerFieldName] = loggerName
        }

        conditionallyAdd(esRecord, settings.includeThreadName, settings.threadNameFieldName, record.threadName)
        conditionallyAdd(esRecord, settings.includeHostName, settings.hostNameFieldName, record.host)

        conditionallyAdd(esRecord, settings.includeStacktrace && record.exception != null, settings.stacktraceFieldName)
                { extractStacktrace(record) }

        if (settings.includeMdc && record.mdc != null) {
            record.mdc?.let { mdc ->
                for ((key, value) in mdc) {
                    esRecord.putIfAbsent(key, value)
                }
            }
        }

        return esRecord
    }

    protected open fun conditionallyAdd(record: MutableMap<String, Any>, include: Boolean, fieldName: String, valueSupplier: () -> Any) {
        if (include) {
            conditionallyAdd(record, include, fieldName, valueSupplier.invoke())
        }
    }

    protected open fun conditionallyAdd(record: MutableMap<String, Any>, include: Boolean, fieldName: String, value: Any) {
        if (include) {
            record[fieldName] = value
        }
    }

    protected open fun extractStacktrace(record: LogRecord): String {
        val writer = StringWriter()
        record.exception?.printStackTrace(PrintWriter(writer))

        return writer.toString()
    }

    protected open fun formatTimestamp(timestamp: Instant): Any {
        if (settings.timestampFormat === TimestampFormat.MILLIS_SINCE_EPOCH) {
            return timestamp.toEpochMilli()
        }

        return ZonedDateTime.ofInstant(timestamp, ZoneOffset.UTC).format(TimestampFormatter)
    }

}