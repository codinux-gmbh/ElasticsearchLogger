package net.codinux.log.elasticsearch.errorhandler

import org.slf4j.LoggerFactory


open class LogbackErrorHandler(loggerName: String) : ErrorHandler { // TODO: use ContextAware

    constructor() : this(LogbackErrorHandler::class.java)

    constructor(loggerClass: Class<*>) : this(loggerClass.name)


    protected open val log = LoggerFactory.getLogger(loggerName)


    override fun logError(message: String, e: Throwable?) {
        log.error(message, e)
    }

    override fun logInfo(message: String) {
        log.info(message)
    }

}