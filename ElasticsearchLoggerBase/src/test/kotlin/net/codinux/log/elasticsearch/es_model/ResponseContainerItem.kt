package net.codinux.log.elasticsearch.es_model


/**
 * See https://raw.githubusercontent.com/elastic/elasticsearch-specification/main/output/schema/schema.json
 */
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