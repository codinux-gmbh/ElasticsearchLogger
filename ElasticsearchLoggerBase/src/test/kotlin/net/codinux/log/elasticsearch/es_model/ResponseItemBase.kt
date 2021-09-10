package net.codinux.log.elasticsearch.es_model


/**
 * See https://raw.githubusercontent.com/elastic/elasticsearch-specification/main/output/schema/schema.json
 */
open class ResponseItemBase @JvmOverloads constructor(
    val _index: String,
    val status: Int,
    val result: String? = null,
    val _id: String? = null,
    val _version: VersionNumber? = null,
    val _type: String? = null,
    val _primary_term: Long? = null,
    val _seq_no: SequenceNumber? = null,
    val _shards: ShardStatistics? = null,
    val error: ErrorCause? = null,
    val forced_refresh: Boolean? = null,
    val get: Any? = null // TODO: don't know how to model that
) {

    override fun toString(): String {
        return "$_index $status $result $_id"
    }
}