package com.jetbrains.rider.plugins.ulearnintegration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.jetbrains.rider.plugins.ulearnintegration.domain.Course
import com.jetbrains.rider.plugins.ulearnintegration.domain.CourseCatalog
import com.jetbrains.rider.plugins.ulearnintegration.infrastructure.UlearnHttpCatalog

@Service(Service.Level.APP)
class UlearnCatalogService {

    val catalog: CourseCatalog = UlearnHttpCatalog()

    /** Выполняет work в фоне, а onDone вызывает в UI-потоке. */
    fun <T> loadAsync(work: () -> T, onDone: (Result<T>) -> Unit) {
        val app = ApplicationManager.getApplication()
        app.executeOnPooledThread {
            val result = runCatching(work)
            app.invokeLater { onDone(result) }
        }
    }

    /** Курсы с учётом белого списка. Блокирующий: только из фона. */
    fun visibleCourses(): List<Course> {
        val all = catalog.courses()
        val enabled = service<UlearnSettings>().state.enabledCourseIds
        return if (enabled.isEmpty()) all else all.filter { it.id in enabled }
    }
}