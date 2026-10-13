package com.astute.pause.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import java.time.Instant

class Converters {
    @TypeConverter fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
    @TypeConverter fun stringsToJson(value: List<String>): String = Json.encodeToString(value)
    @TypeConverter fun jsonToStrings(value: String): List<String> = Json.decodeFromString(value)
    @TypeConverter fun longsToJson(value: List<Long>): String = Json.encodeToString(value)
    @TypeConverter fun stringMapToJson(value: Map<String, String>): String = Json.encodeToString(value)
    @TypeConverter fun jsonToStringMap(value: String): Map<String, String> = Json.decodeFromString(value)
    @TypeConverter fun jsonToLongs(value: String): List<Long> = Json.decodeFromString(value)
}
