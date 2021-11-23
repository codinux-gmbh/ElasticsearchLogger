package net.codinux.log.elasticsearch


class TimestampResolutionConverter {

  companion object {

    const val MillisecondsConfigValue = "millis"

    const val MicrosecondsConfigValue = "micros"

    const val NanosecondsConfigValue = "nanos"

  }


  fun convert(value: String): TimestampResolution {
    return when (value.toLowerCase()) {
      MillisecondsConfigValue -> TimestampResolution.Milliseconds
      MicrosecondsConfigValue -> TimestampResolution.Microseconds
      NanosecondsConfigValue -> TimestampResolution.Nanoseconds
      else -> TimestampResolution.valueOf(value.toUpperCase())
    }
  }

}