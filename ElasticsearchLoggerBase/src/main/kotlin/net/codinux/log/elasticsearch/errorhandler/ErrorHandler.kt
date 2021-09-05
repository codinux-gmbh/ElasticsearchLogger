package net.codinux.log.elasticsearch.errorhandler


interface ErrorHandler {

    fun logError(message: String, e: Throwable? = null)

}