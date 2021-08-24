package net.codinux.log.elasticsearch.errorhandler


open class StdErrErrorHandler : ErrorHandler {

    override fun showError(message: String, e: Throwable?) {
        System.err.println(message)

        e?.printStackTrace()
    }

}