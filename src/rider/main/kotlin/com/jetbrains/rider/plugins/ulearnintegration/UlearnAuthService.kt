package com.jetbrains.rider.plugins.ulearnintegration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.rd.ide.model.LoginRequest
import com.jetbrains.rd.ide.model.LoginResponse
import com.jetbrains.rd.ide.model.ulearnModel
import com.jetbrains.rd.platform.util.lifetime
import com.jetbrains.rider.projectView.solution

// Аннотация @Service регистрирует класс в DI платформы
@Service(Service.Level.PROJECT)
class UlearnAuthService(private val project: Project) {

    // Лямбда callback: (LoginResponse) -> Unit это как Action<LoginResponse> в C#
    fun login(login: String, password: String, onResult: (LoginResponse) -> Unit) {
        val model = project.solution.ulearnModel
        val lifetime = project.lifetime

        val request = LoginRequest(login, password)

        model.login.start(lifetime, request).result.advise(lifetime) { rdResult ->
            val response = rdResult.unwrap()

            // Возвращаем результат в UI-поток
            ApplicationManager.getApplication().invokeLater {
                onResult(response)
            }
        }
    }

    companion object {
        // Метод для удобного получения сервиса: UlearnAuthService.getInstance(project)
        fun getInstance(project: Project): UlearnAuthService = project.service()
    }
}