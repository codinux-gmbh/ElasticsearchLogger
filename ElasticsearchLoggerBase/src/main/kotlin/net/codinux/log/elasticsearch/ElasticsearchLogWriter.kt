package net.codinux.log.elasticsearch

import com.fasterxml.jackson.databind.ObjectMapper
import net.codinux.log.elasticsearch.converter.ElasticsearchIndexNameConverter
import net.codinux.log.elasticsearch.errorhandler.ErrorHandler
import net.codinux.log.elasticsearch.errorhandler.OnlyOnceErrorHandler
import net.codinux.log.elasticsearch.errorhandler.StdErrErrorHandler
import net.codinux.log.elasticsearch.kubernetes.KubernetesInfo
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.client.RequestOptions
import java.io.PrintWriter
import org.elasticsearch.client.RestClient
import org.apache.http.HttpHost
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.bulk.BulkResponse
import java.io.StringWriter
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.Exception
import kotlin.concurrent.thread


open class ElasticsearchLogWriter @JvmOverloads constructor(
    protected open val settings: LoggerSettings,
    protected open val errorHandler: ErrorHandler = OnlyOnceErrorHandler(StdErrErrorHandler())
) : LogWriter {

    companion object {
        val MillisTimestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        val MicrosTimestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ")
        val NanosTimestampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSZ")
    }


    protected open val recordsQueue = CopyOnWriteArrayList<String>()

    protected open val indexNameConverter = ElasticsearchIndexNameConverter()

    protected open val restClient: RestHighLevelClient = RestHighLevelClient(RestClient.builder(HttpHost.create(settings.host)))

    protected open val mapper = ObjectMapper()

    protected open val handleRecords = AtomicBoolean(true)

    protected open val isClosed = AtomicBoolean(false)

    protected open val timestampFormatter = when (settings.timestampResolution) {
        TimestampResolution.Milliseconds -> MillisTimestampFormatter
        TimestampResolution.Microseconds -> MicrosTimestampFormatter
        TimestampResolution.Nanoseconds -> NanosTimestampFormatter
    }

    protected open val workerThread = thread(name = "Send logs to Elasticsearch") {
        sendData()
    }


    init {
        errorHandler.logInfo("Logging to index '${settings.indexNamePattern}' on host ${settings.host}")
    }


    protected open fun sendData() {
        // TODO: may simple use a REST client and create POST oneself

        while (handleRecords.get()) {
            if (recordsQueue.isNotEmpty()) {
                synchronized(recordsQueue) {
                    sendNextBatch()
                }
            }

            try { TimeUnit.MILLISECONDS.sleep(settings.sendLogRecordsPeriodMillis) } catch (ignored: Exception) { }
        }

        errorHandler.logInfo("sendData() thread has stopped")
    }

    protected open fun sendNextBatch() {
        try {
            val recordsToSend = calculateRecordsToSend()

            try {
                val response = sendRecords(recordsToSend)

                if (response.hasFailures()) {
                    errorHandler.logError("Could not send log records to Elasticsearch: ${response.status()} ${response.buildFailureMessage()}")

                    reAddFailedItemsToQueue(response, recordsToSend)
                }
            } catch (e: Exception) {
                errorHandler.logError("Could not send batch with ${recordsToSend.size} items to Elasticsearch", e)

                reAddSentItemsToQueue(recordsToSend)
            }
        } catch (e: Exception) {
            errorHandler.logError("Could not calculate next batch to send to Elasticsearch", e)
        }
    }

    protected open fun sendRecords(recordsToSend: List<String>): BulkResponse {
        val bulkRequest = createBulkRequest(recordsToSend)

        return restClient.bulk(bulkRequest, RequestOptions.DEFAULT)
    }

    protected open fun createBulkRequest(recordsToSend: List<String>): BulkRequest {
        val indexName = getIndexName(settings)
        val bulkRequest = BulkRequest(indexName)

        recordsToSend.forEach { recordJson ->
            val request = IndexRequest(indexName)
            request.source(recordJson, XContentType.JSON)
            bulkRequest.add(request)
        }

        return bulkRequest
    }

    protected open fun getIndexName(settings: LoggerSettings): String {
        return indexNameConverter.resolvePatterns(settings.indexNamePattern, settings.patternsInIndexName, errorHandler)
    }

    protected open fun calculateRecordsToSend(): List<String> {
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

            return recordsToSend
        }
    }

    protected open fun reAddFailedItemsToQueue(response: BulkResponse, sentRecords: List<String>) {
        response.items.forEachIndexed { index, item ->
            if (item.isFailed) {
                val failedRecord = sentRecords[index]
                recordsQueue.add(recordsQueue.size, failedRecord)
            }
        }
    }

    protected open fun reAddSentItemsToQueue(sentRecords: List<String>) {
        recordsQueue.addAll(recordsQueue.size, sentRecords)
    }


    override fun writeRecord(record: LogRecord) {
        try {
            val recordJson = createEsRecordJson(record)

            if (isClosed.get()) { // don't know if that ever will be the case: if close has been called, send all records immediately, don't hesitate anymore
                sendRecords(listOf(recordJson))
                return
            }

            synchronized(recordsQueue) {
                recordsQueue.add(recordJson)

                while (recordsQueue.size > settings.maxBufferedLogRecords) {
                    recordsQueue.removeLast()
                }
            }
        } catch (e: Exception) {
            errorHandler.logError("Could not queue record $record to send to Elasticsearch", e)
        }
    }


    override fun close() {
        try {
            isClosed.set(true)
            handleRecords.set(false)

            val records = ArrayList(recordsQueue)
            recordsQueue.clear()

            sendRecords(records) // send all records immediately, don't hesitate anymore

            workerThread.join(100)
        } catch (e: Exception) {
            errorHandler.logError("Could not stop ElasticsearchLogWriter")
        }
    }


    protected open fun createEsRecordJson(record: LogRecord): String {
        val esRecord = mapToEsRecord(record)

        return mapper.writeValueAsString(esRecord)
    }

    protected open fun mapToEsRecord(record: LogRecord): Map<String, Any> {
        val esRecord = mutableMapOf<String, Any>()

        var message = record.message
        record.exception?.let { exception -> message += ": " + exception.message }

        esRecord[settings.messageFieldName] = message
        esRecord[settings.timestampFieldName] = formatTimestamp(record.timestamp)

        conditionallyAdd(esRecord, settings.includeLogLevel, settings.logLevelFieldName, record.level)
        conditionallyAdd(esRecord, settings.includeLogger, settings.loggerFieldName, record.logger)

        // loggerName is in most cases full qualified class name including packages, try to extract only name of class
        conditionallyAdd(esRecord, settings.includeLoggerName, settings.loggerNameFieldName) { extractLoggerName(record) }

        conditionallyAdd(esRecord, settings.includeThreadName, settings.threadNameFieldName, record.threadName)
        conditionallyAdd(esRecord, settings.includeHostName, settings.hostNameFieldName, record.host)

        conditionallyAdd(esRecord, settings.includeStacktrace && record.exception != null, settings.stacktraceFieldName) { extractStacktrace(record) }

        if (settings.includeMdc && record.mdc != null) {
            record.mdc?.let { mdc ->
                val prefix = determinePrefix(settings.mdcKeysPrefix)

                for ((key, value) in mdc) {
                    esRecord.put(prefix + key, value)
                }
            }
        }

        conditionallyAdd(esRecord, settings.includeMarker && record.marker != null, settings.markerFieldName, record.marker ?: "")
        conditionallyAdd(esRecord, settings.includeNdc && record.ndc != null, settings.ndcFieldName, record.ndc ?: "")

        if (settings.includeKubernetesInfo) {
            record.kubernetesInfo?.let { kubernetesInfo ->
                addKubernetesInfoToEsRecord(esRecord, kubernetesInfo)
            }
        }

        return esRecord
    }

    private fun addKubernetesInfoToEsRecord(esRecord: MutableMap<String, Any>, info: KubernetesInfo) {
        val prefix = determinePrefix(settings.kubernetesFieldsPrefix)

        esRecord.put(prefix + "namespace", info.namespace)
        esRecord.put(prefix + "podName", info.podName)
        esRecord.put(prefix + "podIp", info.podIp)
        esRecord.put(prefix + "startTime", info.startTime)
        addIfNotNull(esRecord, prefix, "podUid", info.podUid)
        esRecord.put(prefix + "restartCount", info.restartCount)
        addIfNotNull(esRecord, prefix, "containerName", info.containerName)
        addIfNotNull(esRecord, prefix, "containerId", info.containerId)
        addIfNotNull(esRecord, prefix, "imageName", info.imageName)
        addIfNotNull(esRecord, prefix, "imageId", info.imageId)
        addIfNotNull(esRecord, prefix, "nodeIp", info.nodeIp)
        addIfNotNull(esRecord, prefix, "node", info.nodeName)
        addIfNotNull(esRecord, prefix, "clusterName", info.clusterName)

        if (settings.includeKubernetesLabels) {
            val labelsPrefix = prefix + determinePrefix(settings.kubernetesLabelsPrefix)

            info.labels.forEach { name, value ->
                esRecord.put(labelsPrefix + convertToEsFieldName(name), value)
            }
        }

        if (settings.includeKubernetesAnnotations) {
            val annotationsPrefix = prefix + determinePrefix(settings.kubernetesAnnotationsPrefix)

            info.annotations.forEach { name, value ->
                esRecord.put(annotationsPrefix + convertToEsFieldName(name), value)
            }
        }
    }

    private fun determinePrefix(prefix: String?): String {
        return if (prefix.isNullOrBlank()) "" else prefix + "."
    }

    protected open fun convertToEsFieldName(name: String): String {
        return name.replace(".", "_") // dots signify sub objects
    }

    protected open fun addIfNotNull(record: MutableMap<String, Any>, fieldNamePrefix: String, fieldName: String, value: Any?) {
        value?.let {
            record[fieldNamePrefix + fieldName] = value
        }
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


    protected open fun formatTimestamp(timestamp: Instant): String {
        return timestamp.atOffset(ZoneOffset.UTC).format(timestampFormatter)
    }

    protected open fun extractStacktrace(record: LogRecord): String {
        val writer = StringWriter()
        record.exception?.printStackTrace(PrintWriter(writer))

        val stackTrace = writer.toString()

        if (stackTrace.length > settings.stacktraceMaxFieldLength) {
            return stackTrace.substring(0, settings.stacktraceMaxFieldLength)
        }

        return stackTrace
    }

    private fun extractLoggerName(record: LogRecord): String {
        var loggerName = record.logger

        val indexOfDot = loggerName.lastIndexOf('.')
        if (indexOfDot >= 0) {
            loggerName = loggerName.substring(indexOfDot + 1)
        }

        return loggerName
    }

}