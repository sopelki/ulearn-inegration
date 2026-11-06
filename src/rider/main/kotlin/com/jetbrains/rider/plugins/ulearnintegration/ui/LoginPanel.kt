package com.jetbrains.rider.plugins.ulearnintegration.ui

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBUI
import com.jetbrains.rider.plugins.ulearnintegration.UlearnAuthService
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.event.DocumentEvent

class LoginPanel(private val project: Project) {

    private val loginField = ExtendableTextField()
    private val passwordField = JBPasswordField()
    private val loginButton = JButton("Войти")
    private val statusLabel = JLabel("")

    private val spinnerExtension = ExtendableTextComponent.Extension { AnimatedIcon.Default.INSTANCE }

    val component: JComponent = panel {
        group("Авторизация", indent = false) {
            row("Логин:") {
                cell(loginField).columns(COLUMNS_SHORT).focused()
            }.topGap(TopGap.SMALL)

            row("Пароль:") {
                cell(passwordField).columns(COLUMNS_SHORT)
            }.bottomGap(BottomGap.SMALL)

            row("") {
                cell(loginButton)
                cell(statusLabel)
            }
        }
    }.apply {
        border = JBUI.Borders.empty(2, 8, 2, 0)
    }

    init {
        val clearOutlineOnType = object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                clearOutline()
            }
        }
        loginField.document.addDocumentListener(clearOutlineOnType)
        passwordField.document.addDocumentListener(clearOutlineOnType)

        loginButton.addActionListener { performLogin() }

        loginField.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) performLogin()
            }
        })

        passwordField.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) performLogin()
            }
        })
    }

    /** Вызывается при выходе из аккаунта: возвращает форму в исходное состояние. */
    fun reset() {
        passwordField.text = ""
        loginButton.isEnabled = true
        loginField.removeExtension(spinnerExtension)
        statusLabel.icon = null
        statusLabel.text = ""
        clearOutline()
    }

    private fun clearOutline() {
        loginField.putClientProperty("JComponent.outline", null)
        passwordField.putClientProperty("JComponent.outline", null)
        loginField.repaint()
        passwordField.repaint()
    }

    private fun performLogin() {
        val login = loginField.text.trim()
        val password = String(passwordField.password)

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
        if (hasError) return

        loginButton.isEnabled = false
        statusLabel.icon = null
        statusLabel.text = ""
        loginField.addExtension(spinnerExtension)

        UlearnAuthService.getInstance(project).login(login, password) { response ->
            loginButton.isEnabled = true
            loginField.removeExtension(spinnerExtension)

            // При успехе ничего не делаем: AccountTab сам переключится на профиль, когда придёт обновление currentUser.
            if (!response.isSuccess) {
                statusLabel.icon = AllIcons.General.Error
                statusLabel.foreground = JBColor.RED
                statusLabel.text = "Ошибка входа"
                showErrorNotification(response.message)
            }
        }
    }

    private fun showErrorNotification(message: String) {
        NotificationGroupManager.getInstance().getNotificationGroup("Ulearn Notifications")
            .createNotification("Ошибка авторизации Ulearn", message, NotificationType.ERROR).notify(project)
    }
}