package net.codinux.log.elasticsearch.errorhandler

import org.jboss.logmanager.Level
import org.jboss.logmanager.Logger


open class JBossLoggingErrorHandler(loggerName: String) : ErrorHandler {

    constructor() : this(JBossLoggingErrorHandler::class.java)

    constructor(loggerClass: Class<*>) : this(loggerClass.name)


    protected open val log = Logger.getLogger(loggerName)


    override fun logError(message: String, e: Throwable?) {
        log.log(Level.ERROR, message, e)
    }

    override fun logInfo(message: String) {
        log.info(message)
    }

}