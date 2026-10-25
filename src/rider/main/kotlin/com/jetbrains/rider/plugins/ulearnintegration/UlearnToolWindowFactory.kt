package com.jetbrains.rider.plugins.ulearnintegration

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.panel
import javax.swing.JButton
import javax.swing.JLabel

class UlearnToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Создаем компоненты
        val loginField = JBTextField()
        val passwordField = JBPasswordField()
        val loginButton = JButton("Войти")
        val statusLabel = JLabel("") // Результат от бэкенда

        // Строим интерфейс
        val mainPanel = panel {
            row("Логин:") {
                cell(loginField)
            }
            row("Пароль:") {
                cell(passwordField)
            }
            row {
                cell(loginButton)
            }
            row {
                cell(statusLabel)
            }
        }

        loginButton.addActionListener {
            val login = loginField.text
            val password = String(passwordField.password)

            statusLabel.text = "Авторизация..."

            val authService = UlearnAuthService.getInstance(project)

            authService.login(login, password) { response ->
                if (response.isSuccess) {
                    statusLabel.text = "OK: ${response.message}"
                } else {
                    statusLabel.text = "Ошибка: ${response.message}"
                }
            }
        }

        // Регистрируем панель в окне Rider
        val content = ContentFactory.getInstance().createContent(mainPanel, "Login", false)
        toolWindow.contentManager.addContent(content)
    }
}