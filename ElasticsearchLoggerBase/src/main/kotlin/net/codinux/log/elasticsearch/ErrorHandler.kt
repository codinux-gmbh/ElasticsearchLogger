package net.codinux.log.elasticsearch


interface ErrorHandler {

    fun showError(message: String, e: Throwable?)

}