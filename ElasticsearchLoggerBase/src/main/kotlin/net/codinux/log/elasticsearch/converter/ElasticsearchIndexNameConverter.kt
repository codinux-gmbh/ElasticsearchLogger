package net.codinux.log.elasticsearch.converter

import net.codinux.log.elasticsearch.errorhandler.ErrorHandler
import org.elasticsearch.common.Strings
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*


open class ElasticsearchIndexNameConverter @JvmOverloads constructor(
  protected open val invalidCharactersReplacement: String = DefaultInvalidCharacterReplacement
) {

  companion object {

    const val DefaultInvalidCharacterReplacement = "_"

    @JvmStatic
    val InvalidIndexNameStartCharacters = listOf('_', '-', '+')

    @JvmStatic
    val InvalidIndexNameCharacters: Set<Char>

    @JvmStatic
    val UpperCaseRegex = Regex("[A-Z]")

    @JvmStatic
    val DatePatternRegex = Regex("%date\\{(.+)}")

    init {
      val invalidCharacters = Strings.INVALID_FILENAME_CHARS.toMutableSet()
      invalidCharacters.add(':')

      InvalidIndexNameCharacters = Collections.unmodifiableSet(invalidCharacters)
    }

  }


  /**
   * Removes invalid characters from [indexNamePattern] and resolves patterns like %date{yyyy-MM-dd} to a date time.
   */
  open fun buildIndexName(indexNamePattern: String, errorHandler: ErrorHandler): String {
    val indexName = resolvePatterns(indexNamePattern, errorHandler)

    return removeInvalidCharacters(indexName, errorHandler)
  }


  /**
   * See [https://github.com/elastic/elasticsearch/blob/a0fda84da59a14dabce08dfc9b1f445d2cc71e55/server/src/test/java/org/elasticsearch/cluster/metadata/MetadataCreateIndexServiceTests.java#L582-L594]
   * for invalid characters.
   */
  protected open fun removeInvalidCharacters(indexName: String, errorHandler: ErrorHandler): String {
    var validIndexName = indexName

    if (validIndexName == "." || validIndexName == "..") {
      val replacement = if (invalidCharactersReplacement.length == 1 && InvalidIndexNameStartCharacters.contains(invalidCharactersReplacement[0])) "logs"
      else invalidCharactersReplacement
      errorHandler.logError("The index name may not be '.' or '..'. Replacing it with '$replacement'.")
      validIndexName = validIndexName.replace(validIndexName, replacement)
    }

    if (Strings.validFileName(indexName) == false || indexName.contains(':')) {
      InvalidIndexNameCharacters.forEach { invalidCharacter ->
        if (validIndexName.contains(invalidCharacter)) {
          errorHandler.logInfo("Elasticsearch index names may not contain '$invalidCharacter'. Replacing it with '$invalidCharactersReplacement'.")

          validIndexName = validIndexName.replace(invalidCharacter.toString(), invalidCharactersReplacement)
        }
      }
    }

    // remove invalid index name start characters: _, - and +
    while (validIndexName.isNotEmpty() && InvalidIndexNameStartCharacters.contains(validIndexName[0])) {
      errorHandler.logInfo("Elasticsearch index names may not start with '${validIndexName[0]}'. Replacing it with '$invalidCharactersReplacement'.")
      validIndexName = validIndexName.substring(1)
    }

    if (UpperCaseRegex.containsMatchIn(validIndexName)) {
      errorHandler.logInfo("Index name '$validIndexName' may not contain upper case characters. Converting it to '${validIndexName.toLowerCase()}'.")
      validIndexName = validIndexName.toLowerCase()
    }

    return validIndexName
  }

  protected open fun resolvePatterns(indexNamePattern: String, errorHandler: ErrorHandler): String {

    DatePatternRegex.find(indexNamePattern)?.let { match ->
      if (match.groupValues.size > 1) { // should always be the case, as matched the pattern, just to be on the safe side
        val datePattern = match.groupValues[1]
        try {
          val formatter = DateTimeFormatter.ofPattern(datePattern)
          return indexNamePattern.replace(match.value, formatter.format(Instant.now().atOffset(ZoneOffset.UTC)))
        } catch (e: Exception) {
          errorHandler.logError("Could not convert date pattern '$datePattern' from index name '$indexNamePattern' with java.time.format.DateTimeFormatter", e)
        }
      }
    }
    return indexNamePattern
  }

}