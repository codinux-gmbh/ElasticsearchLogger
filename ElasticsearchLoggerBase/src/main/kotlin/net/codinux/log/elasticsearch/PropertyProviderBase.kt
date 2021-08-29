package net.codinux.log.elasticsearch


abstract class PropertyProviderBase {

    companion object {
        const val FIELD_NAME_KEY = ".name"
        const val INCLUDE_NAME_KEY = ".include"

        const val MILLIS_SINCE_EPOCH_PROPERTY_VALUE = "MILLIS"
        const val FORMATTED_DATE_TIME_PROPERTY_VALUE = "FORMATTED"
    }


    abstract fun getProperty(propertyName: String): String?


    open fun extractSettings(): LoggerSettings {
        return LoggerSettings(
            getBooleanProperty("enable", LoggerSettings.EnabledDefaultValue),

                getElasticsearchPropertyOr("host", LoggerSettings.HostNotSetValue),

                getElasticsearchPropertyOr("index", LoggerSettings.IndexNameDefaultValue),

                getFieldName("message", LoggerSettings.MessageDefaultFieldName),

                getTimestampFormat() ?: LoggerSettings.TimestampDefaultFormat,
                getFieldName("timestamp", LoggerSettings.TimestampDefaultFieldName),

                getIncludeField("level", LoggerSettings.IncludeLogLevelDefaultValue),
                getFieldName("level", LoggerSettings.LogLevelDefaultFieldName),

                getIncludeField("logger", LoggerSettings.IncludeLoggerDefaultValue),
                getFieldName("logger", LoggerSettings.LoggerDefaultFieldName),

                getIncludeField("loggername", LoggerSettings.IncludeLoggerNameDefaultValue),
                getFieldName("loggername", LoggerSettings.LoggerNameDefaultFieldName),

                getIncludeField("hostName", LoggerSettings.IncludeHostNameDefaultValue),
                getFieldName("hostName", LoggerSettings.HostNameDefaultFieldName),

                getIncludeField("thread", LoggerSettings.IncludeThreadNameDefaultValue),
                getFieldName("thread", LoggerSettings.ThreadNameDefaultFieldName),

                getIncludeField("stacktrace", LoggerSettings.IncludeStacktraceDefaultValue),
                getFieldName("stacktrace", LoggerSettings.StacktraceDefaultFieldName),

                getIncludeField("mdc", LoggerSettings.IncludeMdcDefaultValue),

                getIntProperty("maxlogrecordperbatch", LoggerSettings.MaxLogRecordsPerBatchDefaultValue),
                getIntProperty("maxbufferedlogrecords", LoggerSettings.MaxBufferedLogRecordsDefaultValue),
                getLongProperty("sendlogrecordsperiodmillis", LoggerSettings.SendLogRecordsPeriodMillisDefaultValue)
        )
    }


    protected open fun getIncludeField(fieldName: String, defaultValue: Boolean): Boolean {
        return getBooleanProperty(fieldName + INCLUDE_NAME_KEY, defaultValue)
    }

    protected open fun getFieldName(fieldName: String, defaultValue: String): String {
        return getElasticsearchProperty(fieldName + FIELD_NAME_KEY) ?: defaultValue
    }

    protected open fun getIntProperty(propertyName: String, defaultValue: Int): Int {
        return getElasticsearchProperty(propertyName)?.let { it.toInt() } ?: defaultValue
    }

    protected open fun getLongProperty(propertyName: String, defaultValue: Long): Long {
        return getElasticsearchProperty(propertyName)?.let { it.toLong() } ?: defaultValue
    }

    protected open fun getBooleanProperty(propertyName: String, defaultValue: Boolean): Boolean {
        return getElasticsearchProperty(propertyName)?.let { it.toBoolean() } ?: defaultValue
    }

    protected open fun getTimestampFormat(): TimestampFormat? {
        return getElasticsearchProperty("timestampformat")?.let { value ->
            val upperCase = value.toUpperCase()

            if (upperCase == MILLIS_SINCE_EPOCH_PROPERTY_VALUE) {
                return TimestampFormat.MILLIS_SINCE_EPOCH
            } else if (upperCase == FORMATTED_DATE_TIME_PROPERTY_VALUE) {
                return TimestampFormat.FORMATTED_DATE_TIME
            }

            return TimestampFormat.valueOf(upperCase)
        }
    }

    protected open fun getElasticsearchPropertyOr(propertyName: String, defaultValue: String): String {
        return getElasticsearchProperty(propertyName) ?: defaultValue
    }

    protected open fun getElasticsearchProperty(propertyName: String): String? {
        return getProperty("log.elasticsearch.$propertyName")
    }

}