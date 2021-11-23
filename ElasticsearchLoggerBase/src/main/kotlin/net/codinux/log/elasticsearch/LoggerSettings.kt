package net.codinux.log.elasticsearch


open class LoggerSettings(
        open var enabled: Boolean = EnabledDefaultValue,

        open var host: String = HostNotSetValue,

        open var indexName: String = IndexNameDefaultValue,

        open var messageFieldName: String = MessageDefaultFieldName,

        open var timestampFormat: TimestampFormat = TimestampDefaultFormat,
        open var timestampFieldName: String = TimestampDefaultFieldName,

        open var includeLogLevel: Boolean = IncludeLogLevelDefaultValue,
        open var logLevelFieldName: String = LogLevelDefaultFieldName,

        open var includeLogger: Boolean = IncludeLoggerDefaultValue,
        open var loggerFieldName: String = LoggerDefaultFieldName,

        open var includeLoggerName: Boolean = IncludeLoggerNameDefaultValue,
        open var loggerNameFieldName: String = LoggerNameDefaultFieldName,

        open var includeThreadName: Boolean = IncludeThreadNameDefaultValue,
        open var threadNameFieldName: String = ThreadNameDefaultFieldName,

        open var includeHostName: Boolean = IncludeHostNameDefaultValue,
        open var hostNameFieldName: String = HostNameDefaultFieldName,

        open var includeStacktrace: Boolean = IncludeStacktraceDefaultValue,
        open var stacktraceFieldName: String = StacktraceDefaultFieldName,

        open var includeMdc: Boolean = IncludeMdcDefaultValue,
        open var mdcKeysPrefix: String? = MdcFieldsPrefixDefaultValue,

        open var maxLogRecordsPerBatch: Int = MaxLogRecordsPerBatchDefaultValue,
        open var maxBufferedLogRecords: Int = MaxBufferedLogRecordsDefaultValue,
        open var sendLogRecordsPeriodMillis: Long = SendLogRecordsPeriodMillisDefaultValue
) {

    companion object {

        const val EnabledDefaultValue = true

        const val HostNotSetValue = "null"

        const val IndexNameDefaultValue = "log"

        const val MessageDefaultFieldName = "message"

        @JvmStatic
        val TimestampDefaultFormat = TimestampFormat.FORMATTED_DATE_TIME
        const val TimestampDefaultFieldName = "@timestamp"

        const val IncludeLogLevelDefaultValue = true
        const val LogLevelDefaultFieldName = "level"

        const val IncludeLoggerDefaultValue = true
        const val LoggerDefaultFieldName = "logger"

        const val IncludeLoggerNameDefaultValue = false
        const val LoggerNameDefaultFieldName = "loggername"

        const val IncludeThreadNameDefaultValue = true
        const val ThreadNameDefaultFieldName = "thread"

        const val IncludeHostNameDefaultValue = true
        const val HostNameDefaultFieldName = "host"

        const val IncludeStacktraceDefaultValue = true
        const val StacktraceDefaultFieldName = "stacktrace"

        const val IncludeMdcDefaultValue = true
        const val MdcFieldsPrefixDefaultValue: String = ""

        const val MaxLogRecordsPerBatchDefaultValue = 100
        const val MaxBufferedLogRecordsDefaultValue = 2000
        const val SendLogRecordsPeriodMillisDefaultValue = 100L

    }

}