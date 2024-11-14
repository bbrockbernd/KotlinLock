import kotlinx.cinterop.*
import platform.posix.*
import kotlin.test.Test

@OptIn(ExperimentalForeignApi::class)
class KilledThreadTest {
    
    @Test
    fun threadKillTest() {
        memScoped { 
            val mutex = NativeMutex()
            val pthread = alloc<pthread_tVar>()
            val cRef = StableRef.create(mutex).asCPointer()
            pthread_create(pthread.ptr, null, staticCFunction(::threadFun), cRef)
//            pthread_kill(pthread.value, 9)
            
            pthread_join(pthread.value, null)
            println("Done")
        }
        
    }
    
    
}

@OptIn(ExperimentalForeignApi::class)
private fun threadFun(arg: COpaquePointer?): COpaquePointer? {
    val mutex = arg!!.asStableRef<NativeMutex>().get()
    mutex.lock()
    sleep(5u)
    mutex.unlock()
    println(" Joe")
    return null
}
