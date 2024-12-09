import kotlinx.cinterop.*
import platform.posix.*
import platform.windows.INFINITE
import platform.windows.UINT64Var
import platform.windows.WaitOnAddress
import platform.windows.WakeByAddressSingle

internal actual object NativeParkingDelegator: ParkingDelegator {
    private val delegator = FutexDelegator
    actual override fun createFutexPtr(): Long = delegator.createFutexPtr()
    actual override fun wait(futexPrt: Long): Boolean = delegator.wait(futexPrt)
    actual override fun wake(futexPrt: Long): Int = delegator.wake(futexPrt)
    actual override fun manualDeallocate(futexPrt: Long) = delegator.manualDeallocate(futexPrt)
}

@OptIn(ExperimentalForeignApi::class)
internal object FutexDelegator: ParkingDelegator {
    override fun createFutexPtr(): Long {
        val signal = nativeHeap.alloc<UINT64Var>()
        signal.value = 0u
        return signal.ptr.toLong()
    }

    // From https://learn.microsoft.com/en-us/windows/win32/api/synchapi/nf-synchapi-waitonaddress
    override fun wait(futexPrt: Long): Boolean {
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

    override fun wake(futexPrt: Long): Int {
        WakeByAddressSingle(futexPrt.toCPointer<UINT64Var>())
        // windows doesn't return a success or fail status.
        return 0
    }

    override fun manualDeallocate(futexPrt: Long) {
        val cPtr = futexPrt.toCPointer<UINT64Var>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
        nativeHeap.free(cPtr)
    }
}


@OptIn(ExperimentalForeignApi::class)
internal object PosixDelegator : ParkingDelegator {
    override fun createFutexPtr(): Long {
        val combo = nativeHeap.alloc<posix_combo_t>()
        pthread_mutex_init(combo.mutex.toCPointer(), null)
        pthread_cond_init(combo.cond.toCPointer(), null)
        combo.wake = 0uL
        return combo.ptr.toLong()
    }

    override fun wait(futexPrt: Long): Boolean {
        val comboPtr = futexPrt.toCPointer<posix_combo_t>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
        val combo = comboPtr.pointed

        pthread_mutex_lock(combo.mutex.toCPointer())
        while (combo.wake == 0uL) {
            pthread_cond_wait(combo.cond.toCPointer(), combo.mutex.toCPointer())
        }
        pthread_mutex_unlock(combo.mutex.toCPointer())

        pthread_mutex_destroy(combo.mutex.toCPointer())
        pthread_cond_destroy(combo.cond.toCPointer())
        nativeHeap.free(comboPtr)
        return false
    }

    override fun wake(futexPrt: Long): Int {
        val comboPtr = futexPrt.toCPointer<posix_combo_t>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
        val combo = comboPtr.pointed
        pthread_mutex_lock(combo.mutex.toCPointer())
        combo.wake = 1uL
        pthread_cond_signal(combo.cond.toCPointer())
        pthread_mutex_unlock(combo.mutex.toCPointer())
        return 0
    }

    override fun manualDeallocate(futexPrt: Long) {
        val comboPtr = futexPrt.toCPointer<posix_combo_t>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
        val combo = comboPtr.pointed
        pthread_mutex_destroy(combo.mutex.toCPointer())
        pthread_cond_destroy(combo.cond.toCPointer())
        nativeHeap.free(comboPtr)
    }
}
