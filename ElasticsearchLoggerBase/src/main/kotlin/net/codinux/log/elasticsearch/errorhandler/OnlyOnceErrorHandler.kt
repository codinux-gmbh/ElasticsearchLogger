package net.codinux.log.elasticsearch.errorhandler

/**
 * Ensures that an error gets only handled once. Delegates actual error handling to wrapped ErrorHandler.
 */
open class OnlyOnceErrorHandler @JvmOverloads constructor(
        protected open val wrappedErrorHandler: ErrorHandler = StdErrErrorHandler()
) : ErrorHandler {

    protected open val handledErrors = mutableMapOf<Class<out Throwable>, String>()

    override fun showError(message: String, e: Throwable?) {
        if (e != null) {
            if (handledErrors[e.javaClass] == e.message) {
                return // already handled
            }

            handledErrors[e.javaClass] = e.message ?: ""
        }

        wrappedErrorHandler.showError(message, e)
    }

}