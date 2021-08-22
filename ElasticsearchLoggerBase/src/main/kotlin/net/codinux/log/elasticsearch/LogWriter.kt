package net.codinux.log.elasticsearch


interface LogWriter {

    fun writeRecord(record: LogRecord)

}