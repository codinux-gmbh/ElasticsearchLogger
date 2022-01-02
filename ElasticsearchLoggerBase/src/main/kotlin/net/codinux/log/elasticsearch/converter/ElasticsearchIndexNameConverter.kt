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
    val DatePatternRegex = Regex("%date\\{([0-9A-Za-z-_.:,'+]+)}")

    const val PatternsMaskString = "hijklmnopqrstuvwxyz-hijklmnopqrstuvwxyz-hijklmnopqrstuvwxyz-hijklmnopqrstuvwxyz"

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
    val patterns = getIncludedPatterns(indexNamePattern)
    var indexName = maskPatterns(indexNamePattern, patterns)

    indexName = removeInvalidCharacters(indexName, errorHandler)

    return unmaskPatterns(indexName, patterns)
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


  open fun resolvePatterns(indexNamePattern: String, patternsInIndexName: List<MatchResult>, errorHandler: ErrorHandler): String {

    var resolvedIndexName = indexNamePattern

    patternsInIndexName.forEach { match ->
      if (match.groupValues.size > 1) { // should always be the case, as matched the pattern, just to be on the safe side
        val datePattern = match.groupValues[1]
        try {
          val formatter = DateTimeFormatter.ofPattern(datePattern)
          resolvedIndexName = resolvedIndexName.replace(match.value, formatter.format(Instant.now().atOffset(ZoneOffset.UTC)))
        } catch (e: Exception) {
          errorHandler.logError("Could not convert date pattern '$datePattern' from index name '$indexNamePattern' with java.time.format.DateTimeFormatter", e)
        }
      }
    }

    return resolvedIndexName
  }

  protected open fun maskPatterns(indexNamePattern: String, patterns: List<MatchResult>): String {
    var maskedIndexNamePattern = indexNamePattern

    patterns.forEachIndexed { matchResultIndex, matchResult ->
      matchResult.groupValues.forEachIndexed { patternIndex, pattern ->
        maskedIndexNamePattern = maskedIndexNamePattern.replaceFirst(pattern, PatternsMaskString + "_" + matchResultIndex + "_" + patternIndex)
      }
    }

    return maskedIndexNamePattern
  }

  protected open fun unmaskPatterns(indexNamePattern: String, patterns: List<MatchResult>): String {
    var unmaskedIndexNamePattern = indexNamePattern

    patterns.forEachIndexed { matchResultIndex, matchResult ->
      matchResult.groupValues.forEachIndexed { patternIndex, pattern ->
        unmaskedIndexNamePattern = unmaskedIndexNamePattern.replaceFirst(PatternsMaskString + "_" + matchResultIndex + "_" + patternIndex, pattern)
      }
    }

    return unmaskedIndexNamePattern
  }

  open fun getIncludedPatterns(indexNamePattern: String): List<MatchResult> {
    return DatePatternRegex.findAll(indexNamePattern).toList()
  }

}