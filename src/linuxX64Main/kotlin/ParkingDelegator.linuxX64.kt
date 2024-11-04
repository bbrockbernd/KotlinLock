import kotlinx.cinterop.*
import platform.linux.SYS_futex
import platform.posix.NULL
import platform.posix.syscall

const val FUTEX_WAIT = 0
const val FUTEX_WAKE = 1

//TODO deal with interrupts
@OptIn(ExperimentalForeignApi::class)
actual class ParkingDelegator {
    actual fun createFutexPtr(): Long {
        val signal = nativeHeap.alloc<UIntVar>()
        signal.value = 0u
        return signal.ptr.toLong()
    }

    actual fun wait(futexPrt: Long, notifyWake: (Int) -> Unit) {
        val cPtr = futexPrt.toCPointer<UIntVar>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
        val result = syscall(SYS_futex.toLong(), futexPrt, FUTEX_WAIT, 0u, NULL)
        notifyWake(result.toInt())
        nativeHeap.free(cPtr)
    }

    actual fun wake(futexPrt: Long): Int {
        return syscall(SYS_futex.toLong(), futexPrt, FUTEX_WAKE, 1u, NULL).toInt()
    }
}
