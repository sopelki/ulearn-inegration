package com.jetbrains.rider.plugins.ulearnintegration.infrastructure

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.jetbrains.rider.plugins.ulearnintegration.domain.Course
import com.jetbrains.rider.plugins.ulearnintegration.domain.CourseCatalog
import com.jetbrains.rider.plugins.ulearnintegration.domain.CourseUnit
import com.jetbrains.rider.plugins.ulearnintegration.domain.Exercise
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

// DTO: повторяют формат API, и только нужные поля.
// Всё nullable: Gson не проверяет типы Kotlin, и отсутствующее поле станет null.
private class CoursesResponse(val courses: List<CourseDto>?)
private class CourseDto(val id: String?, val title: String?)

private class CourseDetailsResponse(val units: List<UnitDto>?)
private class UnitDto(val title: String?, val slides: List<SlideDto>?)
private class SlideDto(
    val id: String?,
    val title: String?,
    val type: String?,
    val requiresReview: Boolean?,
    val maxScore: Int?,
)

private class SlideDetailsResponse(val blocks: List<BlockDto>?)
private class BlockDto(
    @SerializedName("\$type") val type: String?,
    val content: String?,
)

class UlearnHttpCatalog : CourseCatalog {

    private val gson = Gson()
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

    override fun courses(): List<Course> =
        gson.fromJson(get("/courses"), CoursesResponse::class.java).courses.orEmpty().mapNotNull { dto ->
            val id = dto.id ?: return@mapNotNull null
            Course(id, dto.title ?: id)
        }

    override fun units(courseId: String): List<CourseUnit> =
        gson.fromJson(get("/courses/$courseId"), CourseDetailsResponse::class.java)
            .units.orEmpty()
            .mapNotNull { unit ->
                val exercises = unit.slides.orEmpty()
                    .filter(::isProjectExercise)
                    .mapNotNull { slide ->
                        val id = slide.id ?: return@mapNotNull null
                        Exercise(courseId, id, slide.title ?: id, slide.maxScore ?: 0)
                    }
                if (exercises.isEmpty()) null
                else CourseUnit(unit.title ?: "Без названия", exercises)
            }

    override fun projectUrl(exercise: Exercise): String? {
        val slide = gson.fromJson(
            get("/slides/${exercise.courseId}/${exercise.id}"), SlideDetailsResponse::class.java
        )
        val url = slide.blocks.orEmpty().filter { it.type == "html" }.mapNotNull { it.content }
            .firstNotNullOfOrNull { html -> STUDENT_ZIP.find(html)?.groupValues?.get(1) } ?: return null

        return if (url.startsWith("/")) BASE_URL + url else url
    }

    /**
     * Единственное место, где решается, какие слайды считаем «практикой с проектом».
     * Признак косвенный: когда найдутся исключения, менять только здесь.
     */
    private fun isProjectExercise(slide: SlideDto) = slide.type == "exercise" && slide.requiresReview == true

    private fun get(path: String): String {
        val request = HttpRequest.newBuilder(URI.create(BASE_URL + path)).header("User-Agent", USER_AGENT)
            .timeout(Duration.ofSeconds(20)).GET().build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IOException("Ulearn ответил ${response.statusCode()} на $path")
        }
        return response.body()
    }

    private companion object {
        const val BASE_URL = "https://api.ulearn.me"
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        val STUDENT_ZIP = Regex("href=\"([^\"]*student-zip[^\"]*)\"")
    }
}