package net.codinux.log.elasticsearch.es_model

import com.fasterxml.jackson.annotation.JsonInclude


/**
 * See https://raw.githubusercontent.com/elastic/elasticsearch-specification/main/output/schema/schema.json
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class ErrorCause @JvmOverloads constructor(
    val type: String,
    val reason: String,
    val caused_by: ErrorCause? = null,
    val shard: String? = null,
    val stack_trace: String? = null,
    val root_cause: List<ErrorCause> = listOf(),
    val bytes_limit: Long? = null,
    val bytes_wanted: Long? = null,
    val column: Int? = null,
    val col: Int? = null,
    val failed_shards: List<ShardFailure> = listOf(),
    val grouped: Boolean? = null,
    val index: IndexName? = null,
    val index_uuid: String? = null, // actually of type Uuid
    val language: String? = null,
    val licensed_expired_feature: String? = null,
    val line: Int? = null,
    val max_buckets: Int? = null
// there are so much additional properties, but i didn't my time was up to add more
) {

    override fun toString(): String {
        return "$type: $reason"
    }

}