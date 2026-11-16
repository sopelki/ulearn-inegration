package com.jetbrains.rider.plugins.ulearnintegration.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.HelpTooltip
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.jetbrains.rider.plugins.ulearnintegration.UlearnCatalogService
import com.jetbrains.rider.plugins.ulearnintegration.domain.Exercise
import com.jetbrains.rider.plugins.ulearnintegration.domain.ProjectBinding
import com.jetbrains.rider.plugins.ulearnintegration.infrastructure.UlearnProjectFile
import java.awt.BorderLayout
import java.nio.file.Path
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

class CoursesTab(private val project: Project) : UlearnTab {
    override val title = "Практики"

    private val catalogService = service<UlearnCatalogService>()
    private val picker = ExercisePicker()

    // Что записано в .ulearn открытого проекта (null = привязки нет)
    private var binding: ProjectBinding? = null

    private val hintLabel =
        JBLabel("Ручная привязка проекта", AllIcons.General.Information, SwingConstants.LEFT).apply {
            foreground = JBColor.GRAY
            border = JBUI.Borders.empty(8, 4, 4, 0)

        }

    private val selectedTitleLabel = JBLabel("").apply { font = JBFont.label().asBold() }
    private val selectedInfoLabel = JBLabel("").apply { foreground = JBColor.GRAY }
    private val selectButton = JButton("Выбрать").apply { isEnabled = false }

    private val detailsPanel = JPanel(BorderLayout(0, JBUI.scale(4))).apply {
        border = JBUI.Borders.emptyTop(4)
        add(selectedTitleLabel, BorderLayout.NORTH)
        add(selectedInfoLabel, BorderLayout.CENTER)
        add(selectButton, BorderLayout.SOUTH)
    }

    override fun createComponent(): JComponent {
        HelpTooltip().setTitle("Ручная привязка проекта").setDescription(
                """
        <span style="font-family: 'JetBrains Mono', monospace; font-size: 11px;">
            Выберите практику, которой соответствует открытый сейчас проект.
            Выбор запишется в файл <b>${UlearnProjectFile.FILE_NAME}</b> в его корне.
            <p>Чтобы скачать проект задания, используйте <i>Get from Version Control</i>.</p>
        </span>
        """.trimIndent()
            ).installOn(hintLabel)


        // Как picker узнаёт, какая практика уже привязана
        picker.isBound = { e -> binding?.let { it.courseId == e.courseId && it.slideId == e.id } == true }
        picker.onSelectionChanged = { updateSelection(it) }
        selectButton.addActionListener { picker.selectedExercise?.let { bindProject(it) } }
        updateSelection(null)

        // Сначала читаем .ulearn, потом запускаем загрузку курсов
        val basePath = project.basePath
        catalogService.loadAsync({ basePath?.let { UlearnProjectFile.read(Path.of(it)) } }) { result ->
            binding = result.getOrNull()
            picker.preferredCourseId = binding?.courseId
            picker.reload()
        }

        return JPanel(BorderLayout(0, JBUI.scale(6))).apply {
            border = JBUI.Borders.empty(2, 8)
            add(hintLabel, BorderLayout.NORTH)
            add(picker.component, BorderLayout.CENTER)
            add(detailsPanel, BorderLayout.SOUTH)
        }
    }

    private fun updateSelection(exercise: Exercise?) {
        if (exercise == null) {
            selectedTitleLabel.text = "Практика не выбрана"
            selectedInfoLabel.text = "Выберите практику в списке"
            selectButton.isEnabled = false
            return
        }

        val isBound = picker.isBound(exercise)
        val unitTitle = picker.selectedUnit?.title ?: ""
        selectedTitleLabel.text = exercise.title
        selectedInfoLabel.text =
            "$unitTitle · ${exercise.maxScore} баллов" + if (isBound) " · привязан к проекту" else ""
        selectButton.isEnabled = true
    }

    /** Записывает выбор в .ulearn в корне открытого проекта. */
    private fun bindProject(exercise: Exercise) {
        val basePath = project.basePath
        if (basePath == null) {
            picker.setStatus("Не удалось определить папку проекта")
            return
        }

        selectButton.isEnabled = false
        catalogService.loadAsync({ UlearnProjectFile.write(Path.of(basePath), exercise) }) { result ->
            result.onSuccess {
                binding = ProjectBinding(exercise.courseId, exercise.id)
                picker.repaintTree()   // галочка переезжает на новую практику
                picker.setStatus("Проект привязан к ${exercise.title}")
            }.onFailure {
                picker.setStatus("Не удалось записать ${UlearnProjectFile.FILE_NAME}: ${it.message}")
            }
            updateSelection(picker.selectedExercise)   // вернуть кнопке состояние по текущему выбору
        }
    }
}