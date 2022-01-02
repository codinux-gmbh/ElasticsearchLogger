package net.codinux.log.elasticsearch.converter

import net.codinux.log.elasticsearch.errorhandler.ErrorHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.*

import org.mockito.kotlin.never
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

internal class ElasticsearchIndexNameConverterTest {

  private val underTest = ElasticsearchIndexNameConverter()


  @Test
  fun validDatePatternAtEnd() {
    assertValidDateTimePatternAtEnd("yyyy-MM-dd")
  }

  @Test
  fun validDateTimePatternAtEnd() {
    assertValidDateTimePatternAtEnd("yyyy-MM-dd_HH-mm-ss")
  }

  private fun assertValidDateTimePatternAtEnd(dateTimePattern: String) {
    // given
    val indexNamePattern = "logs-%date{$dateTimePattern}"
    val expectedDateString = DateTimeFormatter.ofPattern(dateTimePattern).format(Instant.now().atOffset(ZoneOffset.UTC))
    val errorHandlerMock = mock(ErrorHandler::class.java)


    // when
    val result = resolvePatterns(indexNamePattern, errorHandlerMock)


    // then
    assertThat(result).isEqualTo("logs-" + expectedDateString)

    assertNoErrorOccurred(errorHandlerMock)
  }


  @Test
  fun validDatePatternAtStart() {
    assertValidDateTimePatternAtStart("yyyy-MM-dd")
  }

  @Test
  fun validDateTimePatternAtStart() {
    assertValidDateTimePatternAtStart("yyyy-MM-dd_HH-mm-ss")
  }

  private fun assertValidDateTimePatternAtStart(dateTimePattern: String) {
    // given
    val indexNamePattern = "%date{$dateTimePattern}-logs"
    val expectedDateString = DateTimeFormatter.ofPattern(dateTimePattern).format(Instant.now().atOffset(ZoneOffset.UTC))
    val errorHandlerMock = mock(ErrorHandler::class.java)


    // when
    val result = resolvePatterns(indexNamePattern, errorHandlerMock)


    // then
    assertThat(result).isEqualTo(expectedDateString + "-logs")

    assertNoErrorOccurred(errorHandlerMock)
  }


  @Test
  fun validDatePatternInString() {
    assertValidDateTimePatternInString("yyyy-MM-dd")
  }

  @Test
  fun validDateTimePatternInString() {
    assertValidDateTimePatternInString("yyyy-MM-dd_HH-mm-ss")
  }

  private fun assertValidDateTimePatternInString(dateTimePattern: String) {
    // given
    val indexNamePattern = "logs-%date{$dateTimePattern}-index"
    val expectedDateString = DateTimeFormatter.ofPattern(dateTimePattern).format(Instant.now().atOffset(ZoneOffset.UTC))
    val errorHandlerMock = mock(ErrorHandler::class.java)


    // when
    val result = resolvePatterns(indexNamePattern, errorHandlerMock)


    // then
    assertThat(result).isEqualTo("logs-" + expectedDateString + "-index")

    assertNoErrorOccurred(errorHandlerMock)
  }

  @Test
  fun multipleDateTimePatternInString() {
    // given
    val indexNamePattern = "logs-%date{yyyy-ww}-%date{dd_HH-mm-ss}"
    val expectedIndexName = indexNamePattern.replace("%date{yyyy-ww}", DateTimeFormatter.ofPattern("yyyy-ww").format(Instant.now().atOffset(ZoneOffset.UTC)))
                                            .replace("%date{dd_HH-mm-ss}", DateTimeFormatter.ofPattern("dd_HH-mm-ss").format(Instant.now().atOffset(ZoneOffset.UTC)))
    val errorHandlerMock = mock(ErrorHandler::class.java)


    // when
    val result = resolvePatterns(indexNamePattern, errorHandlerMock)


    // then
    assertThat(result).isEqualTo(expectedIndexName)

    assertNoErrorOccurred(errorHandlerMock)
  }


  @Test
  fun invalidDateTimePattern() {
    // given
    val invalidDateTimePattern = "aaaa-bb-cc"
    val indexNamePattern = "logs-%date{$invalidDateTimePattern}"
    val errorHandlerMock = mock(ErrorHandler::class.java)


    // when
    val result = resolvePatterns(indexNamePattern, errorHandlerMock)


    // then
    assertThat(result).isEqualTo(indexNamePattern) // nothing has been replaced

    verify(errorHandlerMock, times(1)).logError(anyString(), any(Exception::class.java))
  }


  @Test
  fun validDateTimePatternDoesNotGetReplaced() {
    // given
    val indexNamePattern = "logs-%date{yyyy-dd_HH-mm-ss}"
    val errorHandlerMock = mock(ErrorHandler::class.java)


    // when
    val result = underTest.buildIndexName(indexNamePattern, errorHandlerMock) // checks if masking date time patterns works


    // then
    assertThat(result).isEqualTo(indexNamePattern) // nothing may gets replaced then

    verify(errorHandlerMock, never()).logError(anyString(), any(Exception::class.java))
  }

