package net.codinux.log.elasticsearch.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import net.codinux.log.elasticsearch.es_model.*


class TestDataCreator {

    companion object {

        const val IndexName = "test-index"

        const val ExceptedElasticsearchUrl = "/$IndexName/_bulk"

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
            HttpHeader("content-type", "application/json; charset=UTF-8")
        )

        const val IndexingFailedType = "es_rejected_execution_exception"

        const val IndexingFailedReason = "rejected execution of org.elasticsearch.transport.TransportService\$7@227349ba on EsThreadPoolExecutor" +
                "[bulk, queue capacity = 200, org.elasticsearch.common.util.concurrent.EsThreadPoolExecutor@4d3e4d91[Running, pool size = 4, active threads = 4, " +
                "queued tasks = 202, completed tasks = 210040]]"

    }

    private val mapper = ObjectMapper()


    fun mockIndexingSuccessResponse(esMock: WireMockServer) {
        mockIndexingResponse(esMock, createIndexingSuccessResponse())
    }

    fun mockIndexingFailureResponse(esMock: WireMockServer) {
        mockIndexingResponse(esMock, createIndexingFailureResponse())
    }

    fun mockIndexingResponse(esMock: WireMockServer, body: String) {
        esMock.stubFor(post(urlPathEqualTo(ExceptedElasticsearchUrl))
            .withHeader("content-type", EqualToPattern("application/json"))
            .willReturn(ok().withHeaders(ElasticsearchResponseHeaders).withBody(body)))
    }

    fun mockElasticsearchInfoRequest(esMock: WireMockServer) {
        esMock.stubFor(get(urlPathEqualTo("/"))
            .willReturn(ok().withHeaders(ElasticsearchResponseHeaders).withBody(ElasticsearchInfoResponseBody)))
    }


    private fun createIndexingSuccessResponse(): String {
        val response = BulkResponse(8, false, listOf(
            ResponseContainerItem(ResponseItemBase(
                IndexName, 201, "7rI3f3sBzy23N1EWgjPP", "created", 1, "_doc", 2, 9, createShardStatistics()
            ))
        ))

        return mapToJson(response)
    }

    private fun createIndexingFailureResponse(): String {
        val response = BulkResponse(8, true, listOf(
            ResponseContainerItem(ResponseItemBase(IndexName, 429, "7rI3f3sBzy23N1EWgjPP", error = createErrorCause()))
        ))

        return mapToJson(response)
    }

    private fun createErrorCause() = ErrorCause(IndexingFailedType, IndexingFailedReason)

    private fun createShardStatistics() = ShardStatistics(2, 1, 0)

    private fun mapToJson(response: BulkResponse) = mapper.writeValueAsString(response)

}