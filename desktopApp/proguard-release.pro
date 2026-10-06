-dontwarn com.hrm.latex.parser.tokenizer.LatexTokenizer$Companion

# JavaFX 的工具包和原生窗口栈只能反射加载（javafx.toolkit 属性默认指向
# com.sun.javafx.tk.quantum.QuantumToolkit，glass/prism 同理），静态分析不可达；
# 缺失时运行时报 "No toolkit found"。JavaFX 内部反射遍布，按整包保留。
-keep class javafx.** { *; }
-keep class com.sun.javafx.** { *; }
-keep class com.sun.glass.** { *; }
-keep class com.sun.prism.** { *; }
-keep class com.sun.scenario.** { *; }
-keep class com.sun.pisces.** { *; }
-keep class com.sun.media.** { *; }
-keep class com.sun.openjfx.** { *; }
-keep class netscape.javascript.** { *; }

-keep class * implements io.ktor.client.HttpClientEngineContainer {
    *;
}

-keep class * implements io.ktor.serialization.kotlinx.KotlinxSerializationExtensionProvider {
    *;
}

-keep class io.ktor.client.engine.cio.CIOEngineContainer {
    *;
}

-keep class io.ktor.client.engine.java.JavaHttpEngineContainer {
    *;
}

-keep class io.ktor.serialization.kotlinx.json.KotlinxSerializationJsonExtensionProvider {
    *;
}

# Coil 的网络 fetcher 仅通过 META-INF/services/coil3.util.FetcherServiceLoaderTarget
# 在运行期反射加载，shrink 阶段静态分析不可达会被整包删除；服务文件本身仍会保留，
# 运行时加载不到目标类 → 默认 ImageLoader 没有任何网络 fetcher → 所有网络图片静默失败。
-keep class * implements coil3.util.FetcherServiceLoaderTarget {
    *;
}

-keep class coil3.network.ktor3.** {
    *;
}

# JNA 通过反射解析接口方法名映射 native 函数（DWM 标题栏上色），
# 并在运行期解包 jnidispatch.dll，整包保留避免 shrink 破坏。
-keep class com.sun.jna.** {
    *;
}

-keep interface com.github.zly2006.zhihu.viewmodel.PaginationEnvironment {
    *;
}

-keep interface com.github.zly2006.zhihu.viewmodel.CollectionContentEnvironment {
    *;
}

-keep interface com.github.zly2006.zhihu.viewmodel.NotificationPaginationEnvironment {
    *;
}

-keep class * implements com.github.zly2006.zhihu.viewmodel.PaginationEnvironment {
    *;
}

-keep class com.github.zly2006.zhihu.viewmodel.filter.*_Impl {
    *;
}

-keep class com.github.zly2006.zhihu.viewmodel.local.*_Impl {
    *;
}

-keep class * extends androidx.room.RoomDatabase {
    *;
}

-keep @androidx.room.Database class * {
    *;
}

-keep @androidx.room.Dao class * {
    *;
}

-keep class androidx.sqlite.driver.bundled.** {
    *;
}

-keepclasseswithmembernames class * {
    native <methods>;
}
