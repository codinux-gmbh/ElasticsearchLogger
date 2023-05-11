package net.codinux.log.elasticsearch

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.IThrowableProxy
import ch.qos.logback.classic.spi.ThrowableProxy
import net.codinux.log.elasticsearch.errorhandler.ErrorHandler
import net.codinux.log.elasticsearch.errorhandler.LogbackErrorHandler
import java.net.InetAddress
import java.time.Instant
import kotlin.jvm.JvmOverloads


open class LogbackElasticsearchLogAppender @JvmOverloads constructor(
    settings: LoggerSettings = LoggerSettings(),
    protected open val errorHandler: ErrorHandler = LogbackErrorHandler()
) : ConfigurableUnsynchronizedAppender(settings) {

    protected open lateinit var elasticsearchLogHandler: ElasticsearchLogHandler

    protected open val hostName = InetAddress.getLocalHost().hostName


    override fun start() {
        // don't start appender if enabled is false or host is not set
        if (settings.enabled) {
            if (settings.host.isNullOrBlank()) {
                errorHandler.logError("Host has to be not set. Please configure it in your Logback settings. Cannot start, shutting down LogbackElasticsearchLogAppender.")
            } else {
                elasticsearchLogHandler = ElasticsearchLogHandler(settings, errorHandler)

                super.start()
            }
        }
    }

    override fun stop() {
        elasticsearchLogHandler.close()

        super.stop()
    }


    override fun append(eventObject: ILoggingEvent?) {
        if (eventObject != null) {
            elasticsearchLogHandler.handle(mapRecord(eventObject))
        }
    }

    protected open fun mapRecord(event: ILoggingEvent): LogRecord {
        return LogRecord(event.formattedMessage, Instant.ofEpochMilli(event.timeStamp), event.level.levelStr, event.loggerName,
            event.threadName, hostName, getThrowable(event), event.mdcPropertyMap, event.marker?.name)
    }

    protected open fun getThrowable(event: ILoggingEvent): Throwable? {
        event.throwableProxy?.let { proxy ->
            if (proxy is ThrowableProxy) {
                return proxy.throwable
            } else {
                return tryToInstantiateThrowable(proxy)
            }
        }

        return null
    }

    protected open fun tryToInstantiateThrowable(proxy: IThrowableProxy): Throwable? {
        try {
            val throwableClass = Class.forName(proxy.className)

            val throwable = throwableClass.declaredConstructors.firstOrNull { it.parameterCount == 1 && it.parameterTypes[0] == String::class.java }?.let { constructor ->
                constructor.newInstance(proxy.message) as Throwable
            } ?: throwableClass.getDeclaredConstructor().newInstance() as Throwable

            throwable.stackTrace = proxy.stackTraceElementProxyArray.map { it.stackTraceElement }.toTypedArray()

            return throwable
        } catch (e: Exception) {
            errorHandler.logError("Could not get Throwable from IThrowableProxy", e)
        }

        return null
    }

}