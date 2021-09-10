package net.codinux.log.elasticsearch.errorhandler

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.lang.RuntimeException
import java.net.ConnectException
import java.time.Duration
import java.util.concurrent.TimeUnit

class LogPerPeriodErrorHandlerTest {

    companion object {

        private const val PeriodInMillis = 10L

        private val ExceptionToLogEachTime = IllegalArgumentException::class.java

    }


    private val wrappedErrorHandler = Mockito.mock(ErrorHandler::class.java)

    private val underTest = LogPerPeriodErrorHandler(Duration.ofMillis(PeriodInMillis), wrappedErrorHandler, listOf(ExceptionToLogEachTime))


    @Test
    fun logsSameErrorOnlyOnceInPeriod() {
        val exception = ConnectException("Connection refused")


        underTest.logError("Message 1", exception)

        underTest.logError("Message 2", exception)


        verify(wrappedErrorHandler, times(1)).logError(any(), any()) // verify that wrapped ErrorHandler gets called only once
        verify(wrappedErrorHandler, times(1)).logError("Message 1", exception) // and with correct parameter
    }

    @Test
    fun logsSameErrorAgainAfterPeriod() {
        val exception = ConnectException("Connection refused")


        underTest.logError("Message 1", exception)

        waitMillis(PeriodInMillis + 10)

        underTest.logError("Message 2", exception)


        verify(wrappedErrorHandler, times(2)).logError(any(), any()) // verify that wrapped ErrorHandler gets called for each error
        verify(wrappedErrorHandler, times(1)).logError("Message 1", exception) // and with correct parameters
        verify(wrappedErrorHandler, times(1)).logError("Message 2", exception) // and with correct parameters
    }

    @Test
    fun differentError() {
        val exception1 = ConnectException("Connection refused")
        val exception2 = RuntimeException("Something else happened")


        underTest.logError("Message 1", exception1)

        underTest.logError("Message 2", exception2)


        verify(wrappedErrorHandler, times(2)).logError(any(), any()) // verify that wrapped ErrorHandler gets called twice
        verify(wrappedErrorHandler, times(1)).logError("Message 1", exception1) // and with correct parameter
        verify(wrappedErrorHandler, times(1)).logError("Message 2", exception2) // and with correct parameter
    }

    @Test
    fun exceptionIsInExceptionsToLogEachTime() {
        val exception = IllegalArgumentException("Some parameter has been false")


        underTest.logError("Message 1", exception)

        underTest.logError("Message 2", exception)


        verify(wrappedErrorHandler, times(2)).logError(any(), any()) // verify that wrapped ErrorHandler gets called for each error
        verify(wrappedErrorHandler, times(1)).logError("Message 1", exception) // and with correct parameters
        verify(wrappedErrorHandler, times(1)).logError("Message 2", exception) // and with correct parameters
    }


    private fun waitMillis(millis: Long) {
        TimeUnit.MILLISECONDS.sleep(millis)
    }

}