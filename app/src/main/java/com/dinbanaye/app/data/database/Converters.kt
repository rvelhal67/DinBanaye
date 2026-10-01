package com.dinbanaye.app.data.database

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromDoneDates(value: Set<String>?): String {
        return value?.joinToString("|") ?: ""
    }

    @TypeConverter
    fun toDoneDates(value: String?): Set<String> {
        if (value.isNullOrEmpty()) return emptySet()
        return value.split("|").filter { it.isNotEmpty() }.toSet()
    }
}
