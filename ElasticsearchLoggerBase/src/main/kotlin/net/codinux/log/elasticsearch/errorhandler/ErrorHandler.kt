package net.codinux.log.elasticsearch.errorhandler


interface ErrorHandler {

    fun logError(message: String) {
        logError(message, null)
    }

    fun logError(message: String, e: Throwable?)

}