package com.jetbrains.rider.plugins.ulearnintegration

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
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
import javax.swing.event.DocumentEvent

class UlearnToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val loginField = ExtendableTextField()
        val passwordField = JBPasswordField()
        val loginButton = JButton("Войти")
        val statusLabel = JLabel("")

        val spinnerExtension = ExtendableTextComponent.Extension { AnimatedIcon.Default.INSTANCE }

        val clearOutlineOnType = object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                loginField.putClientProperty("JComponent.outline", null)
                passwordField.putClientProperty("JComponent.outline", null)
                loginField.repaint()
                passwordField.repaint()
            }
        }

        loginField.document.addDocumentListener(clearOutlineOnType)
        passwordField.document.addDocumentListener(clearOutlineOnType)

        val mainPanel = panel {
            group("Авторизация", indent = false) {
                row("Логин:") {
                    cell(loginField)
                        .columns(COLUMNS_SHORT)
                        .focused()
                }
                row("Пароль:") {
                    cell(passwordField)
                        .columns(COLUMNS_SHORT)
                }.bottomGap(BottomGap.SMALL)

                // row("") занимает левую колонку пустотой
                row("") {
                    cell(loginButton)
                    cell(statusLabel)
                }
            }
        }.apply {
            border = JBUI.Borders.empty(2, 8)
        }

        fun showErrorNotification(message: String) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Ulearn Notifications")
                .createNotification(
                    "Ошибка авторизации Ulearn",
                    message,
                    NotificationType.ERROR
                )
                .notify(project)
        }

        fun performLogin() {
            val login = loginField.text.trim()
            val password = String(passwordField.password)

            // Валидация
            var hasError = false
            if (login.isEmpty()) {
                loginField.putClientProperty("JComponent.outline", "error")
                loginField.repaint()
                hasError = true
            }
            if (password.isEmpty()) {
                passwordField.putClientProperty("JComponent.outline", "error")
                passwordField.repaint()
                hasError = true
            }

            if (hasError) {
//                statusLabel.icon = AllIcons.General.Error
                statusLabel.foreground = JBColor.RED
//                statusLabel.text = "Заполните все поля"
                return
            }
            // Конец валидации

            loginButton.isEnabled = false
            statusLabel.text = ""
            statusLabel.icon = null

            loginField.addExtension(spinnerExtension)

            val authService = UlearnAuthService.getInstance(project)
            authService.login(login, password) { response ->
                loginButton.isEnabled = true

                loginField.removeExtension(spinnerExtension)

                if (response.isSuccess) {
                    statusLabel.icon = AllIcons.General.InspectionsOK
                    statusLabel.foreground = JBColor.GREEN
//                    statusLabel.text = "Вы авторизованы"
                } else {
                    statusLabel.icon = AllIcons.General.Error
                    statusLabel.foreground = JBColor.RED
                    statusLabel.text = "Ошибка входа"
                    showErrorNotification(response.message)
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

        val content = ContentFactory.getInstance().createContent(mainPanel, "Настройки", false)
        toolWindow.contentManager.addContent(content)
    }
}