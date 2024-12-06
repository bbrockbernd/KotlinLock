plugins {
    kotlin("multiplatform") version "2.0.21"
    id("org.jetbrains.kotlinx.atomicfu") version "0.26.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

kotlin {
    jvm()
    val linuxTargets = listOf(
        linuxX64 {
            compilations.getByName("main").cinterops {
                val ulock by creating {
                    defFile(project.file("stub.def"))
                    packageName = "stub"
                }
                val posixcombo by creating {
                    defFile(project.file("src/nativeInterop/cinterop/posixcombo.def"))
                    packageName = "platform.posix"
//                    includeDirs("${project.rootDir}/src/nativeInterop/cinterop")
                }
            }
        }
    )

    // apple targets
    val appleTargets = listOf (
        iosArm64(),
        iosX64(),
        macosX64(),
        macosArm64()
    )
    
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
//                includeDirs("${project.rootDir}/src/nativeInterop/cinterop")
            }
        }
    }

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
//                includeDirs("${project.rootDir}/src/nativeInterop/cinterop")
            }
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(kotlin("stdlib-common"))
            implementation("org.jetbrains.compose.runtime:runtime:1.7.0")
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
    }
}

repositories {
    google()
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}
