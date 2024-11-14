import kotlinx.cinterop.*
import platform.posix.*
import kotlin.test.Test

@OptIn(ExperimentalForeignApi::class)
class KilledThreadTest {
    
    @Test
    fun threadKillTest() {
        memScoped { 
            val pthread = alloc<pthread_tVar>()
            pthread_create(pthread.ptr, null, staticCFunction(::threadFun) , null)
            pthread_kill(pthread.value, 9)
            
            pthread_join(pthread.value, null)
            println("Done")
        }
        
    }
    
    
}

@OptIn(ExperimentalForeignApi::class)
private fun threadFun(arg: COpaquePointer?): COpaquePointer? {
    sleep(5u)
    println(" Joe")
    return null
}
