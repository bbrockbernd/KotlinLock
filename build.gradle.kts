plugins {
    kotlin("multiplatform") version "2.0.21"
    id("org.jetbrains.kotlinx.atomicfu") version "0.26.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

kotlin {
    jvm()
    val linuxTargets = listOf(
        // Tier 2,
        linuxX64(),
        linuxArm64(),
    )
    
    val androidNativeTargets = listOf(
        // Tier 3
        androidNativeArm32(),
        androidNativeArm64(),
        androidNativeX86(),
        androidNativeX64(),
    )

    // apple targets
    val appleTargets = listOf (
        // Tier 1
        macosX64(),
        macosArm64(),
        iosSimulatorArm64(),
        iosX64(),

        // Tier 2,
        watchosSimulatorArm64(),
        watchosX64(),
        watchosArm32(),
        watchosArm64(),
        tvosSimulatorArm64(),
        tvosX64(),
        tvosArm64(),
        iosArm64(),

        // Tier 3
        watchosDeviceArm64(),
    )
    
    applyDefaultHierarchyTemplate()
    
    mingwX64 {
        binaries.all {
            linkerOpts += "-lSynchronization"
        }
        compilations.getByName("main").cinterops {
            val ulock by creating {
                defFile(project.file("stub.def"))
                packageName = "stub"
            }
            val posixcombo by creating {
                defFile(project.file("src/nativeInterop/cinterop/posixcombo.def"))
                packageName = "platform.posix"
            }
        }
    }
    
    linuxTargets.forEach {
        it.compilations.getByName("main").cinterops {
            val ulock by creating {
                defFile(project.file("stub.def"))
                packageName = "stub"
            }
            val posixcombo by creating {
                defFile(project.file("src/nativeInterop/cinterop/posixcombo.def"))
                packageName = "platform.posix"
            }
        }
    }

    androidNativeTargets.forEach {
        it.compilations.getByName("main").cinterops {
            val ulock by creating {
                defFile(project.file("stub.def"))
                packageName = "stub"
            }
            val posixcombo by creating {
                defFile(project.file("src/nativeInterop/cinterop/posixcombo.def"))
                packageName = "platform.posix"
            }
        }
    }
    
    appleTargets.forEach {
        it.compilations.getByName("main").cinterops {
            val ulock by creating {
                defFile(project.file("src/nativeInterop/cinterop/ulock.def"))
                packageName = "platform.darwin.ulock"
                includeDirs("${project.rootDir}/src/nativeInterop/cinterop")
            }
            val posixcombo by creating {
                defFile(project.file("src/nativeInterop/cinterop/posixcombo.def"))
                packageName = "platform.posix"
            }
        }
    }

    @Suppress("DEPRECATION") //https://github.com/Kotlin/kotlinx-atomicfu/issues/207
    linuxArm32Hfp {
        compilations.getByName("main").cinterops {
            // This is a hack to fix commonization bug: KT-73136
            val ulock by creating {
                defFile(project.file("stub.def"))
                packageName = "stub"
            }
            val posixcombo by creating {
                defFile(project.file("src/nativeInterop/cinterop/posixcombo.def"))
                packageName = "platform.posix"
            }
        }
    }
    
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
        
        
        val linux64Main by creating { dependsOn(nativeMain.get()) }
        linuxX64Main.get().dependsOn(linux64Main)
        linuxArm64Main.get().dependsOn(linux64Main)
        
        val linux32Main by creating { dependsOn(nativeMain.get()) }
        linuxArm32HfpMain.get().dependsOn(linux32Main)
        
    }
}

repositories {
    google()
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}
