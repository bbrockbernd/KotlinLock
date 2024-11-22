import kotlinx.cinterop.*
import platform.linux.SYS_futex
import platform.posix.EINTR
import platform.posix.NULL
import platform.posix.syscall

const val FUTEX_WAIT = 0
const val FUTEX_WAKE = 1

//TODO test interrupts
@OptIn(ExperimentalForeignApi::class)
internal actual object NativeParkingDelegator: ParkingDelegator {
    actual override fun createFutexPtr(): Long {
        val signal = nativeHeap.alloc<UIntVar>()
        signal.value = 0u
        return signal.ptr.toLong()
    }

    actual override fun wait(futexPrt: Long): Boolean {
        val cPtr = futexPrt.toCPointer<UIntVar>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
        val result = syscall(SYS_futex.toLong(), futexPrt, FUTEX_WAIT, 0u, NULL)
        val interrupted = result.toInt() == EINTR
        nativeHeap.free(cPtr)
        return false
    }

    actual override fun wake(futexPrt: Long): Int {
        return syscall(SYS_futex.toLong(), futexPrt, FUTEX_WAKE, 1u, NULL).toInt()
    }

    actual override fun manualDeallocate(futexPrt: Long) {
        val cPtr = futexPrt.toCPointer<UIntVar>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
        nativeHeap.free(cPtr)
    }
}
