package net.codinux.log.elasticsearch

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import net.codinux.log.elasticsearch.errorhandler.ErrorHandler
import net.codinux.log.elasticsearch.es_model.BulkResponse
import net.codinux.log.elasticsearch.es_model.ResponseContainerItem
import net.codinux.log.elasticsearch.es_model.ResponseItemBase
import net.codinux.log.elasticsearch.es_model.ShardStatistics
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import kotlin.math.roundToInt

class ElasticsearchLogHandlerTest {

    companion object {

        private const val Scheme = "http"

        private const val Host = "localhost"

        private const val Port = 12345

        private const val IndexName = "test-index"

        private const val ExceptedElasticsearchUrl = "/_bulk"

        private const val Message = "Something terrible happened"

        private val Timestamp = Instant.now()

        private val LogLevel = Level.INFO.name

        private const val LoggerClassName = "MyFancyClass"

        private const val LoggerFullQualifiedName = "com.example.$LoggerClassName"

        private const val ThreadName = "MainThread"

        private const val HostName = "localhost"

        private const val MaxLogRecordsPerBatch = 10


        private const val ElasticsearchInfoResponseBody = "{\n" +
                "  \"name\" : \"10bfd6ace8bc\",\n" +
                "  \"cluster_name\" : \"docker-cluster\",\n" +
                "  \"cluster_uuid\" : \"mSOq8WKCTdigScTrQExAig\",\n" +
                "  \"version\" : {\n" +
                "    \"number\" : \"7.14.0\",\n" +
                "    \"build_flavor\" : \"default\",\n" +
                "    \"build_type\" : \"docker\",\n" +
                "    \"build_hash\" : \"dd5a0a2acaa2045ff9624f3729fc8a6f40835aa1\",\n" +
                "    \"build_date\" : \"2021-07-29T20:49:32.864135063Z\",\n" +
                "    \"build_snapshot\" : false,\n" +
                "    \"lucene_version\" : \"8.9.0\",\n" +
                "    \"minimum_wire_compatibility_version\" : \"6.8.0\",\n" +
                "    \"minimum_index_compatibility_version\" : \"6.0.0-beta1\"\n" +
                "  },\n" +
                "  \"tagline\" : \"You Know, for Search\"\n" +
                "}\n"

        private val ElasticsearchResponseHeaders = HttpHeaders(
                HttpHeader("X-elastic-product", "Elasticsearch"),
                HttpHeader("content-type", "application/json; charset=UTF-8"))
    }


    private val esMock = WireMockServer(Port)

    private val settings = LoggerSettings(host = "$Scheme://$Host:$Port", indexName = IndexName, maxLogRecordsPerBatch = MaxLogRecordsPerBatch)

    private val errorHandlerMock = mock(ErrorHandler::class.java)

    private val underTest = ElasticsearchLogHandler(settings, errorHandlerMock)

    private val mapper = ObjectMapper()


    @BeforeEach
    fun init() {
        esMock.start()

        mockElasticsearchInfoRequest()
    }

    @AfterEach
    fun tearDown() {
        underTest.close()

        esMock.stop()
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

    private fun verifyCountRecordsInBatch(requestBody: String, countRecords: Int) {
        assertThat(countOccurrences(requestBody, "\"message\":\"$Message\"")).isEqualTo(countRecords)
        assertThat(countOccurrences(requestBody, "\"level\":\"$LogLevel\"")).isEqualTo(countRecords)
    }

    private fun sendMultipleRecords(countRecords: Int) {
        val record = LogRecord(Message, Timestamp, LogLevel, LoggerFullQualifiedName, ThreadName, HostName)

        mockIndexingSuccessResponse()


        for (i in 0 until countRecords) {
            underTest.handle(record)
        }

        waitTillAsynchronousProcessingDone()
    }

    private fun countOccurrences(string: String, patternToFind: String): Int {
        return string.windowed(patternToFind.length).count { it == patternToFind }
    }


    private fun mockIndexingSuccessResponse() {
        esMock.stubFor(post(urlPathEqualTo(ExceptedElasticsearchUrl))
                .withHeader("content-type", EqualToPattern("application/json"))
                .willReturn(ok().withHeaders(ElasticsearchResponseHeaders).withBody(createIndexingSuccessResponse())))
    }

    private fun mockElasticsearchInfoRequest() {
        esMock.stubFor(get(urlPathEqualTo("/"))
                .willReturn(ok().withHeaders(ElasticsearchResponseHeaders).withBody(ElasticsearchInfoResponseBody)))
    }


    private fun createIndexingSuccessResponse(): String {
        val response = BulkResponse(8, false, listOf(
            ResponseContainerItem(ResponseItemBase(IndexName, 201, "create", "7rI3f3sBzy23N1EWgjPP", 1, "_doc", 2, 9,
                createShardStatistics()))
        ))

        return mapToJson(response)
    }

    private fun mapToJson(response: BulkResponse) = mapper.writeValueAsString(response)

    private fun createShardStatistics() = ShardStatistics(2, 1, 0)

    private fun waitTillAsynchronousProcessingDone() {
        underTest.flush()
        TimeUnit.MILLISECONDS.sleep(settings.sendLogRecordsPeriodMillis * 4)
    }

}