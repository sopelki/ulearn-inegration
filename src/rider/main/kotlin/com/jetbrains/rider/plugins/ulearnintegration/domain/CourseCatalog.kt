package com.jetbrains.rider.plugins.ulearnintegration.domain

/** Все методы блокирующие (сеть): вызывать только из фонового потока. */
interface CourseCatalog {
    fun courses(): List<Course>
    fun exercises(courseId: String): List<Exercise>

    /** Ссылка на zip с проектом или null, если у задания проекта нет. */
    fun projectUrl(exercise: Exercise): String?
}