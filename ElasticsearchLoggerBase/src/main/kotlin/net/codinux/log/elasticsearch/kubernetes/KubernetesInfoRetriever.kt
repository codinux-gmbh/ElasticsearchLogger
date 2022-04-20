package net.codinux.log.elasticsearch.kubernetes

import io.fabric8.kubernetes.client.DefaultKubernetesClient
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory
import java.io.File
import java.net.InetAddress
import java.time.Instant

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread


open class KubernetesInfoRetriever {

    companion object {
        private val StartTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

        private val log = LoggerFactory.getLogger(KubernetesInfoRetriever::class.java)
    }


    open fun retrieveKubernetesInfoAsync(callback: (KubernetesInfo?) -> Unit) {
        thread {
            callback(retrieveKubernetesInfo())
        }
    }

    open fun retrieveKubernetesInfo(): KubernetesInfo? {
        try {
            val namespaceFile = File("/run/secrets/kubernetes.io/serviceaccount/namespace")
            if (namespaceFile.exists() == false) {
                log.info("Not running in a Kubernetes environment, file '/run/secrets/kubernetes.io/serviceaccount/namespace' does not exist.")
                return null
            }

            val namespace = namespaceFile.readText()

            val localHost = InetAddress.getLocalHost()
            val podName = localHost.hostName
            val podIp = localHost.hostAddress

            return retrieveKubernetesInfo(namespace, podName, podIp)
        } catch(e: Exception) {
            log.error("Could not retrieve pod info, no cluster or container info will be added to logs", e)
        }

        return null
    }

    open fun retrieveKubernetesInfo(namespace: String, podName: String, podIp: String): KubernetesInfo? {
        var containerName: String? = null
        var podIp = podIp
        var nodeIp: String? = null
        var nodeName: String? = null
        var startTime: String = Instant.now().atOffset(ZoneOffset.UTC).format(StartTimeFormat)

        var containerId: String? = null
        var imageName: String? = null
        var imageId: String? = null
        var restartCount = 0
        var uid: String? = null
        var clusterName: String? = null

        var labels: Map<String, String> = mapOf()
        var annotations: Map<String, String> = mapOf()

        val client = DefaultKubernetesClient()

        val pod = client.pods().inNamespace(namespace).withName(podName)?.get()

        pod?.apply {
            metadata?.apply {
                uid = this.uid // a string like 2e8e9f1e-7df3-48d4-a0fa-908b042bc63b // TODO: needed?
                clusterName = this.clusterName // will this ever be != null ?

                labels = this.labels ?: mapOf()
                annotations = this.annotations ?: mapOf()
            }

            spec?.apply {
                nodeName = this.nodeName

                // TODO: find the container we are running in. Till then we simply take the first container
                spec.containers?.firstOrNull()?.let { container ->
                    containerName = container.name
                    imageName = container.image // TODO: extract imageName and version from "registry.gitlab.com/codinux-gmbh/bankingrestapi:alpha10-04"
                }
            }

            status?.apply {
                podIp = podIP
                nodeIp = hostIP
                startTime = this.startTime

                // TODO: find the container we are running in. Till then we simply take the first container
                containerStatuses?.firstOrNull()?.let { containerStatus ->
                    containerName = containerStatus.name
                    containerId = containerStatus.containerID // TODO: extract containerId from "containerd://54347805363989984bd17380fcc93c0146cbbf98fd22bcefeb8949258efbacbb" docker://694c2546560bd5b05789b6b53ac2f25941003feff12ff00fdfe68384dac4f334
                    imageName = containerStatus.image // TODO: extract imageName and version from "registry.gitlab.com/codinux-gmbh/bankingrestapi:alpha10-04"
                    imageId = containerStatus.imageID // TODO: extract imageName and containerHash from "registry.gitlab.com/codinux-gmbh/bankingrestapi@sha256:af038c686d5f4d896ff18ce3947b6679cd00e7b51f2e72dc4e399472b6c2ff93" docker-pullable://ippendigital/cms-core-page-api@sha256:6f60adfab8f731974f02aad9dec9d8407ab345a1dc7a9a688e1ab4f144adcb14
                    restartCount = containerStatus.restartCount
                }
            }
        }

        return KubernetesInfo(namespace, podName, podIp, startTime, uid, restartCount, containerName, containerId, imageName, imageId, nodeIp, nodeName,
            clusterName, labels, annotations)
    }

}