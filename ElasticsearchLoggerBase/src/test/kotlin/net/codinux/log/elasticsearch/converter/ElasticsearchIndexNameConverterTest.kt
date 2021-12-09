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
    val expectedDateString = DateTimeFormatter.ofPattern(dateTimePattern).format(Instant.now().atOffset(ZoneOffset.UTC))
    val errorHandlerMock = mock(ErrorHandler::class.java)


    // when
    val result = underTest.buildIndexName("logs-%date{$dateTimePattern}", errorHandlerMock)


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
    val expectedDateString = DateTimeFormatter.ofPattern(dateTimePattern).format(Instant.now().atOffset(ZoneOffset.UTC))
    val errorHandlerMock = mock(ErrorHandler::class.java)


    // when
    val result = underTest.buildIndexName("%date{$dateTimePattern}-logs", errorHandlerMock)


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
    val expectedDateString = DateTimeFormatter.ofPattern(dateTimePattern).format(Instant.now().atOffset(ZoneOffset.UTC))
    val errorHandlerMock = mock(ErrorHandler::class.java)


    // when
    val result = underTest.buildIndexName("logs-%date{$dateTimePattern}-index", errorHandlerMock)


    // then
    assertThat(result).isEqualTo("logs-" + expectedDateString + "-index")

    assertNoErrorOccurred(errorHandlerMock)
  }


  @Test
  fun invalidDateTimePattern() {
    // given
    val invalidDateTimePattern = "aaaa-bb-cc"
    val indexNamePattern = "logs-%date{$invalidDateTimePattern}"
    val errorHandlerMock = mock(ErrorHandler::class.java)


    // when
    val result = underTest.buildIndexName(indexNamePattern, errorHandlerMock)


    // then
    assertThat(result).isEqualTo(indexNamePattern) // nothing has been replaced

    verify(errorHandlerMock, times(1)).logError(anyString(), any(Exception::class.java))
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


  private fun assertNoErrorOccurred(errorHandlerMock: ErrorHandler) {
    verify(errorHandlerMock, never()).logError(anyString())
    verify(errorHandlerMock, never()).logError(anyString(), any(Exception::class.java))
  }

}