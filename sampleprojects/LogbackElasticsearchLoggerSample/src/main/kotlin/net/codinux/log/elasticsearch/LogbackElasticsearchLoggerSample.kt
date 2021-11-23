package net.codinux.log.elasticsearch

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    // set the configuration in your Logback config, e.g. logback.xml
    LogbackElasticsearchLoggerSample().runExample()
}


open class LogbackElasticsearchLoggerSample {

    private val log = LoggerFactory.getLogger(LogbackElasticsearchLoggerSample::class.java.name)


    fun runExample() {
        MDC.put("traceId", UUID.randomUUID().toString()) // as a sample adds a traceId to all logs below

        log.error("Error without Exception")
        log.error("Error with Exception", Exception("A test exception for error log level"))

        log.warn("Warn without {}", "Exception")
        log.warn("Warn with Exception", Exception("A test exception for warn log level"))

        log.info("Info log")

        log.debug("Debug log - shouldn't get logged")

        TimeUnit.SECONDS.sleep(5) // ElasticsearchLogger sends records asynchronously, give it some time for that

        exitProcess(0)
    }

}