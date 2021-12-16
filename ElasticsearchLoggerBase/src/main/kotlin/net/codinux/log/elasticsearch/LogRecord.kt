package net.codinux.log.elasticsearch

import net.codinux.log.elasticsearch.kubernetes.KubernetesInfo
import java.time.Instant


open class LogRecord @JvmOverloads constructor(
    open val message: String,

    open val timestamp: Instant,

    open val level: String,

    open val logger: String,

    open val threadName: String,

    open val host: String,

    open val exception: Throwable? = null,

    open val mdc: Map<String, String>? = null,

    open var marker: String? = null,

    open var kubernetesInfo: KubernetesInfo? = null
) {

    override fun toString(): String {
        return "$timestamp $level $message"
    }

}