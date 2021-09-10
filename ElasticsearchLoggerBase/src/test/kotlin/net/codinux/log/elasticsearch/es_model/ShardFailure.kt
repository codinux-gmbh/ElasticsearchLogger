package net.codinux.log.elasticsearch.es_model


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