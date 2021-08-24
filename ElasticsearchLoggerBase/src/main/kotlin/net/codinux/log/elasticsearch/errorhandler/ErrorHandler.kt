package net.codinux.log.elasticsearch.errorhandler


interface ErrorHandler {

    fun showError(message: String, e: Throwable?)

}