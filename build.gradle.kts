import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform") version "2.0.21"
    id("org.jetbrains.kotlinx.atomicfu") version "0.26.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

kotlin {
    
    mingwX64() {
        binaries.all {
            linkerOpts += "-lSynchronization"
        }
    }
    
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
        
        
    }
    
    
    
    
}

repositories {
    mavenCentral()
}
