import kotlinx.cinterop.*
import platform.darwin.UInt32
import platform.darwin.UInt64Var

@OptIn(ExperimentalForeignApi::class)
actual class ParkingDelegator {
    actual fun createFutexPtr(): Long {
        val signal = nativeHeap.alloc<UInt64Var>()
        signal.value = 0u
        return signal.ptr.toLong()
    }

    actual fun wait(futexPrt: Long, notifyWake: (Int) -> Unit) {
        val cPointer = futexPrt.toCPointer<UInt64Var>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
        val result = platform.darwin.ulock.__ulock_wait(UL_COMPARE_AND_WAIT, cPointer, 0u, 0u)
        notifyWake(result)
        nativeHeap.free(cPointer)
    }

    actual fun wake(futexPrt: Long): Int {
        return platform.darwin.ulock.__ulock_wake(UL_COMPARE_AND_WAIT, futexPrt.toCPointer<UInt64Var>(), 0u)
    }
}
const val UL_COMPARE_AND_WAIT: UInt32 = 1u
