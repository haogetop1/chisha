buildscript {
    repositories {
        google()
        maven("https://maven.aliyun.com/repository/public")
        mavenCentral()
    }
    dependencies {
        // AGP 9 内置 Kotlin，这里把 KGP 顶到 2.4.21
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.12")
    }
}

plugins {
    id("com.android.application") version "9.5.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
