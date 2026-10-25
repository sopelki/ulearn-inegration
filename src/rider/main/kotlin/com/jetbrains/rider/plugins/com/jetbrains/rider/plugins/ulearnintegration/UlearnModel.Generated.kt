@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package com.jetbrains.rd.ide.model

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.impl.*

import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.*
import com.jetbrains.rd.util.*
import kotlin.time.Duration
import kotlin.reflect.KClass
import kotlin.jvm.JvmStatic



/**
 * #### Generated from [UlearnIntegrationModel.main.kt:8]
 */
class UlearnModel private constructor(
    private val _login: RdCall<LoginRequest, LoginResponse>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            val classLoader = javaClass.classLoader
            serializers.register(LazyCompanionMarshaller(RdId(-1456312260941647207), classLoader, "com.jetbrains.rd.ide.model.LoginRequest"))
            serializers.register(LazyCompanionMarshaller(RdId(-8252191941719026313), classLoader, "com.jetbrains.rd.ide.model.LoginResponse"))
        }
        
        
        
        
        
        const val serializationHash = -5132448264445198965L
        
    }
    override val serializersOwner: ISerializersOwner get() = UlearnModel
    override val serializationHash: Long get() = UlearnModel.serializationHash
    
    //fields
    val login: IRdCall<LoginRequest, LoginResponse> get() = _login
    //methods
    //initializer
    init {
        bindableChildren.add("login" to _login)
    }
    
    //secondary constructor
    internal constructor(
    ) : this(
        RdCall<LoginRequest, LoginResponse>(LoginRequest, LoginResponse)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("UlearnModel (")
        printer.indent {
            print("login = "); _login.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): UlearnModel   {
        return UlearnModel(
            _login.deepClonePolymorphic()
        )
    }
    //contexts
    //threading
    override val extThreading: ExtThreadingKind get() = ExtThreadingKind.Default
}
val Solution.ulearnModel get() = getOrCreateExtension("ulearnModel", ::UlearnModel)



/**
 * #### Generated from [UlearnIntegrationModel.main.kt:11]
 */
data class LoginRequest (
    val login: String,
    val password: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<LoginRequest> {
        override val _type: KClass<LoginRequest> = LoginRequest::class
        override val id: RdId get() = RdId(-1456312260941647207)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): LoginRequest  {
            val login = buffer.readString()
            val password = buffer.readString()
            return LoginRequest(login, password)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: LoginRequest)  {
            buffer.writeString(value.login)
            buffer.writeString(value.password)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as LoginRequest
        
        if (login != other.login) return false
        if (password != other.password) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + login.hashCode()
        __r = __r*31 + password.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("LoginRequest (")
        printer.indent {
            print("login = "); login.print(printer); println()
            print("password = "); password.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [UlearnIntegrationModel.main.kt:17]
 */
data class LoginResponse (
    val isSuccess: Boolean,
    val message: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<LoginResponse> {
        override val _type: KClass<LoginResponse> = LoginResponse::class
        override val id: RdId get() = RdId(-8252191941719026313)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): LoginResponse  {
            val isSuccess = buffer.readBool()
            val message = buffer.readString()
            return LoginResponse(isSuccess, message)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: LoginResponse)  {
            buffer.writeBool(value.isSuccess)
            buffer.writeString(value.message)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as LoginResponse
        
        if (isSuccess != other.isSuccess) return false
        if (message != other.message) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + isSuccess.hashCode()
        __r = __r*31 + message.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("LoginResponse (")
        printer.indent {
            print("isSuccess = "); isSuccess.print(printer); println()
            print("message = "); message.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}
