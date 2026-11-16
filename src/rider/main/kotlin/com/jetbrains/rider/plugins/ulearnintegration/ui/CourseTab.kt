package com.jetbrains.rider.plugins.ulearnintegration.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.CollectionListModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.jetbrains.rider.plugins.ulearnintegration.UlearnCatalogService
import com.jetbrains.rider.plugins.ulearnintegration.domain.Course
import com.jetbrains.rider.plugins.ulearnintegration.domain.Exercise
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class CoursesTab : UlearnTab {
    override val title = "Практики"

    private val catalogService = service<UlearnCatalogService>()

    private val courseBox = ComboBox<Course>()
    private val exerciseModel = CollectionListModel<Exercise>()
    private val exerciseList = JBList(exerciseModel)

    // Номер последнего запроса: защита от устаревших ответов
    private var requestId = 0

    override fun createComponent(): JComponent {
        courseBox.renderer = SimpleListCellRenderer.create("") { it.title }
        exerciseList.cellRenderer = SimpleListCellRenderer.create("") { "${it.title} (${it.maxScore})" }

        courseBox.addActionListener {
            (courseBox.selectedItem as? Course)?.let { loadExercises(it) }
        }

        loadCourses()

        return JPanel(BorderLayout(0, JBUI.scale(6))).apply {
            border = JBUI.Borders.empty(2, 8)
            add(courseBox, BorderLayout.NORTH)
            add(JBScrollPane(exerciseList), BorderLayout.CENTER)
        }
    }

    private fun loadCourses() {
        exerciseList.emptyText.text = "Загрузка…"
        catalogService.loadAsync({ catalogService.catalog.courses() }) { result ->
            result
                .onSuccess { courses ->
                    courseBox.model = CollectionComboBoxModel(courses)
                    exerciseList.emptyText.text = "Выберите курс"
                    courses.firstOrNull()?.let { loadExercises(it) }
                }
                .onFailure { exerciseList.emptyText.text = "Не удалось загрузить курсы: ${it.message}" }
        }
    }

    private fun loadExercises(course: Course) {
        val myRequest = ++requestId
        exerciseModel.removeAll()
        exerciseList.emptyText.text = "Загрузка…"
        exerciseList.setPaintBusy(true)

        catalogService.loadAsync({ catalogService.catalog.exercises(course.id) }) { result ->
            if (myRequest != requestId) return@loadAsync   // пока грузили, выбрали другой курс

            exerciseList.setPaintBusy(false)
            result
                .onSuccess { exercises ->
                    exerciseModel.replaceAll(exercises)
                    exerciseList.emptyText.text = "В этом курсе нет практик с проектом"
                }
                .onFailure { exerciseList.emptyText.text = "Ошибка загрузки: ${it.message}" }
        }
    }
}