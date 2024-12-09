import kotlinx.cinterop.*
import platform.posix.posix_combo_t
import platform.posix.pthread_mutex_init
import platform.posix.pthread_mutex_t
import platform.posix.pthread_mutex_tVar
import kotlin.test.Test

@OptIn(ExperimentalForeignApi::class)
class SpecialWindowsTest {
    
    @Test
    fun windowsTest() {
        // Here how they do it in atomic fu
        println("Test 1 started")
        val bla = nativeHeap.alloc<pthread_mutex_tVar>()
        println("ptr created")
        pthread_mutex_init(bla.ptr, null)
        println("mut initialized")
        
        // Here how we do it
        println("Test 3 started")
        val bla2 = nativeHeap.alloc<posix_combo_t>()
        println("ptr 3 created")
        val mutLongVar = LongVarOf<pthread_mutex_t>(bla2.mutex.objcPtr())
        pthread_mutex_init(mutLongVar.ptr, null)
        println("mut 3 initialized")
        
        
        
    }
}