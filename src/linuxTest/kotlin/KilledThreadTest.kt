import kotlinx.cinterop.*
import platform.posix.*
import kotlin.test.Test

@OptIn(ExperimentalForeignApi::class)
class KilledThreadTest {
    
    @Test
    fun threadKillTest() {
        memScoped { 
            val mutex = NativeMutex { NativeParkingDelegator }
            val pthread = alloc<pthread_tVar>()
            val cRef = StableRef.create(mutex).asCPointer()
            
            mutex.lock()
            println("[MAIN] Entered mutex")
            pthread_create(pthread.ptr, null, staticCFunction(::threadFun), cRef)
            sleep(2u)
            println("[MAIN] Killing SUB")
            
            pthread_kill(pthread.value, 2)


            sleep(2u)
            println("[MAIN] Exiting mutex")
            mutex.unlock()

            
            
            
            pthread_join(pthread.value, null)
            println("Done")
        }
        
    }
    
    
}

@OptIn(ExperimentalForeignApi::class)
private fun threadFun(arg: COpaquePointer?): COpaquePointer? {
    val mutex = arg!!.asStableRef<NativeMutex>().get()
    mutex.lock()
    println("[SUB] Entered mutex")
    
    println("[SUB] Exiting mutex")
    mutex.unlock()
    return null
}
