import kotlinx.cinterop.*
import platform.linux.SYS_futex
import platform.posix.NULL
import platform.posix.syscall

const val FUTEX_WAIT = 0
const val FUTEX_WAKE = 1

@OptIn(ExperimentalForeignApi::class)
actual class ThreadParker {
    var ptr: UIntVar? = null
    
    actual fun park() {
        ptr = nativeHeap.alloc<UIntVar>()
        ptr!!.value = 0u
        syscall(SYS_futex.toLong(), ptr.rawPtr.toLong(), FUTEX_WAIT, 0u, NULL)
        nativeHeap.free(ptr.rawPtr)
        ptr = null
    }
    
    actual fun unpark() {
        syscall(SYS_futex.toLong(), ptr.rawPtr.toLong(), FUTEX_WAKE, 1u, NULL)
    }
}