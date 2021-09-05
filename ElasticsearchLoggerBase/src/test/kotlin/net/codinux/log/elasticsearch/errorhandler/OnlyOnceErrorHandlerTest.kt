package net.codinux.log.elasticsearch.errorhandler

import org.junit.jupiter.api.Test

import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.lang.RuntimeException
import java.net.ConnectException

class OnlyOnceErrorHandlerTest {

    private val wrappedErrorHandler = mock(ErrorHandler::class.java)

    private val underTest = OnlyOnceErrorHandler(wrappedErrorHandler)


    @Test
    fun showSameErrorTwice() {
        val exception = ConnectException("Connection refused")


        underTest.logError("Message 1", exception)

        underTest.logError("Message 2", exception)


        verify(wrappedErrorHandler, times(1)).logError(any(), any()) // verify that wrapped ErrorHandler gets called only once
        verify(wrappedErrorHandler, times(1)).logError("Message 1", exception) // and with correct parameter
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

}