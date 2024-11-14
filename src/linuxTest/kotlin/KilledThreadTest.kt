import kotlinx.cinterop.*
import platform.posix.pthread_create
import platform.posix.pthread_tVar
import kotlin.test.Test

@OptIn(ExperimentalForeignApi::class)
class KilledThreadTest {
    
    @Test
    fun threadKillTest() {
        memScoped { 
            val pthread = alloc<pthread_tVar>()
            pthread_create(pthread.ptr, null, staticCFunction(::threadFun) , null)
            
            println("Done")
        }
        
    }
    
    
}

@OptIn(ExperimentalForeignApi::class)
private fun threadFun(arg: COpaquePointer?): COpaquePointer? {
    println(" Joe")
    return null
}
