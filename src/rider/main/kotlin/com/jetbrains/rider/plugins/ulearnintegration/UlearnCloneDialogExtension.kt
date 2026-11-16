package com.jetbrains.rider.plugins.ulearnintegration

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtension
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.jetbrains.rider.plugins.ulearnintegration.domain.Exercise
import com.jetbrains.rider.plugins.ulearnintegration.infrastructure.ProjectDownloader
import com.jetbrains.rider.plugins.ulearnintegration.ui.ExercisePicker
import java.awt.BorderLayout
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

class UlearnCloneDialogExtension : VcsCloneDialogExtension {

    override fun getName(): String = "Ulearn"

    override fun getIcon(): Icon = IconLoader.getIcon("/icons/ulearn_u.svg", UlearnCloneDialogExtension::class.java)

    override fun getTooltip(): String? = null

    override fun createMainComponent(
        project: Project, modalityState: ModalityState
    ): VcsCloneDialogExtensionComponent = UlearnCloneComponent()
}

class UlearnCloneComponent : VcsCloneDialogExtensionComponent() {

    private val catalogService = service<UlearnCatalogService>()
    private val picker = ExercisePicker()

    private val defaultBaseDir = Path.of(System.getProperty("user.home"), "UlearnProjects")
    private var suggestedName = ""

    private val dirField = TextFieldWithBrowseButton().apply {
        text = defaultBaseDir.toString()
        @Suppress("DialogTitleCapitalization") addBrowseFolderListener(
            null, FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle("Папка для проекта")
        )
    }

    private val view = JPanel(BorderLayout(0, JBUI.scale(8))).apply {
        border = JBUI.Borders.empty(8)
        add(picker.component, BorderLayout.CENTER)
        add(JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
            add(JBLabel("Папка:"), BorderLayout.WEST)
            add(dirField, BorderLayout.CENTER)
        }, BorderLayout.SOUTH)
    }

    init {
        picker.onSelectionChanged = { suggestPath(it); updateOkState() }
        dirField.textField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = updateOkState()
        })
        picker.reload()
    }

    /** Полный путь проекта из поля или null, если путь пустой или некорректный. */
    private fun targetPath(): Path? {
        val text = dirField.text.trim()
        if (text.isEmpty()) return null
        return runCatching { Path.of(text) }.getOrNull()
    }

    /** Подставляет в поле путь для выбранной практики. */
    private fun suggestPath(exercise: Exercise?) {
        if (exercise == null) return

        val current = targetPath() ?: defaultBaseDir
        val parent = if (suggestedName.isNotEmpty() && current.fileName?.toString()
                ?.startsWith(suggestedName) == true
        ) current.parent ?: defaultBaseDir
        else current

        suggestedName = folderName(exercise)
        dirField.text = parent.resolve(suggestedName).toString()
    }

    override fun getView(): JComponent = view

    override fun doClone(checkoutListener: CheckoutProvider.Listener) {
        val exercise = picker.selectedExercise ?: return
        val target = targetPath() ?: return

        ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Скачивание проекта Ulearn", true) {
            private var result: Result<Path>? = null

            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = exercise.title
                result = runCatching { downloadProject(exercise, target) }
            }

            // onSuccess вызывается в UI-потоке
            override fun onSuccess() {
                result?.onSuccess { openProject(it) }?.onFailure { showError(it.message ?: "Неизвестная ошибка") }
            }
        })
    }

    override fun doValidateAll(): List<ValidationInfo> {
        val errors = mutableListOf<ValidationInfo>()

        if (picker.selectedExercise == null) {
            errors += ValidationInfo("Выберите практику", picker.component)
        }

        val target = targetPath()
        if (target == null) {
            errors += ValidationInfo("Укажите корректный путь для проекта", dirField)
        } else if (Files.exists(target) && Files.list(target).use { it.findAny().isPresent }) {
            errors += ValidationInfo("Папка уже существует и не пуста: измените имя", dirField)
        }

        return errors
    }

    private fun updateOkState() {
        dialogStateListener.onOkActionEnabled(picker.selectedExercise != null && targetPath() != null)
    }

    override fun onComponentSelected() {
        updateOkState()
    }

    private fun downloadProject(exercise: Exercise, target: Path): Path {
        if (Files.exists(target) && Files.list(target).use { it.findAny().isPresent }) {
            throw IOException("Папка $target уже существует и не пуста")
        }
        val url = catalogService.catalog.projectUrl(exercise)
            ?: throw IOException("У этого задания нет проекта для скачивания")
        return ProjectDownloader.download(exercise, url, target)
    }

    private fun openProject(path: Path) {
        ProjectUtil.openOrImport(path, OpenProjectTask.build())
    }

    /** Имя папки из названия практики: без символов, запрещённых в путях Windows. */
    private fun folderName(exercise: Exercise) =
        exercise.title.replace(Regex("[\\\\/:*?\"<>|«»]"), "").trim().ifEmpty { exercise.id }

    private fun showError(message: String) {
        NotificationGroupManager.getInstance().getNotificationGroup("Ulearn Notifications")
            .createNotification("Не удалось скачать проект", message, NotificationType.ERROR).notify(null)
    }
}