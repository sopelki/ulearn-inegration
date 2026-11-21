package com.jetbrains.rider.plugins.ulearnintegration

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.rd.ide.model.PracticeRequest
import com.jetbrains.rd.ide.model.PracticeResponse
import com.jetbrains.rd.ide.model.ulearnModel
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rider.projectView.solution

@Service(Service.Level.PROJECT)
class UlearnPracticeService(private val project: Project) : Disposable {

    private val lifetimeDef = LifetimeDefinition()

    /**
     * Запрашивает у бэкенда практику, выбранную в этом solution (файл .ulearn).
     * refresh = true: игнорировать кэш бэкенда и перезапросить практику у Ulearn.
     */
    fun loadPractice(refresh: Boolean, onResult: (PracticeResponse) -> Unit) {
        val model = project.solution.ulearnModel
        val lifetime = lifetimeDef.lifetime

        model.getPractice.start(lifetime, PracticeRequest(refresh)).result.advise(lifetime) { rdResult ->
            val response = try {
                rdResult.unwrap()
            } catch (e: Throwable) {
                PracticeResponse(false, e.message ?: "Неизвестная ошибка", null)
            }

            // Возвращаем результат в UI-поток
            ApplicationManager.getApplication().invokeLater {
                onResult(response)
            }
        }
    }

    override fun dispose() {
        lifetimeDef.terminate()
    }

    companion object {
        fun getInstance(project: Project): UlearnPracticeService = project.service()
    }
}
