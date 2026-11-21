package com.jetbrains.rider.plugins.ulearnintegration.ui

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.ide.model.PracticeInfo
import com.jetbrains.rider.plugins.ulearnintegration.UlearnAuthService
import com.jetbrains.rider.plugins.ulearnintegration.UlearnPracticeService
import java.awt.BorderLayout
import java.awt.CardLayout
import java.net.URI
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.event.HyperlinkEvent
import javax.swing.text.html.HTMLDocument

/**
 * Вкладка с текстом практики, выбранной в текущем solution (файл .ulearn).
 *
 * parent: Disposable, с жизненным циклом которого связана подписка на смену пользователя
 * (как у AccountTab, сюда передаётся toolWindow.disposable).
 */
class PracticeTab(
    private val project: Project,
    parent: Disposable
) : UlearnTab {

    override val title: String = "Текст практики"

    override fun createComponent(): JComponent = component

    private val titleLabel = JBLabel().apply {
        font = JBFont.h3().asBold()
    }
    private val refreshButton = JButton("Обновить")

    private val messageLabel = JBLabel().apply { setAllowAutoWrapping(true) }
    private val retryButton = JButton("Повторить")

    private val messagePanel: JComponent = panel {
        row { cell(messageLabel).align(AlignX.FILL) }
        row { cell(retryButton) }
    }.apply {
        border = JBUI.Borders.empty(8)
    }

    // JEditorPane не выполняет скрипты из HTML, поэтому безопасен для текста с сервера.
    private val practicePane = JEditorPane().apply {
        editorKit = HTMLEditorKitBuilder().build()
        isEditable = false
        border = JBUI.Borders.empty(4, 8)
        addHyperlinkListener { e ->
            if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                e.url?.let { BrowserUtil.browse(it) }
            }
        }
    }

    private val cardLayout = CardLayout()
    private val cards = JPanel(cardLayout).apply {
        add(messagePanel, MESSAGE)
        add(JBScrollPane(practicePane).apply { border = JBUI.Borders.empty() }, CONTENT)
    }

    private val component: JComponent = JPanel(BorderLayout()).apply {
        add(JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(6, 8)
            add(titleLabel, BorderLayout.CENTER)
            add(refreshButton, BorderLayout.EAST)
        }, BorderLayout.NORTH)
        add(cards, BorderLayout.CENTER)
    }

    // Номер последнего запроса: ответы на устаревшие запросы игнорируем.
    private var requestId = 0

    init {
        refreshButton.addActionListener { load(refresh = true) }
        retryButton.addActionListener { load(refresh = true) }

        showMessage("Загрузка…")

        // Подписка срабатывает сразу с текущим значением, поэтому отдельный первый запрос не нужен.
        UlearnAuthService.getInstance(project).onUserChanged(parent) { user ->
            requestId++   // результаты запросов для прежнего пользователя больше не нужны
            if (user == null) {
                refreshButton.isEnabled = false
                showMessage("Войдите в аккаунт на вкладке «Аккаунт», чтобы увидеть текст практики.")
            } else {
                refreshButton.isEnabled = true
                load(refresh = false)
            }
        }
    }

    private fun load(refresh: Boolean) {
        val id = ++requestId
        refreshButton.isEnabled = false
        showMessage("Загрузка…")

        UlearnPracticeService.getInstance(project).loadPractice(refresh) { response ->
            if (id != requestId) return@loadPractice   // пришёл ответ на устаревший запрос

            refreshButton.isEnabled = true
            val practice = response.practice
            when {
                !response.isSuccess -> showMessage("Не удалось загрузить практику.\n${response.message}", canRetry = true)
                practice == null -> showMessage(response.message)   // в solution нет файла .ulearn
                else -> showPractice(practice)
            }
        }
    }

    private fun showMessage(text: String, canRetry: Boolean = false) {
        titleLabel.text = ""
        messageLabel.text = "<html>${StringUtil.escapeXmlEntities(text).replace("\n", "<br>")}</html>"
        retryButton.isVisible = canRetry
        cardLayout.show(cards, MESSAGE)
    }

    private fun showPractice(practice: PracticeInfo) {
        titleLabel.text = practice.title

        // Относительные ссылки и картинки из HTML Ulearn разрешаем относительно сайта.
        (practicePane.document as HTMLDocument).base = URI(BASE_URL).toURL()
        practicePane.text = buildHtml(practice)
        practicePane.caretPosition = 0   // иначе JEditorPane прокручивается в конец текста

        cardLayout.show(cards, CONTENT)
    }

    private fun buildHtml(practice: PracticeInfo): String = buildString {
        append("<html><body>")
        append(practice.html)
        if (practice.hints.isNotEmpty()) {
            append("<h3>Подсказки</h3><ol>")
            practice.hints.forEach { append("<li>").append(it).append("</li>") }
            append("</ol>")
        }
        append("</body></html>")
    }

    private companion object {
        const val MESSAGE = "message"
        const val CONTENT = "content"
        const val BASE_URL = "https://ulearn.me/"
    }
}
