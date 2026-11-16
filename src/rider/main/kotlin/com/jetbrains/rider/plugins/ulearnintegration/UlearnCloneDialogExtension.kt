package com.jetbrains.rider.plugins.ulearnintegration

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogComponentStateListener
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.jetbrains.rider.plugins.ulearnintegration.domain.Exercise
import com.jetbrains.rider.plugins.ulearnintegration.infrastructure.ProjectDownloader
import com.jetbrains.rider.plugins.ulearnintegration.ui.ExercisePicker
import java.awt.BorderLayout
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JComponent
import javax.swing.JPanel

class UlearnCloneComponent : VcsCloneDialogExtensionComponent() {

    private val catalogService = service<UlearnCatalogService>()
    private val picker = ExercisePicker()

    private val dirField = TextFieldWithBrowseButton().apply {
        text = Path.of(System.getProperty("user.home"), "UlearnProjects").toString()
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

    private var stateListener: VcsCloneDialogComponentStateListener? = null

    init {
        picker.reload()   // автозагрузки в picker нет, запускаем сами
    }

    override fun getView(): JComponent = view

    override fun onComponentSelected() {
        // Состояние кнопки Clone
    }

    override fun doClone(checkoutListener: CheckoutProvider.Listener) {
        val exercise = picker.selectedExercise
        if (exercise == null) {
            picker.setStatus("Выберите практику")
            return
        }
        val target = Path.of(dirField.text.trim(), folderName(exercise))

        ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Скачивание проекта Ulearn", true) {
            private var error: Throwable? = null

            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = exercise.title
                error = runCatching { downloadProject(exercise, target) }.exceptionOrNull()
            }

            // onSuccess и onThrowable вызываются в UI-потоке
            override fun onSuccess() {
                val e = error
                if (e == null) {
                    checkoutListener.directoryCheckedOut(target.toFile(), null)
                    checkoutListener.checkoutCompleted()
                } else {
                    showError(e.message ?: "Неизвестная ошибка")
                }
            }
        })
    }

    override fun doValidateAll(): List<ValidationInfo> {
        TODO("Not yet implemented")
    }

    private fun downloadProject(exercise: Exercise, target: Path) {
        if (Files.exists(target) && Files.list(target).use { it.findAny().isPresent }) {
            throw IOException("Папка $target уже существует и не пуста")
        }
        val url = catalogService.catalog.projectUrl(exercise)
            ?: throw IOException("У этого задания нет проекта для скачивания")
        ProjectDownloader.download(exercise, url, target)
    }

    /** Имя папки из названия практики: без символов, запрещённых в путях Windows. */
    private fun folderName(exercise: Exercise) =
        exercise.title.replace(Regex("[\\\\/:*?\"<>|«»]"), "").trim().ifEmpty { exercise.id }

    private fun showError(message: String) {
        NotificationGroupManager.getInstance().getNotificationGroup("Ulearn Notifications")
            .createNotification("Не удалось скачать проект", message, NotificationType.ERROR).notify(null)
    }
}