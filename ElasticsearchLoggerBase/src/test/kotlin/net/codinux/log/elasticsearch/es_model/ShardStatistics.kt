package net.codinux.log.elasticsearch.es_model

import com.fasterxml.jackson.annotation.JsonInclude


@JsonInclude(JsonInclude.Include.NON_NULL)
class ShardStatistics @JvmOverloads constructor(
    val total: Int, // actual: UInt
    val successful: Int, // actual: UInt
    val failed: Int, // actual: UInt
    val skipped: Int? = null, // actual: UInt
    val failures: List<ShardFailure> = listOf()
) {

    override fun toString(): String {
        return "total $total, successful $successful, failed $failed, skipped $skipped"
    }

}