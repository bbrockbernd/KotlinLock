plugins {
    kotlin("multiplatform") version "2.0.21"
    id("org.jetbrains.kotlinx.atomicfu") version "0.26.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

kotlin {
    jvm()
    linuxX64()
    macosArm64()
    iosArm64() {
        compilations.getByName("main") {
            cinterops.creating { 
                definitionFile.set(project.file("src/nativeInterop/cinterop/ulock.def"))

            }
        }
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
    
    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }
        
        commonTest {
            dependencies{ 
                implementation(kotlin("test"))
            }
        }
        
        jvmMain {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        
        macosMain {
            
        }
        
        macosTest {
            
        }
        
        appleMain {
            
        }
        
        
//        linuxX64Main {
//            
//        }
//        linuxX64Test {
//            dependencies {
//                implementation(kotlin("test"))
//            }
//        }
    }
    
    
    
}

repositories {
    mavenCentral()
}

//dependencies {
//    testImplementation(kotlin("test"))
//}
//
//tasks.test {
//    useJUnitPlatform()
//}