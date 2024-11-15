import kotlinx.cinterop.*
import platform.darwin.UInt32
import platform.darwin.UInt64Var

@OptIn(ExperimentalForeignApi::class)
actual object ParkingUtils {
    actual fun createFutexPtr(): Long {
        val signal = nativeHeap.alloc<UInt64Var>()
        signal.value = 0u
        return signal.ptr.toLong()
    }

    actual fun wait(futexPrt: Long, notifyWake: (interrupted: Boolean) -> Unit) {
        val cPointer = futexPrt.toCPointer<UInt64Var>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
        // THere is very little information about ulock so not sure what returned int stands for an interrupt
        // In any case it should be 0
        val result = platform.darwin.ulock.__ulock_wait(UL_COMPARE_AND_WAIT, cPointer, 0u, 0u)
        notifyWake(result != 0)
        nativeHeap.free(cPointer)
    }

    actual fun wake(futexPrt: Long): Int {
        return platform.darwin.ulock.__ulock_wake(UL_COMPARE_AND_WAIT, futexPrt.toCPointer<UInt64Var>(), 0u)
    }

    actual fun manualDeallocate(futexPrt: Long) {
        val cPointer = futexPrt.toCPointer<UInt64Var>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
        nativeHeap.free(cPointer)
    }
}
const val UL_COMPARE_AND_WAIT: UInt32 = 1u
