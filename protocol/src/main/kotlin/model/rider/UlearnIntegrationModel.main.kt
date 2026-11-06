package model.rider

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.PredefinedType.*
import com.jetbrains.rider.model.nova.ide.SolutionModel

@Suppress("unused")
object UlearnModel : Ext(SolutionModel.Solution) {

    // Входящие данные
    val LoginRequest = structdef {
        field("login", string)
        field("password", string)
    }

    // Ответ бэкенда
    val LoginResponse = structdef {
        field("isSuccess", bool)
        field("message", string)
    }

    val UserInfo = structdef {
        field("id", string)
        field("visibleName", string)
        field("avatarUrl", string.nullable)
    }

    init {
        call("login", LoginRequest, LoginResponse)
        call("logout", void, void)
        property("currentUser", UserInfo.nullable)
    }
}