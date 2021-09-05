package net.codinux.log.elasticsearch.errorhandler

import java.util.logging.Level
import java.util.logging.Logger


open class JavaUtilLogErrorHandler(loggerName: String) : ErrorHandler {

    constructor() : this(JavaUtilLogErrorHandler::class.java)

    constructor(loggerClass: Class<*>) : this(loggerClass.name)


    protected open val log = Logger.getLogger(loggerName)


    override fun logError(message: String, e: Throwable?) {
        log.log(Level.SEVERE, message, e)
    }

    override fun logInfo(message: String) {
        log.info(message)
    }

}