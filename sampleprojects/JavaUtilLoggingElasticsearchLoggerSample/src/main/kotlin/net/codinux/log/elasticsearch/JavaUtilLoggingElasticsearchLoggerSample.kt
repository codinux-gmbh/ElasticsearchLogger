package net.codinux.log.elasticsearch

import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    val configFile = JavaUtilLoggingElasticsearchLoggerSample::class.java.getResourceAsStream("/logging.properties")
    LogManager.getLogManager().readConfiguration(configFile)

    JavaUtilLoggingElasticsearchLoggerSample().runExample()
}


open class JavaUtilLoggingElasticsearchLoggerSample {

    private val log = Logger.getLogger(JavaUtilLoggingElasticsearchLoggerSample::class.java.name)


    fun runExample() {
        log.severe("Error without Exception")
        log.log(Level.SEVERE, "Error with Exception", Exception("A test exception for error log level"))

        log.warning("Warn without Exception")
        log.log(Level.WARNING, "Warn with Exception", Exception("A test exception for warn log level"))

        log.info("Info log")

        log.log(Level.FINE, "Debug log - shouldn't get logged")

        TimeUnit.SECONDS.sleep(1) // ElasticsearchLogger sends records asynchronously, give it some time for that

        exitProcess(0)
    }

}