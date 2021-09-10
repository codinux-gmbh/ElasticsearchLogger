package net.codinux.log.elasticsearch.errorhandler

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Ensures that each error gets only handled once per period (e.g. only once per 30 minutes).
 * Delegates actual error handling to wrapped ErrorHandler.
 */
open class LogPerPeriodErrorHandler @JvmOverloads constructor(
    protected open val period: Duration,
    protected open val wrappedErrorHandler: ErrorHandler = StdErrErrorHandler(),
    protected open val exceptionsToLogEachTime: Collection<Class<out Throwable>> = listOf()
) : ErrorHandler {

    protected open val handledErrors = ConcurrentHashMap<Class<out Throwable>, Instant>()

    override fun logError(message: String, e: Throwable?) {
        if (e != null && exceptionsToLogEachTime.contains(e.javaClass) == false) {
            val errorLastLoggedAt = handledErrors[e.javaClass]
            if (errorLastLoggedAt != null && period > Duration.between(errorLastLoggedAt, Instant.now())) {
                return // already handled in period
            }

            handledErrors.put(e.javaClass, Instant.now())
        }

        wrappedErrorHandler.logError(message, e)
    }

    override fun logInfo(message: String) {
        wrappedErrorHandler.logInfo(message)
    }

}