  @Test
  fun validDateTimePatternDoesNotGetReplaced_TwoDateTimePattern() {
    // given
    val indexNamePattern = "logs-%date{yyyy-ww}-%date{dd_HH-mm-ss}"
    val errorHandlerMock = mock(ErrorHandler::class.java)


    // when
    val result = underTest.buildIndexName(indexNamePattern, errorHandlerMock) // checks if masking date time patterns works


    // then
    assertThat(result).isEqualTo(indexNamePattern) // nothing may gets replaced then

    verify(errorHandlerMock, never()).logError(anyString(), any(Exception::class.java))
  }


  @ParameterizedTest
  @ValueSource(chars = [ '\\', '/', '*', '?', '\"', '<', '>', '|', ' ', ',', ':' ])
  fun indexNameContainsInvalidCharacter(invalidIndexNameCharacter: Char) {
    // given
    val invalidIndexName = "logs-" + invalidIndexNameCharacter
    val errorHandlerMock = mock(ErrorHandler::class.java)


    // when
    val result = underTest.buildIndexName(invalidIndexName, errorHandlerMock)


    // then
    assertThat(result).isNotEqualTo(invalidIndexName)
    assertThat(result).doesNotContain(invalidIndexNameCharacter.toString())
    assertThat(result).isEqualTo(invalidIndexName.replace(invalidIndexNameCharacter.toString(), ElasticsearchIndexNameConverter.DefaultInvalidCharacterReplacement))

    verify(errorHandlerMock, times(1)).logInfo(anyString())
  }

  @ParameterizedTest
  @ValueSource(chars = [ '_', '-', '+' ])
  fun indexNameStartsWithInvalidCharacter(invalidIndexNameStartCharacter: Char) {
    // given
    val invalidIndexName = invalidIndexNameStartCharacter + "-logs"
    val errorHandlerMock = mock(ErrorHandler::class.java)


    // when
    val result = underTest.buildIndexName(invalidIndexName, errorHandlerMock)


    // then
    assertThat(result).isNotEqualTo(invalidIndexName)
    assertThat(result).doesNotContain(invalidIndexNameStartCharacter.toString())
    assertThat(result).isEqualTo("logs")

    verify(errorHandlerMock, times(2)).logInfo(anyString())
  }

  @ParameterizedTest
  @ValueSource(strings = [ ".", ".." ])
  fun indexNameEqualsInvalidName(invalidIndexName: String) {
    // given
    val errorHandlerMock = mock(ErrorHandler::class.java)


    // when
    val result = underTest.buildIndexName(invalidIndexName, errorHandlerMock) // default replacement character "_" is an invalid index name start character


    // then
    assertThat(result).isNotEqualTo(invalidIndexName)
    assertThat(result).isEqualTo("logs")

    verify(errorHandlerMock, times(1)).logError(anyString())
  }

  @ParameterizedTest
  @ValueSource(strings = [ ".", ".." ])
  fun indexNameEqualsInvalidName_ReplacementCharacterIsNotAnInvalidIndexNameStartCharacter(invalidIndexName: String) {
    // given
    val replacementCharacter = "!"
    val underTest = ElasticsearchIndexNameConverter(replacementCharacter)
    val errorHandlerMock = mock(ErrorHandler::class.java)


    // when
    val result = underTest.buildIndexName(invalidIndexName, errorHandlerMock)


    // then
    assertThat(result).isNotEqualTo(invalidIndexName)
    assertThat(result).isEqualTo(replacementCharacter)

    verify(errorHandlerMock, times(1)).logError(anyString())
  }

  @ParameterizedTest
  @ValueSource(strings = [ "Logs", "lOgs", "logS" ])
  fun indexNameContainsUpperCaseCharacters(invalidIndexName: String) {
    // given
    val errorHandlerMock = mock(ErrorHandler::class.java)


    // when
    val result = underTest.buildIndexName(invalidIndexName, errorHandlerMock)


    // then
    assertThat(result).isNotEqualTo(invalidIndexName)
    assertThat(result).doesNotContain(invalidIndexName)
    assertThat(result).isEqualTo("logs")

    verify(errorHandlerMock, times(1)).logInfo(anyString())
  }


  private fun resolvePatterns(indexNamePattern: String, errorHandlerMock: ErrorHandler): String {
    return underTest.resolvePatterns(indexNamePattern, underTest.getIncludedPatterns(indexNamePattern), errorHandlerMock)
  }

  private fun assertNoErrorOccurred(errorHandlerMock: ErrorHandler) {
    verify(errorHandlerMock, never()).logError(anyString())
    verify(errorHandlerMock, never()).logError(anyString(), any(Exception::class.java))
  }

}