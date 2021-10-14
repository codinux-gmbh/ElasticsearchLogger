package net.codinux.log.elasticsearch

import com.github.tomakehurst.wiremock.client.CountMatchingStrategy
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import net.codinux.log.elasticsearch.util.TestDataCreator
import org.junit.jupiter.api.Test
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

class ElasticsearchLogWriterTest : ElasticsearchTestBase() {

    private val underTest = ElasticsearchLogWriter(settings, errorHandlerMock)


    @Test
    fun loggingMessageFailsTwoTimes_GetsLoggedAtThirdAttempt() {
        val record = createLogRecord()

        mockIndexingFailureResponse()


        underTest.writeRecord(record)

        waitTillAsynchronousProcessingDone()
        waitTillAsynchronousProcessingDone() // indexing should fail two times

        esMock.verify(CountMatchingStrategy(CountMatchingStrategy.GREATER_THAN_OR_EQUAL, 2), postRequestedFor(WireMock.urlPathEqualTo(ExceptedElasticsearchUrl))
            .withRequestBody(WireMock.containing("\"message\":\"${Message}\""))
            .withRequestBody(WireMock.containing("\"level\":\"${LogLevel}\"")))

        verify(errorHandlerMock, atLeast(2)).logError("Could not send log records to Elasticsearch: OK failure in bulk execution:\n" +
                "[0]: index [test-index], type [null], id [7rI3f3sBzy23N1EWgjPP], message [ElasticsearchException[Elasticsearch exception " +
                "[type=${TestDataCreator.IndexingFailedType}, reason=${TestDataCreator.IndexingFailedReason}]]]")


        // now let indexing succeed

        esMock.resetAll()

        mockIndexingSuccessResponse()

        reset(errorHandlerMock)

        waitTillAsynchronousProcessingDone()
        waitTillAsynchronousProcessingDone() // we again wait for two indexing attempts but indexing request should occure only once

        esMock.verify(1, postRequestedFor(WireMock.urlPathEqualTo(ExceptedElasticsearchUrl))
            .withRequestBody(WireMock.containing("\"message\":\"${Message}\""))
            .withRequestBody(WireMock.containing("\"level\":\"${LogLevel}\"")))

        verifyNoMoreInteractions(errorHandlerMock)
    }

}