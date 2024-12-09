import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.value
import platform.posix.pthread_create
import platform.posix.pthread_join
import platform.posix.pthread_kill
import platform.posix.pthread_tVar
import platform.posix.sleep
import kotlin.test.Ignore
import kotlin.test.Test

@Ignore
@OptIn(ExperimentalForeignApi::class)
class QosTest {
    
    
    @Test
    fun qosTest() {
        memScoped {
            val mutex = NativeMutex { FutexParkingDelegator }
            val pthread = alloc<pthread_tVar>()
            val cRef = StableRef.create(mutex).asCPointer()

            mutex.lock()
            println("[MAIN] Entered mutex")
            pthread_create(pthread.ptr, null, staticCFunction(::threadFun), cRef)
            sleep(2u)
            println("[MAIN] Killing SUB")

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
