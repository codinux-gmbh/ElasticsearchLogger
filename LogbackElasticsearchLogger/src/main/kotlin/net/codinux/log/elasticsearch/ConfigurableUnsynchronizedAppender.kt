package net.codinux.log.elasticsearch

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.UnsynchronizedAppenderBase


/**
 * As Logback sets configuration directly on appender class and ElasticsearchLogger has a lot of settings, this class wraps all settings and applies them to
 * [settings] property.
 */
abstract class ConfigurableUnsynchronizedAppender(
  protected open val settings: LoggerSettings = LoggerSettings(),
) : UnsynchronizedAppenderBase<ILoggingEvent>() {

  open fun setEnabled(enabled: Boolean) {
    settings.enabled = enabled
  }

  open fun setHost(host: String) {
    settings.host = host
  }

  open fun setUsername(username: String?) {
    settings.username = username
  }

  open fun setPassword(password: String?) {
    settings.password = password
  }

  open fun setIndexName(indexName: String) {
    settings.indexNamePattern = indexName
  }

  open fun setMessageFieldName(messageFieldName: String) {
    settings.messageFieldName = messageFieldName
  }

  open fun setTimestampResolution(timestampResolutionString: String) {
    settings.timestampResolution = TimestampResolutionConverter().convert(timestampResolutionString)
  }

  open fun setTimestampFieldName(timestampFieldName: String) {
    settings.timestampFieldName = timestampFieldName
  }

  open fun setIncludeLogLevel(includeLogLevel: Boolean) {
    settings.includeLogLevel = includeLogLevel
  }

  open fun setLogLevelFieldName(logLevelFieldName: String) {
    settings.logLevelFieldName = logLevelFieldName
  }

  open fun setIncludeLogger(includeLogger: Boolean) {
    settings.includeLogger = includeLogger
  }

  open fun setLoggerFieldName(loggerFieldName: String) {
    settings.loggerFieldName = loggerFieldName
  }

  open fun setIncludeLoggerName(includeLoggerName: Boolean) {
    settings.includeLoggerName = includeLoggerName
  }

  open fun setLoggerNameFieldName(loggerNameFieldName: String) {
    settings.loggerNameFieldName = loggerNameFieldName
  }

  open fun setIncludeThreadName(includeThreadName: Boolean) {
    settings.includeThreadName = includeThreadName
  }

  open fun setThreadNameFieldName(threadNameFieldName: String) {
    settings.threadNameFieldName = threadNameFieldName
  }

  open fun setIncludeHostName(includeHostName: Boolean) {
    settings.includeHostName = includeHostName
  }

  open fun setHostNameFieldName(hostNameFieldName: String) {
    settings.hostNameFieldName = hostNameFieldName
  }

  open fun setIncludeStacktrace(includeStacktrace: Boolean) {
    settings.includeStacktrace = includeStacktrace
  }

  open fun setStacktraceFieldName(stacktraceFieldName: String) {
    settings.stacktraceFieldName = stacktraceFieldName
  }

  open fun setIncludeMdc(includeMdc: Boolean) {
    settings.includeMdc = includeMdc
  }

  open fun setMdcKeysPrefix(mdcKeysPrefix: String) {
    settings.mdcKeysPrefix = mdcKeysPrefix
  }

  open fun setMaxLogRecordsPerBatch(maxLogRecordsPerBatch: Int) {
    settings.maxLogRecordsPerBatch = maxLogRecordsPerBatch
  }

  open fun setMaxBufferedLogRecords(maxBufferedLogRecords: Int) {
    settings.maxBufferedLogRecords = maxBufferedLogRecords
  }

  open fun setSendLogRecordsPeriodMillis(sendLogRecordsPeriodMillis: Long) {
    settings.sendLogRecordsPeriodMillis = sendLogRecordsPeriodMillis
  }

}