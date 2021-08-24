package net.codinux.log.elasticsearch

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class ElasticsearchLogHandlerTest {

    companion object {

        private const val Scheme = "http"

        private const val Host = "localhost"

        private const val Port = 12345

        private const val IndexName = "test-index"

        private const val ExceptedElasticsearchUrl = "/$IndexName/_doc"

        private const val Message = "Something terrible happened"

        private val Timestamp = Instant.now()

        private val LogLevel = Level.INFO.name

        private const val LoggerName = "com.example.MyFancyClass"

        private const val ThreadName = "MainThread"

        private const val HostName = "localhost"


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

        private const val IndexingResponse = "{\"_index\":\"$IndexName\",\"_type\":\"_doc\",\"_id\":\"mg_YeXsBE814VrTbzBWn\",\"_version\":1,\"result\":\"created\"," +
                "\"_shards\":{\"total\":2,\"successful\":1,\"failed\":0},\"_seq_no\":6,\"_primary_term\":1}"

        private val ElasticsearchResponseHeaders = HttpHeaders(
                HttpHeader("X-elastic-product", "Elasticsearch"),
                HttpHeader("content-type", "application/json; charset=UTF-8"))
    }


    private val esMock = WireMockServer(Port)

    private val settings = LoggerSettings(host = "$Scheme://$Host:$Port", indexName = IndexName)

    private val errorHandlerMock = mock(ErrorHandler::class.java)

    private val underTest = ElasticsearchLogHandler(settings, errorHandlerMock)


    @BeforeEach
    fun init() {
        esMock.start()

        mockElasticsearchInfoRequest()
    }

    @AfterEach
    fun tearDown() {
        esMock.stop()
    }


    @Test
    fun logInfoMessage() {
        val record = LogRecord(Message, Timestamp, LogLevel, LoggerName, ThreadName, HostName)

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
        val mdc = mapOf(mdcKey1 to mdcValue1, mdcKey2 to mdcValue2
        )
        val record = LogRecord(Message, Timestamp, LogLevel, LoggerName, ThreadName, HostName, mdc = mdc)

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
        val record = LogRecord(Message, Timestamp, LogLevel, LoggerName, ThreadName, HostName, exception)

        mockIndexingSuccessResponse()


        underTest.handle(record)

        waitTillAsynchronousProcessingDone()


        esMock.verify(postRequestedFor(urlPathEqualTo(ExceptedElasticsearchUrl))
                .withRequestBody(containing("\"stacktrace\":\"java.lang.Exception: $errorMessage")))
    }


    private fun mockIndexingSuccessResponse() {
        esMock.stubFor(post(urlEqualTo(ExceptedElasticsearchUrl))
                .withHeader("content-type", EqualToPattern("application/json"))
                .willReturn(ok().withHeaders(ElasticsearchResponseHeaders).withBody(IndexingResponse)))
    }

    private fun mockElasticsearchInfoRequest() {
        esMock.stubFor(get(anyUrl())
                .willReturn(ok().withHeaders(ElasticsearchResponseHeaders).withBody(ElasticsearchInfoResponseBody)))
    }

    private fun waitTillAsynchronousProcessingDone() {
        underTest.flush()
        TimeUnit.MILLISECONDS.sleep(500)
    }

}