package net.codinux.log.elasticsearch


open class LoggerSettings(
        open var enabled: Boolean = EnabledDefaultValue,

        open var host: String = HostNotSetValue,

        open var indexName: String = IndexNameDefaultValue,

        open var messageFieldName: String = MessageDefaultFieldName,

        open var timestampFieldName: String = TimestampDefaultFieldName,
        open var timestampFormat: TimestampFormat = TimestampDefaultFormat,

        open var includeLogLevel: Boolean = IncludeLogLevelDefaultValue,
        open var logLevelFieldName: String = LogLevelDefaultFieldName,

        open var includeLoggerName: Boolean = IncludeLoggerNameDefaultValue,
        open var loggerNameFieldName: String = LoggerNameDefaultFieldName,

        open var includeThreadName: Boolean = IncludeThreadNameDefaultValue,
        open var threadNameFieldName: String = ThreadNameDefaultFieldName,

        open var includeHostName: Boolean = IncludeHostNameDefaultValue,
        open var hostNameFieldName: String = HostNameDefaultFieldName,

        open var includeStacktrace: Boolean = IncludeStacktraceDefaultValue,
        open var stacktraceFieldName: String = StacktraceDefaultFieldName,

        open var includeMdc: Boolean = IncludeMdcDefaultValue,

        open var maxLogRecordsPerBatch: Int = MaxLogRecordsPerBatchDefaultValue,
        open var maxBufferedLogRecords: Int = MaxBufferedLogRecordsDefaultValue,
        open var sendLogRecordsPeriodMillis: Long = SendLogRecordsPeriodMillisDefaultValue
) {

    companion object {

        const val EnabledDefaultValue = true

        const val HostNotSetValue = "null"

        const val IndexNameDefaultValue = "log"

        const val MessageDefaultFieldName = "message"

        const val TimestampDefaultFieldName = "@timestamp"

        val TimestampDefaultFormat = TimestampFormat.FORMATTED_DATE_TIME

        const val IncludeLogLevelDefaultValue = true
        const val LogLevelDefaultFieldName = "level"

        const val IncludeLoggerNameDefaultValue = true
        const val LoggerNameDefaultFieldName = "logger"

        const val IncludeThreadNameDefaultValue = true
        const val ThreadNameDefaultFieldName = "thread"

        const val IncludeHostNameDefaultValue = true
        const val HostNameDefaultFieldName = "host"

        const val IncludeStacktraceDefaultValue = true
        const val StacktraceDefaultFieldName = "stacktrace"

        const val IncludeMdcDefaultValue = true

        const val MaxLogRecordsPerBatchDefaultValue = 200
        const val MaxBufferedLogRecordsDefaultValue = 2000
        const val SendLogRecordsPeriodMillisDefaultValue = 100L

    }

}