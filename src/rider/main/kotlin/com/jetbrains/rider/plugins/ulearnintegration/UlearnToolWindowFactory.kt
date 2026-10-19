package com.jetbrains.rider.plugins.ulearnintegration

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import java.awt.GridLayout

class UlearnToolWindowFactory : ToolWindowFactory {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        
        val panel = JPanel(GridLayout(5, 1, 5, 5))

        val loginLabel = JLabel("Логин:")
        val loginField = JBTextField()
        
        val passwordLabel = JLabel("Пароль:")
        val passwordField = JBPasswordField()
        
        val loginButton = JButton("Войти в Ulearn")

        panel.add(loginLabel)
        panel.add(loginField)
        panel.add(passwordLabel)
        panel.add(passwordField)
        panel.add(loginButton)

        loginButton.addActionListener {
            val login = loginField.text
            val password = String(passwordField.password)
            
            println("Попытка входа — Логин: $login, Пароль: [скрыт]")
        }

        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}