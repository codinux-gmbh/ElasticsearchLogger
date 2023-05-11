package net.codinux.log.elasticsearch

import net.codinux.log.elasticsearch.converter.ElasticsearchIndexNameConverter
import net.codinux.log.elasticsearch.errorhandler.ErrorHandler
import net.codinux.log.elasticsearch.errorhandler.OnlyOnceErrorHandler
import net.codinux.log.elasticsearch.errorhandler.StdErrErrorHandler
import net.codinux.log.elasticsearch.kubernetes.KubernetesInfo
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.opensearch.client.RestClient
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.core.BulkRequest
import org.opensearch.client.opensearch.core.BulkResponse
import org.opensearch.client.transport.rest_client.RestClientTransport
import java.io.PrintWriter
import java.io.StringWriter
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
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


    protected open val recordsQueue = CopyOnWriteArrayList<Map<String, Any>>()

    protected open val indexNameConverter = ElasticsearchIndexNameConverter()

    protected open val esClient = OpenSearchClient(RestClientTransport(createRestClient(), JacksonJsonpMapper()))

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


    protected open fun createRestClient(): RestClient {
        return RestClient.builder(HttpHost.create(settings.host)).setHttpClientConfigCallback { clientBuilder ->
            clientBuilder.apply {
                if (settings.username != null && settings.password != null) {
                    val credentialsProvider = BasicCredentialsProvider()
                    credentialsProvider.setCredentials(AuthScope.ANY, UsernamePasswordCredentials(settings.username, settings.password))

                    clientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                }

                if (settings.disableCertificateCheck) {
                    clientBuilder.setSSLContext(createDisableCertificateCheckSslContext())
                }
            }
        }.build()
    }

    protected open fun createDisableCertificateCheckSslContext(): SSLContext {
        val trustAllCertificatesTrustManager = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate?> = arrayOf()
            override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) { }
            override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {
                // trust all certificates
            }
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustAllCertificatesTrustManager), null)

        return sslContext
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

                if (response?.errors() == true) {
                    val errors = response.items().filter { it.error() != null }.joinToString { "${it.status()}: ${it.error()?.reason()}" }
                    errorHandler.logError("Could not send log records to Elasticsearch: $errors")

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

    protected open fun sendRecords(recordsToSend: List<Map<String, Any>>): BulkResponse? {
        return try {
            esClient.bulk { builder ->
                createBulkRequest(builder, recordsToSend)
            }
        } catch (e: Exception) {
            errorHandler.logError("Could not send records with EsClient", e)
            null
        }
    }

    private fun createBulkRequest(builder: BulkRequest.Builder, recordsToSend: List<Map<String, Any>>): BulkRequest.Builder {
        builder.index(getIndexName(settings))

        recordsToSend.forEach { recordJson ->
            builder.operations { op ->
                op.index<Map<String, Any>> { index ->
                    index.document(recordJson)
                }
                op
            }
        }

        return builder
    }

    protected open fun getIndexName(settings: LoggerSettings): String {
        return indexNameConverter.resolvePatterns(settings.indexNamePattern, settings.patternsInIndexName, errorHandler)
    }

    protected open fun calculateRecordsToSend(): List<Map<String, Any>> {
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

    protected open fun reAddFailedItemsToQueue(response: BulkResponse, sentRecords: List<Map<String, Any>>) {
        response.items().forEachIndexed { index, item ->
            if (item.error() !=  null) {
                val failedRecord = sentRecords[index]
                recordsQueue.add(recordsQueue.size, failedRecord)
            }
        }
    }

    protected open fun reAddSentItemsToQueue(sentRecords: List<Map<String, Any>>) {
        recordsQueue.addAll(recordsQueue.size, sentRecords)
    }


    override fun writeRecord(record: LogRecord) {
        try {
            val recordJson = mapToEsRecord(record)

            if (isClosed.get()) { // don't know if that ever will be the case: if close has been called, send all records immediately, don't hesitate anymore
                sendRecords(listOf(recordJson))
                return
            }

            synchronized(recordsQueue) {
                recordsQueue.add(recordJson)

                while (recordsQueue.size > settings.maxBufferedLogRecords) {
                    // TODO: log warning that buffer size has been exceeded
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
        addIfNotNull(esRecord, prefix, "restartCount", info.restartCount)
        addIfNotNull(esRecord, prefix, "containerName", info.containerName)
        addIfNotNull(esRecord, prefix, "containerId", info.containerId)
        addIfNotNull(esRecord, prefix, "imageName", info.imageName)
        addIfNotNull(esRecord, prefix, "imageId", info.imageId)
        addIfNotNull(esRecord, prefix, "nodeIp", info.nodeIp)
        addIfNotNull(esRecord, prefix, "node", info.nodeName)

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