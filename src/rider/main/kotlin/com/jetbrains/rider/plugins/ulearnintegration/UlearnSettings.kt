package com.jetbrains.rider.plugins.ulearnintegration

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "UlearnSettings", storages = [Storage("ulearn-settings.xml")])
class UlearnSettings : PersistentStateComponent<UlearnSettings.State> {

    class State {
        var enabledCourseIds: MutableList<String> = mutableListOf()
    }

    private var myState = State()

    override fun getState(): State = myState
    override fun loadState(state: State) {
        myState = state
    }
}