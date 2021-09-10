package net.codinux.log.elasticsearch

import com.github.tomakehurst.wiremock.WireMockServer
import net.codinux.log.elasticsearch.errorhandler.ErrorHandler
import net.codinux.log.elasticsearch.util.TestDataCreator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.mock
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.logging.Level


open class ElasticsearchTestBase {

    companion object {

        const val Scheme = "http"

        const val Host = "localhost"

        protected const val Port = 12345

        const val IndexName = TestDataCreator.IndexName

        const val ExceptedElasticsearchUrl = TestDataCreator.ExceptedElasticsearchUrl

        const val Message = "Something terrible happened"

        val Timestamp = Instant.now()

        val LogLevel = Level.INFO.name

        const val LoggerClassName = "MyFancyClass"

        const val LoggerFullQualifiedName = "com.example.$LoggerClassName"

        const val ThreadName = "MainThread"

        const val HostName = "localhost"

        const val MaxLogRecordsPerBatch = 10

    }


    protected val esMock = WireMockServer(Port)

    protected val settings = LoggerSettings(host = "$Scheme://$Host:$Port", indexName = IndexName, maxLogRecordsPerBatch = MaxLogRecordsPerBatch)

    protected val errorHandlerMock = mock(ErrorHandler::class.java)

    protected val dataCreator = TestDataCreator()


    @BeforeEach
    fun initElasticsearchMock() {
        esMock.start()

        mockElasticsearchInfoRequest()
    }

    @AfterEach
    fun tearDownElasticsearchMock() {
        esMock.stop()
    }


    protected open fun verifyCountRecordsInBatch(requestBody: String, countRecords: Int) {
        assertThat(countOccurrences(requestBody, "\"message\":\"$Message\"")).isEqualTo(countRecords)
        assertThat(countOccurrences(requestBody, "\"level\":\"$LogLevel\"")).isEqualTo(countRecords)
    }

    protected open fun countOccurrences(string: String, patternToFind: String): Int {
        return string.windowed(patternToFind.length).count { it == patternToFind }
    }


    protected open fun mockIndexingSuccessResponse() {
        dataCreator.mockIndexingSuccessResponse(esMock)
    }

    protected open fun mockElasticsearchInfoRequest() {
        dataCreator.mockElasticsearchInfoRequest(esMock)
    }


    protected open fun waitTillAsynchronousProcessingDone() {
        TimeUnit.MILLISECONDS.sleep(settings.sendLogRecordsPeriodMillis * 4)
    }

}