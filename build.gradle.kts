plugins {
    kotlin("multiplatform") version "2.0.21"
}

group = "org.example"
version = "1.0-SNAPSHOT"

kotlin {
    linuxX64()

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
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(kotlin("stdlib-common"))
            implementation("org.jetbrains.compose.runtime:runtime:1.7.0")
        }
    }
}

repositories {
    google()
    mavenCentral()
}
