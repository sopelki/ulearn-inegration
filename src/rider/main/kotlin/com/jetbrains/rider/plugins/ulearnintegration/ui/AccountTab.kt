package com.jetbrains.rider.plugins.ulearnintegration.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.jetbrains.rider.plugins.ulearnintegration.UlearnAuthService
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.JComponent
import javax.swing.JPanel

class AccountTab(private val project: Project, private val parent: Disposable) : UlearnTab {
    override val title = "Аккаунт"

    override fun createComponent(): JComponent {
        val loginPanel = LoginPanel(project)
        val profilePanel = ProfilePanel(project)

        val cards = CardLayout()
        val cardsPanel = JPanel(cards).apply {
            add(loginPanel.component, "login")
            add(profilePanel.component, "profile")
        }

        UlearnAuthService.getInstance(project).onUserChanged(parent) { user ->
            if (user == null) {
                loginPanel.reset()
                cards.show(cardsPanel, "login")
            } else {
                profilePanel.show(user)
                cards.show(cardsPanel, "profile")
            }
        }

        return JPanel(BorderLayout()).apply { add(cardsPanel, BorderLayout.NORTH) }
    }
}