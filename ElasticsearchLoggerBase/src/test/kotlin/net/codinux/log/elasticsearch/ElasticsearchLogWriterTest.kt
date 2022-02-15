package net.codinux.log.elasticsearch

import com.github.tomakehurst.wiremock.client.CountMatchingStrategy
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import net.codinux.log.elasticsearch.util.TestDataCreator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.*
import java.io.PrintWriter

class ElasticsearchLogWriterTest : ElasticsearchTestBase() {

    private val underTest = object : ElasticsearchLogWriter(settings, errorHandlerMock) {

        fun createEsRecordJsonPublic(record: LogRecord): String {
            return super.createEsRecordJson(record)
        }

    }


    @Test
    fun mdcFieldsPrefixOff() {
        settings.mdcKeysPrefix = null
        val record = createLogRecord(mdc = mapOf("key1" to "value 1", "other_context" to "other value"))

        val result = underTest.createEsRecordJsonPublic(record)

        assertThat(result).contains("\"key1\":\"value 1\",\"other_context\":\"other value\"")
    }

    @Test
    fun mdcFieldsPrefixSet() {
        settings.mdcKeysPrefix = "mdc"
        val record = createLogRecord(mdc = mapOf("key1" to "value 1", "other_context" to "other value"))

        val result = underTest.createEsRecordJsonPublic(record)

        assertThat(result).contains("\"mdc.key1\":\"value 1\",\"mdc.other_context\":\"other value\"")
    }


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


    @Test
    fun stackTraceMaxFieldLength() {
        val exception = mock<Exception>()
        `when`(exception.printStackTrace(any<PrintWriter>())).thenAnswer { answer ->
            val stackTraceMock = "a".repeat(LoggerSettings.StacktraceMaxFieldLengthDefaultValue * 2)
            val writer: PrintWriter = answer.arguments.first() as PrintWriter
            writer.write(stackTraceMock)
            writer.flush()
            ""
        }

        val record = createLogRecord(exception = exception)


        val result = underTest.createEsRecordJsonPublic(record)


        assertThat(result).contains("\"stacktrace\":\"${"a".repeat(LoggerSettings.StacktraceMaxFieldLengthDefaultValue)}\"")
    }

}