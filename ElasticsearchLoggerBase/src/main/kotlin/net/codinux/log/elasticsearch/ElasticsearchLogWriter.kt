package net.codinux.log.elasticsearch

import com.fasterxml.jackson.databind.ObjectMapper
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.client.RequestOptions
import java.io.PrintWriter
import java.time.ZonedDateTime
import java.time.ZoneOffset
import org.elasticsearch.client.RestClient
import org.apache.http.HttpHost
import java.io.StringWriter
import java.lang.Exception
import java.time.Instant
import java.time.format.DateTimeFormatter


open class ElasticsearchLogWriter(private val settings: LoggerSettings) : LogWriter {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    }


    protected val restClient: RestHighLevelClient = RestHighLevelClient(RestClient.builder(HttpHost.create(settings.host)))

    protected val mapper = ObjectMapper()


    override fun writeRecord(record: LogRecord) {
        try {
            val esRecord = createEsRecord(record)

            val recordJson = mapper.writeValueAsString(esRecord)

            val request = IndexRequest(settings.indexName)
            request.source(recordJson, XContentType.JSON)

            restClient.index(request, RequestOptions.DEFAULT)
        } catch (e: Exception) {
            // TODO: what to do in this case? retry?
            System.err.println("Could not send record " + record + " to Elasticsearch: " + e.message)
            e.printStackTrace()
        }
    }

    protected open fun createEsRecord(record: LogRecord): Map<String, Any?> {
        val esRecord = mutableMapOf<String, Any>()

        esRecord[settings.messageFieldName] = record.message
        esRecord[settings.timestampFieldName] = formatTimestamp(record.timestamp)

        conditionallyAdd(esRecord, settings.includeLogLevel, settings.logLevelFieldName, record.level)
        conditionallyAdd(esRecord, settings.includeLoggerName, settings.loggerNameFieldName, record.logger)

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

        return ZonedDateTime.ofInstant(timestamp, ZoneOffset.UTC).format(DATE_FORMATTER)
    }

}