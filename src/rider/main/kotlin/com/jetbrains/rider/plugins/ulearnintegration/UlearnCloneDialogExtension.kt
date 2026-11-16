package com.jetbrains.rider.plugins.ulearnintegration

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtension
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import com.intellij.ui.components.JBLabel
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel

class UlearnCloneDialogExtension : VcsCloneDialogExtension {

    override fun getName(): String = "Ulearn"

    override fun getIcon(): Icon = IconLoader.getIcon("/icons/ulearn_u.svg", UlearnCloneDialogExtension::class.java)

    override fun getTooltip(): String? = null

    @Deprecated("Implement createMainComponent(Project, ModalityState)", ReplaceWith("UlearnCloneComponent()"))
    override fun createMainComponent(project: Project): VcsCloneDialogExtensionComponent = UlearnCloneComponent()

    override fun createMainComponent(
        project: Project, modalityState: ModalityState
    ): VcsCloneDialogExtensionComponent = UlearnCloneComponent()
}

class UlearnCloneComponent : VcsCloneDialogExtensionComponent() {

    private val view = JPanel().apply { add(JBLabel("Заглушка")) }

    override fun getView(): JComponent = view

    override fun doClone(checkoutListener: CheckoutProvider.Listener) {
        TODO("Not yet implemented")
    }

    override fun doValidateAll(): List<ValidationInfo> {
        TODO("Not yet implemented")
    }

    override fun onComponentSelected() {
        TODO("Not yet implemented")
    }
}