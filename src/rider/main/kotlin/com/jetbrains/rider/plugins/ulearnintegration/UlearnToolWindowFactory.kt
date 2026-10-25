package com.jetbrains.rider.plugins.ulearnintegration

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.COLUMNS_SHORT
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
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
            group("Авторизация", indent = false) {
                row("Логин:") {
                    cell(loginField)
                        .columns(COLUMNS_SHORT)
//                        .align(AlignX.FILL)
                        .focused()
                }
                row("Пароль:") {
                    cell(passwordField)
                        .columns(COLUMNS_SHORT)
//                        .align(AlignX.FILL)
                }.bottomGap(BottomGap.SMALL)

                row {
                    cell(loginButton)
                }.bottomGap(BottomGap.SMALL)

                row {
                    cell(statusLabel)
                }
            }
        }.apply {
            border = JBUI.Borders.empty(2, 8)
        }

        fun performLogin() {
            val login = loginField.text.trim()
            val password = String(passwordField.password)

            if (login.isEmpty() || password.isEmpty()) {
                statusLabel.foreground = JBColor.RED
                statusLabel.text = "Заполните все поля"
                return
            }

            loginButton.isEnabled = false
            statusLabel.foreground = JBColor.GRAY
            statusLabel.text = "Авторизация..."

            val authService = UlearnAuthService.getInstance(project)
            authService.login(login, password) { response ->
                loginButton.isEnabled = true

                if (response.isSuccess) {
                    statusLabel.foreground = JBColor.GREEN
                    statusLabel.text = response.message
                } else {
                    statusLabel.foreground = JBColor.RED
                    statusLabel.text = response.message
                }
            }
        }

        loginButton.addActionListener { performLogin() }

        passwordField.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    performLogin()
                }
            }
        })

        // Регистрируем панель в окне Rider
        val content = ContentFactory.getInstance().createContent(mainPanel, "Настройки", false)
        toolWindow.contentManager.addContent(content)
    }
}