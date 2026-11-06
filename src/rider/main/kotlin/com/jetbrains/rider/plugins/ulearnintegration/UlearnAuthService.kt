package com.jetbrains.rider.plugins.ulearnintegration

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.rd.ide.model.LoginRequest
import com.jetbrains.rd.ide.model.LoginResponse
import com.jetbrains.rd.ide.model.UserInfo
import com.jetbrains.rd.ide.model.ulearnModel
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rider.projectView.solution

@Service(Service.Level.PROJECT)
class UlearnAuthService(private val project: Project) : Disposable {

    private val lifetimeDef = LifetimeDefinition()

    // Лямбда callback: (LoginResponse) -> Unit это как Action<LoginResponse> в C#
    fun login(login: String, password: String, onResult: (LoginResponse) -> Unit) {
        val model = project.solution.ulearnModel
        val lifetime = lifetimeDef.lifetime

        val request = LoginRequest(login, password)

        model.login.start(lifetime, request).result.advise(lifetime) { rdResult ->
            val response = try {
                rdResult.unwrap()
            } catch (e: Throwable) {
                LoginResponse(false, e.message ?: "Неизвестная ошибка")
            }

            // Возвращаем результат в UI-поток
            ApplicationManager.getApplication().invokeLater {
                onResult(response)
            }
        }
    }

    fun logout() {
        project.solution.ulearnModel.logout.start(lifetimeDef.lifetime, Unit)
    }

    fun onUserChanged(parent: Disposable, listener: (UserInfo?) -> Unit) {
        val nested = lifetimeDef.lifetime.createNested()
        Disposer.register(parent) { nested.terminate() }

        project.solution.ulearnModel.currentUser.advise(nested) { user ->
            ApplicationManager.getApplication().invokeLater { listener(user) }
        }
    }

    override fun dispose() {
        lifetimeDef.terminate()
    }

    companion object {
        // Метод для получения сервиса: UlearnAuthService.getInstance(project)
        fun getInstance(project: Project): UlearnAuthService = project.service()
    }
}