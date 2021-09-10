package net.codinux.log.elasticsearch

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import net.codinux.log.elasticsearch.errorhandler.ErrorHandler
import net.codinux.log.elasticsearch.util.TestDataCreator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import kotlin.math.roundToInt

class ElasticsearchLogHandlerTest : ElasticsearchTestBase() {

    private val underTest = ElasticsearchLogHandler(settings, errorHandlerMock)


    @AfterEach
    fun tearDown() {
        underTest.close()
    }


    @Test
    fun logInfoMessage() {
        val record = LogRecord(Message, Timestamp, LogLevel, LoggerFullQualifiedName, ThreadName, HostName)

        mockIndexingSuccessResponse()


        underTest.handle(record)

        waitTillAsynchronousProcessingDone()


        esMock.verify(postRequestedFor(urlPathEqualTo(ExceptedElasticsearchUrl))
                .withRequestBody(containing("\"message\":\"$Message\""))
                .withRequestBody(containing("\"level\":\"$LogLevel\"")))
    }

    @Test
    fun logMdc() {
        val mdcKey1 = "MdcKey1"
        val mdcValue1 = "MdcValue1"
        val mdcKey2 = "MdcKey2"
        val mdcValue2 = "MdcValue2"
        val mdc = mapOf(mdcKey1 to mdcValue1, mdcKey2 to mdcValue2)
        val record = LogRecord(Message, Timestamp, LogLevel, LoggerFullQualifiedName, ThreadName, HostName, mdc = mdc)

        mockIndexingSuccessResponse()


        underTest.handle(record)

        waitTillAsynchronousProcessingDone()


        esMock.verify(postRequestedFor(urlPathEqualTo(ExceptedElasticsearchUrl))
                .withRequestBody(containing("\"$mdcKey1\":\"$mdcValue1\""))
                .withRequestBody(containing("\"$mdcKey2\":\"$mdcValue2\"")))
    }

    @Test
    fun logStacktrace() {
        val errorMessage = "Just a test exception. No one was hurt during the creation of it"
        val exception = Exception(errorMessage)
        val record = LogRecord(Message, Timestamp, LogLevel, LoggerFullQualifiedName, ThreadName, HostName, exception)

        mockIndexingSuccessResponse()


        underTest.handle(record)

        waitTillAsynchronousProcessingDone()


        esMock.verify(postRequestedFor(urlPathEqualTo(ExceptedElasticsearchUrl))
                .withRequestBody(containing("\"stacktrace\":\"java.lang.Exception: $errorMessage")))
    }

    @Test
    fun logOnlyLoggerName() {
        settings.includeLoggerName = true
        val record = LogRecord(Message, Timestamp, LogLevel, LoggerFullQualifiedName, ThreadName, HostName)

        mockIndexingSuccessResponse()


        underTest.handle(record)

        waitTillAsynchronousProcessingDone()


        esMock.verify(postRequestedFor(urlPathEqualTo(ExceptedElasticsearchUrl))
                .withRequestBody(containing("\"${LoggerSettings.LoggerNameDefaultFieldName}\":\"$LoggerClassName")))
    }


    @Test
    fun maxLogRecordsPerBatch() {
        val countRecords = MaxLogRecordsPerBatch

        sendMultipleRecords(countRecords)


        // only one request has been sent
        esMock.verify(1, postRequestedFor(urlPathEqualTo(ExceptedElasticsearchUrl)))

        val requestBody = esMock.findAll(postRequestedFor(urlPathEqualTo(ExceptedElasticsearchUrl))).first().bodyAsString
        verifyCountRecordsInBatch(requestBody, countRecords)
    }

    @Test
    fun moreThanMaxLogRecordsPerBatch() {
        val countRecords = (MaxLogRecordsPerBatch * 1.5).roundToInt()

        sendMultipleRecords(countRecords)

        waitTillAsynchronousProcessingDone() // two batches -> we need to wait double the time


        // verify records have been sent batched in two request has been sent
        esMock.verify(2, postRequestedFor(urlPathEqualTo(ExceptedElasticsearchUrl)))

        val requests = esMock.findAll(postRequestedFor(urlPathEqualTo(ExceptedElasticsearchUrl)))

        val firstRequestBody = requests.first().bodyAsString
        verifyCountRecordsInBatch(firstRequestBody, MaxLogRecordsPerBatch)

        val secondRequestBody = requests.get(1).bodyAsString
        verifyCountRecordsInBatch(secondRequestBody, countRecords - MaxLogRecordsPerBatch)
    }


    private fun sendMultipleRecords(countRecords: Int) {
        val record = LogRecord(Message, Timestamp, LogLevel, LoggerFullQualifiedName, ThreadName, HostName)

        mockIndexingSuccessResponse()


        for (i in 0 until countRecords) {
            underTest.handle(record)
        }

        waitTillAsynchronousProcessingDone()
    }

    override fun waitTillAsynchronousProcessingDone() {
        underTest.flush()

        super.waitTillAsynchronousProcessingDone()
    }

}