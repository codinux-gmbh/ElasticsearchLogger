package net.codinux.log.elasticsearch.errorhandler


open class StdErrErrorHandler : ErrorHandler {

    override fun logError(message: String, e: Throwable?) {
        if (e == null) {
            System.err.println(message)
        }
        else {
            System.err.println("${message}: ${e.message}")

            e.printStackTrace()
        }
    }

    override fun logInfo(message: String) {
        println(message)
    }

}