package com.jetbrains.rider.plugins.ulearnintegration.ui

import javax.swing.JComponent

interface UlearnTab {
    val title: String
    fun createComponent(): JComponent
}