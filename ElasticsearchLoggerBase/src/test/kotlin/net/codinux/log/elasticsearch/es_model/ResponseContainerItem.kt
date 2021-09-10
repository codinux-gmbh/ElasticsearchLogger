package net.codinux.log.elasticsearch.es_model

import com.fasterxml.jackson.annotation.JsonInclude


/**
 * See https://raw.githubusercontent.com/elastic/elasticsearch-specification/main/output/schema/schema.json
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class ResponseContainerItem @JvmOverloads constructor(
    val index: ResponseItemBase? = null,
    val create: ResponseItemBase? = null,
    val update: ResponseItemBase? = null,
    val delete: ResponseItemBase? = null
) {

    override fun toString(): String {
        return "index = $index\ncreate = $create\nupdate = $update\ndelete = $delete"
    }
}