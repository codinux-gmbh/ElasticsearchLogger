package net.codinux.log.elasticsearch

import java.util.logging.LogManager


open class JavaUtilLogPropertyProvider : PropertyProviderBase() {

    protected open val logManager: LogManager = LogManager.getLogManager()

    protected open val fallbackProvider: PropertyProviderBase = PropertiesFilePropertiesProvider()


    override fun getProperty(propertyName: String): String? {
        return logManager.getProperty(propertyName)
                ?: fallbackProvider.getProperty(propertyName)
    }

}