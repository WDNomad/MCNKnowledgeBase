package ru.mcn.knowledgebase.data.local

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromImages(images: List<String>): String =
        images.joinToString("|")

    @TypeConverter
    fun toImages(value: String): List<String> =
        if (value.isBlank()) emptyList()
        else value.split("|")
}