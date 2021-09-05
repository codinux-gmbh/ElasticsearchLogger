package net.codinux.log.elasticsearch.errorhandler

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Ensures that an error gets only handled once. Delegates actual error handling to wrapped ErrorHandler.
 */
open class OnlyOnceErrorHandler @JvmOverloads constructor(
        protected open val wrappedErrorHandler: ErrorHandler = StdErrErrorHandler()
) : ErrorHandler {

    protected open val handledErrors = CopyOnWriteArrayList<Class<out Throwable>>()

    override fun logError(message: String, e: Throwable?) {
        if (e != null) {
            if (handledErrors.contains(e.javaClass)) {
                return // already handled
            }

            handledErrors.add(e.javaClass)
        }

        wrappedErrorHandler.logError(message, e)
    }

}