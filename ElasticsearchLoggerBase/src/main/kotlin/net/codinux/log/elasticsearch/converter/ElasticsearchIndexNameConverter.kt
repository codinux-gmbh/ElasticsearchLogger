package net.codinux.log.elasticsearch.converter

import net.codinux.log.elasticsearch.errorhandler.ErrorHandler
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


open class ElasticsearchIndexNameConverter {

  companion object {

    @JvmStatic
    val DatePatternRegex = Regex("%date\\{(.+)}")

  }


  open fun createIndexName(indexNamePattern: String, errorHandler: ErrorHandler): String {
    var indexName = indexNamePattern

    DatePatternRegex.find(indexNamePattern)?.let { match ->
      if (match.groupValues.size > 1) { // should always be the case, as matched the pattern, just to be on the safe side
        val datePattern = match.groupValues[1]
        try {
          val formatter = DateTimeFormatter.ofPattern(datePattern)
          indexName = indexName.replace(match.value, formatter.format(Instant.now().atOffset(ZoneOffset.UTC)))
        } catch (e: Exception) {
          errorHandler.logError("Could not convert date pattern '$datePattern' from index name '$indexName' with java.time.format.DateTimeFormatter", e)
        }
      }
    }

    return indexName
  }

}