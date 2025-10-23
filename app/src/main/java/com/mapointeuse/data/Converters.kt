package com.mapointeuse.data

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.format(dateFormatter)
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let {
            runCatching { LocalDate.parse(it, dateFormatter) }.getOrNull()
        }
    }

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.format(dateTimeFormatter)
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let {
            runCatching { LocalDateTime.parse(it, dateTimeFormatter) }.getOrNull()
        }
    }

    @TypeConverter
    fun fromStatutPointage(value: StatutPointage): String {
        return value.name
    }

    @TypeConverter
    fun toStatutPointage(value: String): StatutPointage {
        return runCatching { StatutPointage.valueOf(value) }
            .getOrElse { StatutPointage.EN_COURS }
    }
}
