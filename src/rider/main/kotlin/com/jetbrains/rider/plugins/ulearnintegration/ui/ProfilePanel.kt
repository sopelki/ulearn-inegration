package com.jetbrains.rider.plugins.ulearnintegration.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.ide.model.UserInfo
import com.jetbrains.rider.plugins.ulearnintegration.UlearnAuthService
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import java.net.URI
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel

class ProfilePanel(private val project: Project) {
    private val avatarLabel = JLabel(AllIcons.General.User)
    private val nameLabel = JBLabel().apply { font = JBFont.label().asBold() }
    private val logoutButton = JButton("Выйти")

    val component: JComponent = panel {
        group(indent = false) {
            row {
                cell(avatarLabel)
                cell(nameLabel)
                cell(logoutButton)
//                    .customize(UnscaledGaps(12))
            }
                .bottomGap(BottomGap.SMALL)
                .topGap(TopGap.SMALL)
        }
    }.apply {
        border = JBUI.Borders.empty(2, 16, 2, 0)
    }

    init {
        logoutButton.addActionListener { UlearnAuthService.getInstance(project).logout() }
    }

    fun show(user: UserInfo) {
        nameLabel.text = user.visibleName
        avatarLabel.icon = AllIcons.General.User // заглушка, пока грузится
        user.avatarUrl?.let { loadAvatar(it) }
    }

    private fun loadAvatar(url: String) {
        val app = ApplicationManager.getApplication()
        app.executeOnPooledThread {          // сеть не в UI-потоке
            val image = runCatching { ImageIO.read(URI(url).toURL()) }.getOrNull()
                ?: return@executeOnPooledThread
            val icon = ImageIcon(toCircle(image, 40))
            app.invokeLater { avatarLabel.icon = icon }
        }
    }

    private fun toCircle(src: BufferedImage, size: Int): BufferedImage {
        val out = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = out.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.clip = Ellipse2D.Float(0f, 0f, size.toFloat(), size.toFloat())
        g.drawImage(src, 0, 0, size, size, null)
        g.dispose()
        return out
    }
}