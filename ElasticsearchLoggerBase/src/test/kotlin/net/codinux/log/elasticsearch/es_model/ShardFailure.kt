package net.codinux.log.elasticsearch.es_model

import com.fasterxml.jackson.annotation.JsonInclude


@JsonInclude(JsonInclude.Include.NON_NULL)
class ShardFailure @JvmOverloads constructor(
    val reason: String,
    val shard: String,
    val index: IndexName? = null,
    val node: String? = null,
    val status: String? = null
) {

    override fun toString(): String {
        return "$index $reason"
    }

}