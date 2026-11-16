package com.jetbrains.rider.plugins.ulearnintegration.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.*
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.rider.plugins.ulearnintegration.UlearnCatalogService
import com.jetbrains.rider.plugins.ulearnintegration.domain.Course
import com.jetbrains.rider.plugins.ulearnintegration.domain.CourseUnit
import com.jetbrains.rider.plugins.ulearnintegration.domain.Exercise
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/** Выбор курса и практики. Не знает ни про проект, ни про то, что будет после выбора. */
class ExercisePicker {

    private val catalogService = service<UlearnCatalogService>()

    private val courseBox = ComboBox<Course>()
    private val treeModel = DefaultTreeModel(DefaultMutableTreeNode())
    private val tree = Tree(treeModel)
    private val statusLabel = JBLabel("").apply { foreground = JBColor.GRAY }
    private var requestId = 0

    /** Выбранная практика или null. */
    var selectedExercise: Exercise? = null
        private set

    var selectedUnit: CourseUnit? = null
        private set

    /** Вызывается при каждой смене выбора (в UI-потоке). */
    var onSelectionChanged: (Exercise?) -> Unit = {}

    /** Подсказка, какая практика уже привязана: для галочки и автовыбора. */
    var isBound: (Exercise) -> Boolean = { false }
    var preferredCourseId: String? = null

    val component: JComponent by lazy { build() }

    fun reload() = loadCourses()

    fun setStatus(text: String) {
        statusLabel.text = text
    }

    private fun build(): JComponent {
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
                        if (isBound(item)) icon = com.intellij.icons.AllIcons.General.InspectionsOK
                        append(item.title)
                        append("  ${item.maxScore}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    }
                }
            }
        }

        courseBox.addActionListener { (courseBox.selectedItem as? Course)?.let { loadUnits(it) } }
        tree.addTreeSelectionListener {
            val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode
            selectedExercise = node?.userObject as? Exercise
            selectedUnit = (node?.parent as? DefaultMutableTreeNode)?.userObject as? CourseUnit
            onSelectionChanged(selectedExercise)
        }
        loadCourses()

        return JPanel(BorderLayout(0, JBUI.scale(6))).apply {
            add(courseBox, BorderLayout.NORTH)
            add(JBScrollPane(tree), BorderLayout.CENTER)
            add(statusLabel, BorderLayout.SOUTH)
        }
    }

    private fun loadCourses() {
        statusLabel.text = "Загрузка курсов…"
        catalogService.loadAsync({ catalogService.visibleCourses() }) { result ->
            result.onSuccess { courses ->
                val initial = courses.firstOrNull { it.id == preferredCourseId } ?: courses.firstOrNull()
                courseBox.model = CollectionComboBoxModel(courses, initial)
                statusLabel.text = if (courses.isEmpty()) "Нет включённых курсов" else "Выберите курс"
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
                val count = units.sumOf { it.exercises.size }
                statusLabel.text = if (count == 0) "«${course.title}»: нет практик с проектом"
                else "«${course.title}»: практик $count, тем ${units.size}"
                selectBound()
            }.onFailure { statusLabel.text = "Ошибка загрузки: ${it.message}" }
        }
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

    private fun selectBound() {
        val root = treeModel.root as DefaultMutableTreeNode
        val node = TreeUtil.findNode(root) {
            (it.userObject as? Exercise)?.let(isBound) == true
        } ?: return
        TreeUtil.selectNode(tree, node)
    }

    fun repaintTree() = tree.repaint()
}