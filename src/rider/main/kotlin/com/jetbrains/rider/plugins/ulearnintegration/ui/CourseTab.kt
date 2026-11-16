package com.jetbrains.rider.plugins.ulearnintegration.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.*
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.rider.plugins.ulearnintegration.UlearnCatalogService
import com.jetbrains.rider.plugins.ulearnintegration.domain.Course
import com.jetbrains.rider.plugins.ulearnintegration.domain.CourseUnit
import com.jetbrains.rider.plugins.ulearnintegration.domain.Exercise
import com.jetbrains.rider.plugins.ulearnintegration.domain.ProjectBinding
import com.jetbrains.rider.plugins.ulearnintegration.infrastructure.UlearnProjectFile
import java.awt.BorderLayout
import java.nio.file.Path
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class CoursesTab(private val project: Project) : UlearnTab {
    override val title = "Практики"

    private val catalogService = service<UlearnCatalogService>()

    // Пояснение: зачем нужна вкладка
    private val hintLabel = JBLabel(
        "Ручная привязка проекта", AllIcons.General.Information, SwingConstants.LEFT
    ).apply {
        border = JBUI.Borders.empty(8, 4, 4, 0)
        foreground = JBColor.DARK_GRAY
        toolTipText =
            "<html><body style='width: 340px'>" + "Выберите практику, которой соответствует <b>открытый сейчас проект</b>. " + "Выбор запишется в файл <code>${UlearnProjectFile.FILE_NAME}</code> в его корне.<br><br>" + "Чтобы скачать проект задания, используйте Get from Version Control." + "</body></html>"
    }

    private val courseBox = ComboBox<Course>()

    private val treeModel = DefaultTreeModel(DefaultMutableTreeNode())
    private val tree = Tree(treeModel)

    private val statusLabel = JBLabel("").apply { foreground = JBColor.GRAY }

    private val selectedTitleLabel = JBLabel("").apply { font = JBFont.label().asBold() }
    private val selectedInfoLabel = JBLabel("").apply { foreground = JBColor.GRAY }
    private val selectButton = JButton("Выбрать").apply { isEnabled = false }

    private val detailsPanel = JPanel(BorderLayout(0, JBUI.scale(4))).apply {
        border = JBUI.Borders.emptyTop(4)
        add(selectedTitleLabel, BorderLayout.NORTH)
        add(selectedInfoLabel, BorderLayout.CENTER)
        add(selectButton, BorderLayout.SOUTH)
    }

    private var selectedExercise: Exercise? = null
    private var requestId = 0

    private var binding: ProjectBinding? = null

    override fun createComponent(): JComponent {
        courseBox.renderer = SimpleListCellRenderer.create("") { it.title }

        tree.isRootVisible = false
        tree.showsRootHandles = true
        tree.cellRenderer = object : ColoredTreeCellRenderer() {
            override fun customizeCellRenderer(
                tree: JTree,
                value: Any?,
                selected: Boolean,
                expanded: Boolean,
                leaf: Boolean,
                row: Int,
                hasFocus: Boolean
            ) {
                when (val item = (value as? DefaultMutableTreeNode)?.userObject) {
                    is CourseUnit -> append(item.title, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                    is Exercise -> {
                        if (item.courseId == binding?.courseId && item.id == binding?.slideId) {
                            icon = AllIcons.General.InspectionsOK
                        }
                        append(item.title)
                        append("  ${item.maxScore}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    }
                }
            }
        }

        courseBox.addActionListener {
            (courseBox.selectedItem as? Course)?.let { loadUnits(it) }
        }

        tree.addTreeSelectionListener { updateSelection() }
        selectButton.addActionListener { selectedExercise?.let { bindProject(it) } }
        updateSelection()

        val basePath = project.basePath
        catalogService.loadAsync({ basePath?.let { UlearnProjectFile.read(Path.of(it)) } }) { result ->
            binding = result.getOrNull()
            loadCourses()
        }

        val top = JPanel(BorderLayout(0, JBUI.scale(6))).apply {
            add(hintLabel, BorderLayout.NORTH)
            add(courseBox, BorderLayout.CENTER)
        }

        val bottom = JPanel(BorderLayout()).apply {
            add(detailsPanel, BorderLayout.CENTER)
            add(statusLabel, BorderLayout.SOUTH)
        }

        return JPanel(BorderLayout(0, JBUI.scale(6))).apply {
            border = JBUI.Borders.empty(2, 8)
            add(top, BorderLayout.NORTH)
            add(JBScrollPane(tree), BorderLayout.CENTER)
            add(bottom, BorderLayout.SOUTH)
        }
    }

    /** Записывает выбор в .ulearn в корне открытого проекта. */
    private fun bindProject(exercise: Exercise) {
        val basePath = project.basePath
        if (basePath == null) {
            statusLabel.text = "Не удалось определить папку проекта"
            return
        }

        selectButton.isEnabled = false
        catalogService.loadAsync({ UlearnProjectFile.write(Path.of(basePath), exercise) }) { result ->
            updateSelection()   // вернуть кнопке состояние по текущему выбору
            result.onSuccess {
                binding = ProjectBinding(exercise.courseId, exercise.id)
                tree.repaint()
                statusLabel.text = "Проект привязан к «${exercise.title}»"
            }.onFailure { statusLabel.text = "Не удалось записать ${UlearnProjectFile.FILE_NAME}: ${it.message}" }
        }
    }

    private fun updateSelection() {
        val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode
        val exercise = node?.userObject as? Exercise
        selectedExercise = exercise

        if (exercise == null) {
            selectedTitleLabel.text = "Практика не выбрана"
            selectedInfoLabel.text = "Выберите практику в списке"
            selectButton.isEnabled = false
            return
        }

        val unit = (node.parent as? DefaultMutableTreeNode)?.userObject as? CourseUnit
        selectedTitleLabel.text = exercise.title
        selectedInfoLabel.text = "${unit?.title ?: ""} · ${exercise.maxScore} баллов"
        selectButton.isEnabled = true
    }

    private fun loadCourses() {
        statusLabel.text = "Загрузка курсов…"
        catalogService.loadAsync({ catalogService.visibleCourses() }) { result ->
            result.onSuccess { courses ->
                val initial = courses.firstOrNull { it.id == binding?.courseId } ?: courses.firstOrNull()
                courseBox.model = CollectionComboBoxModel(courses, initial)
                statusLabel.text = "Выберите курс"
                initial?.let { loadUnits(it) }
            }.onFailure { statusLabel.text = "Не удалось загрузить курсы: ${it.message}" }
        }
    }

    private fun loadUnits(course: Course) {
        val myRequest = ++requestId
        showUnits(emptyList())
        statusLabel.text = "Загружаю «${course.title}»…"
        tree.setPaintBusy(true)

        catalogService.loadAsync({ catalogService.catalog.units(course.id) }) { result ->
            if (myRequest != requestId) return@loadAsync

            tree.setPaintBusy(false)
            result.onSuccess { units ->
                showUnits(units)
                selectBound(course)
                val count = units.sumOf { it.exercises.size }
                statusLabel.text = if (count == 0) {
                    "«${course.title}»: нет практик с проектом"
                } else {
                    "«${course.title}»: практик $count, тем ${units.size}"
                }
            }.onFailure { statusLabel.text = "Ошибка загрузки: ${it.message}" }
        }
    }

    private fun selectBound(course: Course) {
        val b = binding ?: return
        if (b.courseId != course.id) return
        val root = treeModel.root as DefaultMutableTreeNode
        val node = TreeUtil.findNode(root) { (it.userObject as? Exercise)?.id == b.slideId } ?: return
        TreeUtil.selectNode(tree, node)
    }

    private fun showUnits(units: List<CourseUnit>) {
        val root = DefaultMutableTreeNode()
        for (unit in units) {
            val unitNode = DefaultMutableTreeNode(unit)
            unit.exercises.forEach { unitNode.add(DefaultMutableTreeNode(it)) }
            root.add(unitNode)
        }
        treeModel.setRoot(root)
        TreeUtil.expandAll(tree)
    }
}