package com.jetbrains.rider.plugins.ulearnintegration

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.jetbrains.rider.plugins.ulearnintegration.ui.AccountTab
import com.jetbrains.rider.plugins.ulearnintegration.ui.CoursesTab
import com.jetbrains.rider.plugins.ulearnintegration.ui.UlearnTab
import com.jetbrains.rider.plugins.ulearnintegration.ui.PracticeTab

class UlearnToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val tabs: List<UlearnTab> = listOf(
            AccountTab(project, toolWindow.disposable),
            CoursesTab(project),
            PracticeTab(project, toolWindow.disposable)
        )
        val contentFactory = ContentFactory.getInstance()
        for (tab in tabs) {
            val content = contentFactory.createContent(tab.createComponent(), tab.title, false)
            toolWindow.contentManager.addContent(content)
        }
    }
}