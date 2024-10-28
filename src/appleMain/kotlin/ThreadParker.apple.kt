import kotlinx.cinterop.*
import platform.darwin.UInt32
import platform.darwin.UInt64
import platform.darwin.UInt64Var
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.internal.GCUnsafeCall



@OptIn(ExperimentalForeignApi::class)
actual class ThreadParker {
    var signal: UInt64Var? = null

    actual fun park() {
        signal = nativeHeap.alloc<UInt64Var>()
        signal!!.value = 0u
//        __ulock_wait(UL_COMPARE_AND_WAIT, signal!!.ptr, 0u, 0u)
        nativeHeap.free(signal.rawPtr)
        signal = null
    }

    actual fun unpark() {
//        __ulock_wake(UL_COMPARE_AND_WAIT, signal!!.ptr, 0u)
    }

}

//const val UL_COMPARE_AND_WAIT: UInt32 = 1u
//@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
//external fun __ulock_wait(operation: UInt32, addr: CPointer<UInt64Var>, value: UInt64, timeout_ns: UInt32): UInt32
//
////@OptIn(ExperimentalForeignApi::class)
////external fun __ulock_wait2(operation: UInt32, addr: CPointer<UInt64Var>, value: UInt64, timeout_ns: UInt32, value2: UInt64)
//
//@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
//external fun __ulock_wake(operation: UInt32, addr: CPointer<UInt64Var>, wake_value: UInt64): UInt32
