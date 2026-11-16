package com.jetbrains.rider.plugins.ulearnintegration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
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
}