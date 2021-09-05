package net.codinux.log.elasticsearch.errorhandler


open class StdErrErrorHandler : ErrorHandler {

    override fun logError(message: String, e: Throwable?) {
        System.err.println(message)

        e?.printStackTrace()
    }

}