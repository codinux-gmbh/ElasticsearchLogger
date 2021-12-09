package net.codinux.log.elasticsearch

import net.codinux.log.elasticsearch.converter.ElasticsearchIndexNameConverter
import net.codinux.log.elasticsearch.errorhandler.ErrorHandler


abstract class PropertyProviderBase {

    companion object {
        const val FIELD_NAME_KEY = ".name"
        const val INCLUDE_NAME_KEY = ".include"
    }


    abstract fun getProperty(propertyName: String): String?


    protected open val indexNameConverter = ElasticsearchIndexNameConverter()

    protected open val timestampResolutionConverter = TimestampResolutionConverter()


    open fun extractSettings(errorHandler: ErrorHandler): LoggerSettings {
        val indexName = indexNameConverter.createIndexName(getElasticsearchPropertyOr("index", LoggerSettings.IndexNameDefaultValue), errorHandler)

        return LoggerSettings(
            getBooleanProperty("enable", LoggerSettings.EnabledDefaultValue),

                getElasticsearchPropertyOr("host", LoggerSettings.HostNotSetValue),

                indexName,

                getFieldName("message", LoggerSettings.MessageDefaultFieldName),

                getTimestampResolution() ?: LoggerSettings.TimestampDefaultResolution,
                getFieldName("timestamp", LoggerSettings.TimestampDefaultFieldName),

                getIncludeField("level", LoggerSettings.IncludeLogLevelDefaultValue),
                getFieldName("level", LoggerSettings.LogLevelDefaultFieldName),

                getIncludeField("logger", LoggerSettings.IncludeLoggerDefaultValue),
                getFieldName("logger", LoggerSettings.LoggerDefaultFieldName),

                getIncludeField("loggername", LoggerSettings.IncludeLoggerNameDefaultValue),
                getFieldName("loggername", LoggerSettings.LoggerNameDefaultFieldName),

                getIncludeField("thread", LoggerSettings.IncludeThreadNameDefaultValue),
                getFieldName("thread", LoggerSettings.ThreadNameDefaultFieldName),

                getIncludeField("hostName", LoggerSettings.IncludeHostNameDefaultValue),
                getFieldName("hostName", LoggerSettings.HostNameDefaultFieldName),

                getIncludeField("stacktrace", LoggerSettings.IncludeStacktraceDefaultValue),
                getFieldName("stacktrace", LoggerSettings.StacktraceDefaultFieldName),

                getIncludeField("mdc", LoggerSettings.IncludeMdcDefaultValue),
                getNullableStringProperty("mdcprefix", LoggerSettings.MdcFieldsPrefixDefaultValue),

                getIncludeField("kubernetes", LoggerSettings.IncludeKubernetesInfoDefaultValue),
                getNullableStringProperty("kubernetesprefix", LoggerSettings.KubernetesFieldsPrefixDefaultValue),

                getIncludeField("kubernetes-labels", LoggerSettings.IncludeKubernetesLabelsDefaultValue),
                getNullableStringProperty("kuberneteslabelsprefix", LoggerSettings.KubernetesLabelsPrefixDefaultValue),

                getIncludeField("kubernetes-annotations", LoggerSettings.IncludeKubernetesAnnotationsDefaultValue),
                getNullableStringProperty("kubernetesannotationsprefix", LoggerSettings.KubernetesAnnotationsPrefixDefaultValue),

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

    protected open fun getNullableStringProperty(propertyName: String, defaultValue: String?): String? {
        return getElasticsearchProperty(propertyName) ?: defaultValue
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

    protected open fun getTimestampResolution(): TimestampResolution? {
        return getElasticsearchProperty("timestampresolution")?.let { value ->
            timestampResolutionConverter.convert(value)
        }
    }

    protected open fun getElasticsearchPropertyOr(propertyName: String, defaultValue: String): String {
        return getElasticsearchProperty(propertyName) ?: defaultValue
    }

    protected open fun getElasticsearchProperty(propertyName: String): String? {
        return getProperty("log.elasticsearch.$propertyName")
    }

}