package com.neilyin.nnowork.data.backup

import java.time.Instant
import java.time.temporal.ChronoUnit

/** iOS JSONEncoder/Decoder `.iso8601` 的跨平台日期轉換。 */
object BackupDateCodec {
    const val EPOCH_ISO = "1970-01-01T00:00:00Z"

    /**
     * Swift Foundation 的 `.iso8601` 備份以秒為精度。這裡同樣截至整秒，
     * 避免帶小數秒的 Android 日期無法被舊版 Foundation 解碼。
     */
    fun format(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
        .truncatedTo(ChronoUnit.SECONDS)
        .toString()

    fun parse(value: String): Long = Instant.parse(value).toEpochMilli()
}
