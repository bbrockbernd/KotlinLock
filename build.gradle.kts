plugins {
    kotlin("multiplatform") version "2.0.21"
    id("org.jetbrains.kotlinx.atomicfu") version "0.26.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

kotlin {
    jvm()
    // Tier 1
    macosX64()
    macosArm64()
    iosSimulatorArm64()
    iosX64()

    // Tier 2
    linuxX64()
    linuxArm64()
    watchosSimulatorArm64()
    watchosX64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosX64()
    tvosArm64()
    iosArm64()

    // Tier 3
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    mingwX64()
    watchosDeviceArm64()
    
    applyDefaultHierarchyTemplate()

    @Suppress("DEPRECATION") //https://github.com/Kotlin/kotlinx-atomicfu/issues/207
    linuxArm32Hfp()
    
    sourceSets {
        commonMain.dependencies {
            implementation(kotlin("stdlib-common"))
//            implementation("org.jetbrains.compose.runtime:runtime:1.7.0")
        }
        
        commonTest.dependencies { 
            implementation(kotlin("test"))
        }
        
        jvmMain.dependencies {
            implementation(kotlin("stdlib-jdk8"))
        }
        
        jvmTest.dependencies {
            implementation("org.jetbrains.kotlinx:lincheck:2.34")
        }

        val androidNative64Main by creating { dependsOn(nativeMain.get()) }
        androidNative64Main.also {
            androidNativeArm64Main.get().dependsOn(it)
            androidNativeX64Main.get().dependsOn(it)
        }

        val androidNative32Main by creating { dependsOn(nativeMain.get()) }
        androidNative32Main.let {
            androidNativeArm32Main.get().dependsOn(it)
            androidNativeX86Main.get().dependsOn(it)
        }
        
        val linux64Main by creating { dependsOn(nativeMain.get()) }
        linux64Main.let {
            linuxX64Main.get().dependsOn(it)
            linuxArm64Main.get().dependsOn(it)
        }
        
        val linux32Main by creating { dependsOn(nativeMain.get()) }
        linux32Main.let {
            linuxArm32HfpMain.get().dependsOn(it)
        }
        
        val apple64Main by creating { dependsOn(nativeMain.get()) }
        apple64Main.let {
            watchosDeviceArm64Main.get().dependsOn(it)
            iosArm64Main.get().dependsOn(it)
            tvosArm64Main.get().dependsOn(it)
            tvosX64Main.get().dependsOn(it)
            tvosSimulatorArm64Main.get().dependsOn(it)
            watchosX64Main.get().dependsOn(it)
            watchosSimulatorArm64Main.get().dependsOn(it)
            iosX64Main.get().dependsOn(it)
            iosSimulatorArm64Main.get().dependsOn(it)
            macosX64Main.get().dependsOn(it)
            macosArm64Main.get().dependsOn(it)
        }
        
        val apple32Main by creating { dependsOn(nativeMain.get()) }
        apple32Main.let {
            watchosArm32Main.get().dependsOn(it)
            watchosArm64Main.get().dependsOn(it) // Uses Int for timespec
        }
    }
}

repositories {
    google()
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}
