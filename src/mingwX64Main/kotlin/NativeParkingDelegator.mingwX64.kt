import kotlinx.cinterop.*
import platform.windows.INFINITE
import platform.windows.UINT64Var
import platform.windows.WaitOnAddress
import platform.windows.WakeByAddressSingle

@OptIn(ExperimentalForeignApi::class)
internal actual object NativeParkingDelegator: ParkingDelegator {
    override actual fun createFutexPtr(): Long {
        val signal = nativeHeap.alloc<UINT64Var>()
        signal.value = 0u
        return signal.ptr.toLong()
    }

    // From https://learn.microsoft.com/en-us/windows/win32/api/synchapi/nf-synchapi-waitonaddress
    override actual fun wait(futexPrt: Long): Boolean {
        val cPtr = futexPrt.toCPointer<UINT64Var>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
        
        val undesiredValue = nativeHeap.alloc<UINT64Var>()
        undesiredValue.value = 0u
        var capturedValue = cPtr.pointed.value.toInt()
        var result = 0
        while (capturedValue == 0) {
            result = WaitOnAddress(cPtr, undesiredValue.ptr, 8u, INFINITE)
            capturedValue = cPtr.pointed.value.toInt()
        }
        
        nativeHeap.free(undesiredValue)
        nativeHeap.free(cPtr)
        // TODO check thread interrupts on windows
        return false
    }

    override actual fun wake(futexPrt: Long): Int {
        WakeByAddressSingle(futexPrt.toCPointer<UINT64Var>())
        // windows doesn't return a success or fail status.
        return 0
    }

    override actual fun manualDeallocate(futexPrt: Long) {
        val cPtr = futexPrt.toCPointer<UINT64Var>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
        nativeHeap.free(cPtr)
    }
}