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

    init {
        // Объявляем RPC метод: принимает LoginRequest, возвращает LoginResponse
        call("login", LoginRequest, LoginResponse)
    }
}