package net.codinux.log.elasticsearch.es_model

import com.fasterxml.jackson.annotation.JsonInclude


/**
 * See https://raw.githubusercontent.com/elastic/elasticsearch-specification/main/output/schema/schema.json
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class BulkResponse @JvmOverloads constructor(
    val took: Long,
    val errors: Boolean,
    val items: List<ResponseContainerItem>,
    val ingest_took: Long? = null
) {

    override fun toString(): String {
        return "errors? $errors. items: ${items.map { System.lineSeparator() + it.toString() }}"
    }
}