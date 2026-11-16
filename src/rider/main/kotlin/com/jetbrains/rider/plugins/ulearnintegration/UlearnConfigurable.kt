package com.jetbrains.rider.plugins.ulearnintegration

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.ui.CheckBoxList
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.jetbrains.rider.plugins.ulearnintegration.domain.Course
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class UlearnConfigurable : Configurable {

    private val catalogService = service<UlearnCatalogService>()
    private val settings = service<UlearnSettings>()

    private val list = CheckBoxList<Course>()
    private val statusLabel = JBLabel("Загрузка курсов…")
    private var courses: List<Course> = emptyList()
    private var loaded = false   // пока курсы не загрузились, сохранять нечего

    override fun getDisplayName() = "Ulearn"

    override fun createComponent(): JComponent {
        catalogService.loadAsync({ catalogService.catalog.courses() }) { result ->
            result
                .onSuccess { loaded(it) }
                .onFailure { statusLabel.text = "Не удалось загрузить курсы: ${it.message}" }
        }
        return JPanel(BorderLayout(0, 6)).apply {
            add(
                JBLabel("Показывать во вкладке только отмеченные курсы"),
                BorderLayout.NORTH
            )
            add(JBScrollPane(list), BorderLayout.CENTER)
            add(statusLabel, BorderLayout.SOUTH)
        }
    }

    private fun loaded(all: List<Course>) {
        courses = all
        val enabled = settings.state.enabledCourseIds
        list.setItems(all) { it.title }
        all.forEach { list.setItemSelected(it, enabled.isEmpty() || it.id in enabled) }
        loaded = true
        statusLabel.text = ""
    }

    private fun selectedIds(): List<String> {
        val selected = courses.filter { list.isItemSelected(it) }.map { it.id }
        return if (selected.size == courses.size) emptyList() else selected
    }

    override fun isModified() = loaded && selectedIds() != settings.state.enabledCourseIds

    override fun apply() {
        if (!loaded) return
        settings.state.enabledCourseIds = selectedIds().toMutableList()
    }
}