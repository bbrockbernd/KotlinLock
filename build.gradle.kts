import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform") version "2.0.21"
    id("org.jetbrains.kotlinx.atomicfu") version "0.26.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

kotlin {
    
    jvm()
    linuxX64()
    
    // apple targets
    val appleTargets = listOf (
        macosArm64(),
        iosArm64(),
        iosX64(),
        macosX64(),
    )
    
    mingwX64()
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }
        
        val commonTest by getting {
            dependencies{ 
                implementation(kotlin("test"))
            }
        }
        
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        

        val nativeTest by creating { dependsOn(commonTest) }
        val nativeMain by creating { dependsOn(commonMain) }
        val linuxX64Main by getting { dependsOn(nativeMain) }
        val mingwX64Main by getting { dependsOn(nativeMain) }
        val mingwX64Test by getting { dependsOn(nativeTest) }
        
    }
    
    appleTargets.forEach {
        it.compilations.getByName("main").defaultSourceSet.dependsOn(sourceSets.nativeMain.get())
        it.compilations.getByName("test").defaultSourceSet.dependsOn(sourceSets.nativeTest.get())
        it.compilations.getByName("main").cinterops {
            val ulock by creating {
                defFile(project.file("src/nativeInterop/cinterop/ulock.def"))
                packageName = "platform.darwin.ulock"
                includeDirs("${project.rootDir}/src/nativeInterop/cinterop")
            }
        }
    }
    
}

repositories {
    mavenCentral()
}
