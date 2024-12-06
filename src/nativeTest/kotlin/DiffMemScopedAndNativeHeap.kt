import kotlinx.cinterop.*
import platform.posix.uint64_t
import platform.posix.uint64_tVar
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.time.measureTime

@Ignore
class DiffMemScopedAndNativeHeap {
    
    @Test
    fun diffMemScopedNativeHeap() {
       repeat(3) {
           val timeMem = measureTime { 
               memScopedTest()
           }
           println("MemScoped: $timeMem")
           
           val timeNH = measureTime {
               nativeHeapTest()
           }
           println("NativeHeap: $timeNH")
       }
    }
    
    @OptIn(ExperimentalForeignApi::class)
    fun memScopedTest() {
        repeat(300000) {
            memScoped {
                val bla = alloc<uint64_tVar>()
            }
        }
    }
    
    @OptIn(ExperimentalForeignApi::class)
    fun nativeHeapTest() {
        repeat(300000) {
            val bla = nativeHeap.alloc<uint64_tVar>()
            nativeHeap.free(bla)
        }
    }
}