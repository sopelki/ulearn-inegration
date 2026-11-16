package com.jetbrains.rider.plugins.ulearnintegration.infrastructure

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.jetbrains.rider.plugins.ulearnintegration.domain.Exercise
import com.jetbrains.rider.plugins.ulearnintegration.domain.ProjectBinding
import java.nio.file.Files
import java.nio.file.Path

/** Чтение и запись файла .ulearn в корне проекта. Методы блокирующие (диск). */
object UlearnProjectFile {
    const val FILE_NAME = ".ulearn"
    private const val VERSION = 1

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun write(projectDir: Path, exercise: Exercise) {
        val file = projectDir.resolve(FILE_NAME)

        // Если файл уже есть, берём его содержимое и меняем только наши поля.
        val json = if (Files.exists(file)) {
            runCatching { JsonParser.parseString(Files.readString(file)).asJsonObject }
                .getOrNull() ?: JsonObject()
        } else {
            JsonObject()
        }

        json.addProperty("version", VERSION)
        json.addProperty("courseId", exercise.courseId)
        json.addProperty("slideId", exercise.id)

        Files.writeString(file, gson.toJson(json))
    }

    fun read(projectDir: Path): ProjectBinding? {
        val file = projectDir.resolve(FILE_NAME)
        if (!Files.exists(file)) return null
        return runCatching {
            val json = JsonParser.parseString(Files.readString(file)).asJsonObject
            ProjectBinding(json["courseId"].asString, json["slideId"].asString)
        }.getOrNull()   // битый или неполный файл считаем отсутствием привязки
    }
}