import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform") version "2.0.21"
    id("org.jetbrains.kotlinx.atomicfu") version "0.26.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

kotlin {
    
    jvm()
    val linuxTargets = listOf(linuxX64())
    
    // apple targets
    val appleTargets = listOf (
        macosArm64(),
        iosArm64(),
        iosX64(),
        macosX64(),
    )

//    androidNativeArm32()
//    androidNativeArm64()
//    androidNativeX86()
//    androidNativeX64()
    val windowsTargets = listOf(
        mingwX64() {
            binaries {
                staticLib {
                    linkerOpts += listOf("-lSynchronization")
                }
            }
        }
    )
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.compose.runtime:runtime:1.7.0")
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
    
    windowsTargets.forEach {
//        it.compilations.getByName("main").defaultSourceSet.dependsOn(sourceSets.nativeMain.get())
//        it.compilations.getByName("test").defaultSourceSet.dependsOn(sourceSets.nativeTest.get())
        
//        it.compilations.getByName("main").cinterops {
//            val synchapi by creating {
//                defFile(project.file("src/nativeInterop/cinterop/synchapi.def"))
//                packageName = "platform.windows.synchapi"
//            }
//        }
    }
    
    linuxTargets.forEach {
        it.compilations.getByName("main").defaultSourceSet.dependsOn(sourceSets.nativeMain.get())
        it.compilations.getByName("test").defaultSourceSet.dependsOn(sourceSets.nativeTest.get())
    }
    
}

repositories {
    google()
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}
