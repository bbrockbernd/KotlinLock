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
        var capturedValue = cPtr.pointed.value
        var result = 0
        while (capturedValue == 0uL) {
            result = WaitOnAddress(cPtr, undesiredValue.ptr, 8u, INFINITE)
            capturedValue = cPtr.pointed.value
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
        println("[createFutexPtr] Creating ptr")
        val combo = nativeHeap.alloc<posix_combo_t>()
        println("[createFutexPtr] Init elements")
        pthread_mutex_init(combo.mutex.toCPointer(), null)
        pthread_cond_init(combo.cond.toCPointer(), null)
        println("[createFutexPtr] Set val")
        combo.wake = 0uL
        return combo.ptr.toLong()
    }

    override fun wait(futexPrt: Long): Boolean {
        println("[wait] deref ptr")
        val comboPtr = futexPrt.toCPointer<posix_combo_t>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
        val combo = comboPtr.pointed

        println("[wait] lock")
        pthread_mutex_lock(combo.mutex.toCPointer())
        while (combo.wake == 0uL) {
            println("[wait] cond wait")
            pthread_cond_wait(combo.cond.toCPointer(), combo.mutex.toCPointer())
        }
        println("[wait] unlock")
        pthread_mutex_unlock(combo.mutex.toCPointer())

        println("[wait] destroy")
        pthread_mutex_destroy(combo.mutex.toCPointer())
        pthread_cond_destroy(combo.cond.toCPointer())
        println("[wait] free")
        nativeHeap.free(comboPtr)
        return false
    }

    override fun wake(futexPrt: Long): Int {
        println("[wake] deref")
        val comboPtr = futexPrt.toCPointer<posix_combo_t>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
        val combo = comboPtr.pointed
        println("[wake] lock")
        pthread_mutex_lock(combo.mutex.toCPointer())
        combo.wake = 1uL
        println("[wake] signal")
        pthread_cond_signal(combo.cond.toCPointer())
        println("[wake] unlock")
        pthread_mutex_unlock(combo.mutex.toCPointer())
        return 0
    }

    override fun manualDeallocate(futexPrt: Long) {
        println("[manual] bla")
        val comboPtr = futexPrt.toCPointer<posix_combo_t>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
        val combo = comboPtr.pointed
        pthread_mutex_destroy(combo.mutex.toCPointer())
        pthread_cond_destroy(combo.cond.toCPointer())
        nativeHeap.free(comboPtr)
    }
}
