package net.codinux.log.elasticsearch.kubernetes


open class KubernetesInfo(
        open val namespace: String,
        open val podName: String,
        open val podIp: String,
        open val startTime: String,
        open val podUid: String? = null,
        open val restartCount: Int? = null,
        open val containerName: String? = null,
        open val containerId: String? = null,
        open val imageName: String? = null,
        open val imageId: String? = null,
        open val nodeIp: String? = null,
        open val nodeName: String? = null,
        open val clusterName: String? = null,
        open val labels: Map<String, String> = mapOf(),
        open val annotations: Map<String, String> = mapOf()
)