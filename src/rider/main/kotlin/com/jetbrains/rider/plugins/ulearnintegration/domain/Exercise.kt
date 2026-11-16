package com.jetbrains.rider.plugins.ulearnintegration.domain

data class Exercise(
    val courseId: String,
    val id: String,
    val title: String,
    val maxScore: Int,
)