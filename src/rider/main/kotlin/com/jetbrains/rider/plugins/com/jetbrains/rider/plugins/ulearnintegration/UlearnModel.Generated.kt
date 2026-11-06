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
    private val _login: RdCall<LoginRequest, LoginResponse>,
    private val _logout: RdCall<Unit, Unit>,
    private val _currentUser: RdProperty<UserInfo?>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            val classLoader = javaClass.classLoader
            serializers.register(LazyCompanionMarshaller(RdId(-1456312260941647207), classLoader, "com.jetbrains.rd.ide.model.LoginRequest"))
            serializers.register(LazyCompanionMarshaller(RdId(-8252191941719026313), classLoader, "com.jetbrains.rd.ide.model.LoginResponse"))
            serializers.register(LazyCompanionMarshaller(RdId(18648563943500), classLoader, "com.jetbrains.rd.ide.model.UserInfo"))
        }
        
        
        
        
        private val __UserInfoNullableSerializer = UserInfo.nullable()
        
        const val serializationHash = -1871874883937804913L
        
    }
    override val serializersOwner: ISerializersOwner get() = UlearnModel
    override val serializationHash: Long get() = UlearnModel.serializationHash
    
    //fields
    val login: IRdCall<LoginRequest, LoginResponse> get() = _login
    val logout: IRdCall<Unit, Unit> get() = _logout
    val currentUser: IProperty<UserInfo?> get() = _currentUser
    //methods
    //initializer
    init {
        _currentUser.optimizeNested = true
    }
    
    init {
        bindableChildren.add("login" to _login)
        bindableChildren.add("logout" to _logout)
        bindableChildren.add("currentUser" to _currentUser)
    }
    
    //secondary constructor
    internal constructor(
    ) : this(
        RdCall<LoginRequest, LoginResponse>(LoginRequest, LoginResponse),
        RdCall<Unit, Unit>(FrameworkMarshallers.Void, FrameworkMarshallers.Void),
        RdProperty<UserInfo?>(null, __UserInfoNullableSerializer)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("UlearnModel (")
        printer.indent {
            print("login = "); _login.print(printer); println()
            print("logout = "); _logout.print(printer); println()
            print("currentUser = "); _currentUser.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): UlearnModel   {
        return UlearnModel(
            _login.deepClonePolymorphic(),
            _logout.deepClonePolymorphic(),
            _currentUser.deepClonePolymorphic()
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


/**
 * #### Generated from [UlearnIntegrationModel.main.kt:22]
 */
data class UserInfo (
    val id: String,
    val visibleName: String,
    val avatarUrl: String?
) : IPrintable {
    //companion
    
    companion object : IMarshaller<UserInfo> {
        override val _type: KClass<UserInfo> = UserInfo::class
        override val id: RdId get() = RdId(18648563943500)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): UserInfo  {
            val id = buffer.readString()
            val visibleName = buffer.readString()
            val avatarUrl = buffer.readNullable { buffer.readString() }
            return UserInfo(id, visibleName, avatarUrl)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: UserInfo)  {
            buffer.writeString(value.id)
            buffer.writeString(value.visibleName)
            buffer.writeNullable(value.avatarUrl) { buffer.writeString(it) }
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
        
        other as UserInfo
        
        if (id != other.id) return false
        if (visibleName != other.visibleName) return false
        if (avatarUrl != other.avatarUrl) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + id.hashCode()
        __r = __r*31 + visibleName.hashCode()
        __r = __r*31 + if (avatarUrl != null) avatarUrl.hashCode() else 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("UserInfo (")
        printer.indent {
            print("id = "); id.print(printer); println()
            print("visibleName = "); visibleName.print(printer); println()
            print("avatarUrl = "); avatarUrl.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}
