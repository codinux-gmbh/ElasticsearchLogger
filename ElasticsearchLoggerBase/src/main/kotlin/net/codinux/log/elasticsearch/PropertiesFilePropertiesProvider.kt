package net.codinux.log.elasticsearch

import net.codinux.log.elasticsearch.errorhandler.StdErrErrorHandler
import java.lang.Exception
import java.util.*


open class PropertiesFilePropertiesProvider : PropertyProviderBase() {

    companion object {
        const val FILE_NAME = "/logging.properties"
    }


    private val properties = Properties()

    init {
        loadPropertiesFile()
    }


    protected open fun loadPropertiesFile() {
        try {
            var inputStream = PropertiesFilePropertiesProvider::class.java.getResourceAsStream(FILE_NAME)
            val thread = Thread.currentThread()
            if (inputStream == null && thread.contextClassLoader != null) {
                inputStream = thread.contextClassLoader.getResourceAsStream(FILE_NAME)
            }
            properties.load(inputStream)
        } catch (e: Exception) {
            StdErrErrorHandler().logError("Could not read logging properties from " + FILE_NAME , e) // TODO: make ErrorHandler configurable
        }
    }

    override fun getProperty(propertyName: String): String? {
        return properties.getProperty(propertyName)
    }

}