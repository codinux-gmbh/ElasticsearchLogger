package net.codinux.log.elasticsearch.converter

import net.codinux.log.elasticsearch.errorhandler.ErrorHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
    assertValidDateTimePatternAtEnd("yyyy-MM-dd HH:mm:ss")
  }

  private fun assertValidDateTimePatternAtEnd(dateTimePattern: String) {
    // given
    val expectedDateString = DateTimeFormatter.ofPattern(dateTimePattern).format(Instant.now().atOffset(ZoneOffset.UTC))
    val errorHandlerMock = mock(ErrorHandler::class.java)


    // when
    val result = underTest.createIndexName("logs-%date{$dateTimePattern}", errorHandlerMock)


    // then
    assertThat(result).isEqualTo("logs-" + expectedDateString)

    verify(errorHandlerMock, never()).logError(anyString()) // no errors occurred
    verify(errorHandlerMock, never()).logError(anyString(), any(Exception::class.java))
  }


  @Test
  fun validDatePatternAtStart() {
    assertValidDateTimePatternAtStart("yyyy-MM-dd")
  }

  @Test
  fun validDateTimePatternAtStart() {
    assertValidDateTimePatternAtStart("yyyy-MM-dd HH:mm:ss")
  }

  private fun assertValidDateTimePatternAtStart(dateTimePattern: String) {
    // given
    val expectedDateString = DateTimeFormatter.ofPattern(dateTimePattern).format(Instant.now().atOffset(ZoneOffset.UTC))
    val errorHandlerMock = mock(ErrorHandler::class.java)


    // when
    val result = underTest.createIndexName("%date{$dateTimePattern}-logs", errorHandlerMock)


    // then
    assertThat(result).isEqualTo(expectedDateString + "-logs")

    verify(errorHandlerMock, never()).logError(anyString()) // no errors occurred
    verify(errorHandlerMock, never()).logError(anyString(), any(Exception::class.java))
  }


  @Test
  fun validDatePatternInString() {
    assertValidDateTimePatternInString("yyyy-MM-dd")
  }

  @Test
  fun validDateTimePatternInString() {
    assertValidDateTimePatternInString("yyyy-MM-dd HH:mm:ss")
  }

  private fun assertValidDateTimePatternInString(dateTimePattern: String) {
    // given
    val expectedDateString = DateTimeFormatter.ofPattern(dateTimePattern).format(Instant.now().atOffset(ZoneOffset.UTC))
    val errorHandlerMock = mock(ErrorHandler::class.java)


    // when
    val result = underTest.createIndexName("logs-%date{$dateTimePattern}-index", errorHandlerMock)


    // then
    assertThat(result).isEqualTo("logs-" + expectedDateString + "-index")

    verify(errorHandlerMock, never()).logError(anyString()) // no errors occurred
    verify(errorHandlerMock, never()).logError(anyString(), any(Exception::class.java))
  }


  @Test
  fun invalidDateTimePattern() {
    // given
    val invalidDateTimePattern = "aaaa-BB-cc"
    val indexNamePattern = "logs-%date{$invalidDateTimePattern}"
    val errorHandlerMock = mock(ErrorHandler::class.java)


    // when
    val result = underTest.createIndexName(indexNamePattern, errorHandlerMock)


    // then
    assertThat(result).isEqualTo(indexNamePattern) // nothing has been replaced

    verify(errorHandlerMock, times(1)).logError(anyString(), any(Exception::class.java))
  }

}