import kotlinx.cinterop.*
import platform.linux.SYS_futex
import platform.posix.*

const val FUTEX_WAIT = 0
const val FUTEX_WAKE = 1

internal actual object NativeParkingDelegator: ParkingDelegator {
    private val delegator = FutexDelegator
    actual override fun createFutexPtr(): Long = delegator.createFutexPtr()
    actual override fun wait(futexPrt: Long): Boolean = delegator.wait(futexPrt)
    actual override fun wake(futexPrt: Long): Int = delegator.wake(futexPrt)
    actual override fun manualDeallocate(futexPrt: Long) = delegator.manualDeallocate(futexPrt)
}

@OptIn(ExperimentalForeignApi::class)
internal object PosixDelegator : ParkingDelegator {
    override fun createFutexPtr(): Long {
        val combo = nativeHeap.alloc<posix_combo_t>()
        pthread_mutex_init(combo.mutex.ptr, null)
        pthread_cond_init(combo.cond.ptr, null)
        combo.wake = 0uL
        return combo.ptr.toLong()
    }

    override fun wait(futexPrt: Long): Boolean {
        val comboPtr = futexPrt.toCPointer<posix_combo_t>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
        val combo = comboPtr.pointed

        pthread_mutex_lock(combo.mutex.ptr)
        while (combo.wake == 0uL) {
            pthread_cond_wait(combo.cond.ptr, combo.mutex.ptr)
        }
        pthread_mutex_unlock(combo.mutex.ptr)

        pthread_mutex_destroy(combo.mutex.ptr)
        pthread_cond_destroy(combo.cond.ptr)
        nativeHeap.free(comboPtr)
        return false
    }

    override fun wake(futexPrt: Long): Int {
        val comboPtr = futexPrt.toCPointer<posix_combo_t>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
        val combo = comboPtr.pointed
        pthread_mutex_lock(combo.mutex.ptr)
        combo.wake = 1uL
        pthread_cond_signal(combo.cond.ptr)
        pthread_mutex_unlock(combo.mutex.ptr)
        return 0
    }

    override fun manualDeallocate(futexPrt: Long) {
        val comboPtr = futexPrt.toCPointer<posix_combo_t>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
        val combo = comboPtr.pointed
        pthread_mutex_destroy(combo.mutex.ptr)
        pthread_cond_destroy(combo.cond.ptr)
        nativeHeap.free(comboPtr)
    }

}

@OptIn(ExperimentalForeignApi::class)
internal object FutexDelegator: ParkingDelegator {
    override fun createFutexPtr(): Long {
        val signal = nativeHeap.alloc<UIntVar>()
        signal.value = 0u
        return signal.ptr.toLong()
    }

    override fun wait(futexPrt: Long): Boolean {
        val cPtr = futexPrt.toCPointer<UIntVar>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
        val result = syscall(SYS_futex.toLong(), futexPrt, FUTEX_WAIT, 0u, NULL)
        val interrupted = result.toInt() == EINTR
        nativeHeap.free(cPtr)
        return interrupted
    }

    override fun wake(futexPrt: Long): Int {
        //Returns n threads woken up (needs to be 1)
        val result = syscall(SYS_futex.toLong(), futexPrt, FUTEX_WAKE, 1u, NULL).toInt()
        return if (result == 1) 0 else -1
    }

    override fun manualDeallocate(futexPrt: Long) {
        val cPtr = futexPrt.toCPointer<UIntVar>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
        nativeHeap.free(cPtr)
    }
}
