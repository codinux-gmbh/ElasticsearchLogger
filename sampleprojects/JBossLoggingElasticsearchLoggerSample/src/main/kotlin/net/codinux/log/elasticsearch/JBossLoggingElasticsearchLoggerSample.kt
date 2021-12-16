package net.codinux.log.elasticsearch

import org.jboss.logging.Logger
import org.jboss.logging.NDC
import org.jboss.logmanager.LogManager
import org.jboss.logmanager.MDC
import org.jboss.logmanager.handlers.ConsoleHandler
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.util.concurrent.TimeUnit
import java.util.logging.SimpleFormatter
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")

    val rootLogger = LogManager.getLogManager().getLogger("")
    rootLogger.addHandler(JBossLoggingElasticsearchLogHandler())
    rootLogger.addHandler(ConsoleHandler(SimpleFormatter()))

    JBossLoggingElasticsearchLoggerSample().runExample()
}


open class JBossLoggingElasticsearchLoggerSample {

    private val log = Logger.getLogger(JBossLoggingElasticsearchLoggerSample::class.java.name)


    fun runExample() {
        log.error("Error without Exception")
        log.error("Error with Exception", Exception("A test exception for error log level"))

        log.warn("Warn without Exception")
        log.warn("Warn with Exception", Exception("A test exception for warn log level"))

        log.info("Info log")

        log.debug("Debug log")

        MDC.put("MDC test key", "MDC test value")
        log.info("Log with MDC set")
        MDC.clear()

        log.info("Log after clearing MDC")

        NDC.push("NDC 1")
        NDC.push("NDC 2")

        log.info("Log with NDC")

        NDC.clear()

        // Marker does not seem to be supported by JBoss logging, even though it's a field of ExtLogRecord with JavaDoc:
        // "Markers are used mostly by SLF4J and Log4j."
        val slf4jLogger = LoggerFactory.getLogger(JBossLoggingElasticsearchLoggerSample::class.java)
        slf4jLogger.info(MarkerFactory.getMarker("ImportantMessageMarker"), "Log with Marker")

        TimeUnit.SECONDS.sleep(1) // ElasticsearchLogger sends records asynchronously, give it some time for that

        exitProcess(0)
    }

}