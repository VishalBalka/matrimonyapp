// Root build file.
//
// Kotlin is NOT applied via org.jetbrains.kotlin.android / kotlin-android.
// AGP 9 built-in Kotlin compiles the Kotlin sources. AGP bundles its own KGP
// version, so the two classpath entries below raise the Kotlin toolchain to the
// version pinned in gradle/libs.versions.toml. Version catalog accessors are not
// available inside a buildscript block, so the version is literal here and MUST
// be kept in sync with `kotlin` in libs.versions.toml.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.4.20")

    }
}

plugins {
    alias(libs.plugins.android.application) apply false
}
