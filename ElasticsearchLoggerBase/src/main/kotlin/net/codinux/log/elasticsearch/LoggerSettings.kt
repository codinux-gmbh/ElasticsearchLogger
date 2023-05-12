package net.codinux.log.elasticsearch


open class LoggerSettings(
    open var enabled: Boolean = EnabledDefaultValue,

    open var host: String = HostNotSetValue,

    open var username: String? = UsernameDefaultValue,
    open var password: String? = PasswordDefaultValue,

    open var disableCertificateCheck: Boolean = DisableCertificateCheckDefaultValue,

    open var indexNamePattern: String = IndexNameDefaultValue,
    open var patternsInIndexName: List<MatchResult> = listOf(),

    open var messageFieldName: String = MessageDefaultFieldName,

    open var timestampResolution: TimestampResolution = TimestampDefaultResolution,
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
    open var stacktraceMaxFieldLength: Int = StacktraceMaxFieldLengthDefaultValue,

    open var includeMdc: Boolean = IncludeMdcDefaultValue,
    open var mdcKeysPrefix: String? = MdcFieldsPrefixDefaultValue,

    open var includeMarker: Boolean = IncludeMarkerDefaultValue,
    open var markerFieldName: String = MarkerDefaultFieldName,

    open var includeNdc: Boolean = IncludeNdcDefaultValue,
    open var ndcFieldName: String = NdcDefaultFieldName,

    open var includeKubernetesInfo: Boolean = IncludeKubernetesInfoDefaultValue,
    open var kubernetesFieldsPrefix: String? = KubernetesFieldsPrefixDefaultValue,

    open var includeKubernetesLabels: Boolean = IncludeKubernetesLabelsDefaultValue,
    open var kubernetesLabelsPrefix: String? = KubernetesLabelsPrefixDefaultValue,

    open var includeKubernetesAnnotations: Boolean = IncludeKubernetesAnnotationsDefaultValue,
    open var kubernetesAnnotationsPrefix: String? = KubernetesAnnotationsPrefixDefaultValue,

    open var maxLogRecordsPerBatch: Int = MaxLogRecordsPerBatchDefaultValue,
    open var maxBufferedLogRecords: Int = MaxBufferedLogRecordsDefaultValue,
    open var sendLogRecordsPeriodMillis: Long = SendLogRecordsPeriodMillisDefaultValue
) {

    companion object {

        const val EnabledDefaultValue = true
        const val EnabledDefaultValueString = "true"

        const val HostNotSetValue = "null"

        val UsernameDefaultValue: String? = null
        const val UsernameDefaultValueString = "null"

        val PasswordDefaultValue: String? = null
        const val PasswordDefaultValueString = "null"

        const val DisableCertificateCheckDefaultValue: Boolean = false
        const val DisableCertificateCheckDefaultValueString = "false"

        const val IndexNameDefaultValue = "logs"

        const val MessageDefaultFieldName = "message"

        @JvmStatic
        val TimestampDefaultResolution = TimestampResolution.Milliseconds
        const val TimestampDefaultFieldName = "@timestamp"

        const val IncludeLogLevelDefaultValue = true
        const val IncludeLogLevelDefaultValueString = "true"
        const val LogLevelDefaultFieldName = "level"

        const val IncludeLoggerDefaultValue = true
        const val IncludeLoggerDefaultValueString = "true"
        const val LoggerDefaultFieldName = "logger"

        const val IncludeLoggerNameDefaultValue = false
        const val IncludeLoggerNameDefaultValueString = "false"
        const val LoggerNameDefaultFieldName = "loggername"

        const val IncludeThreadNameDefaultValue = true
        const val IncludeThreadNameDefaultValueString = "true"
        const val ThreadNameDefaultFieldName = "thread"

        const val IncludeHostNameDefaultValue = true
        const val IncludeHostNameDefaultValueString = "true"
        const val HostNameDefaultFieldName = "host"

        const val IncludeStacktraceDefaultValue = true
        const val IncludeStacktraceDefaultValueString = "true"
        const val StacktraceDefaultFieldName = "stacktrace"
        const val StacktraceMaxFieldLengthDefaultValue = 32766 - 100 // subtract a little buffer
        const val StacktraceMaxFieldLengthDefaultValueString = StacktraceMaxFieldLengthDefaultValue.toString()

        const val IncludeMdcDefaultValue = true
        const val IncludeMdcDefaultValueString = "true"
        const val MdcFieldsPrefixDefaultValue: String = "mdc"

        const val IncludeMarkerDefaultValue = false
        const val IncludeMarkerDefaultValueString = "false"
        const val MarkerDefaultFieldName: String = "marker"

        const val IncludeNdcDefaultValue = false
        const val IncludeNdcDefaultValueString = "false"
        const val NdcDefaultFieldName: String = "ndc"

        const val IncludeKubernetesInfoDefaultValue = false
        const val IncludeKubernetesInfoDefaultValueString = "false"
        const val KubernetesFieldsPrefixDefaultValue: String = "k8s"

        const val IncludeKubernetesLabelsDefaultValue = false
        const val IncludeKubernetesLabelsDefaultValueString = "false"
        const val KubernetesLabelsPrefixDefaultValue: String = "label"

        const val IncludeKubernetesAnnotationsDefaultValue = false
        const val IncludeKubernetesAnnotationsDefaultValueString = "false"
        const val KubernetesAnnotationsPrefixDefaultValue: String = "annotation"

        const val MaxLogRecordsPerBatchDefaultValue = 100
        const val MaxBufferedLogRecordsDefaultValue = 10_000
        const val SendLogRecordsPeriodMillisDefaultValue = 100L

    }

}