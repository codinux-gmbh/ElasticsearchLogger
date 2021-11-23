package net.codinux.log.elasticsearch


class TimestampFormatConverter {

  val MILLIS_FIELD_VALUE = "millis"

  val FORMATTED_FIELD_VALUE = "formatted"


  fun convert(value: String): TimestampFormat {
    return when (value.toLowerCase()) {
      MILLIS_FIELD_VALUE -> TimestampFormat.MILLIS_SINCE_EPOCH
      FORMATTED_FIELD_VALUE -> TimestampFormat.FORMATTED_DATE_TIME
      else -> TimestampFormat.valueOf(value.toUpperCase())
    }
  }

}