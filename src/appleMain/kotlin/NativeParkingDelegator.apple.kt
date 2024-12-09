import kotlinx.cinterop.*
import platform.darwin.UInt32
import platform.darwin.UInt64Var
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
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
//        pthread_mutex_init(combo.mutex.ptr, null)
//        pthread_cond_init(combo.cond.ptr, null)
//        combo.wake = 0uL
        posixParkInit(combo.ptr)
        return combo.ptr.toLong()
    }

    override fun wait(futexPrt: Long): Boolean {
        val comboPtr = futexPrt.toCPointer<posix_combo_t>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
//        val combo = comboPtr.pointed 
//        
//        pthread_mutex_lock(combo.mutex.ptr)
//        while (combo.wake == 0uL) {
//            pthread_cond_wait(combo.cond.ptr, combo.mutex.ptr)
//        }
//        pthread_mutex_unlock(combo.mutex.ptr)
//        
//        pthread_mutex_destroy(combo.mutex.ptr)
//        pthread_cond_destroy(combo.cond.ptr)
        posixWait(comboPtr)
        nativeHeap.free(comboPtr)
        return false
    }

    override fun wake(futexPrt: Long): Int {
        val comboPtr = futexPrt.toCPointer<posix_combo_t>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
//        val combo = comboPtr.pointed
//        pthread_mutex_lock(combo.mutex.ptr)
//        combo.wake = 1uL
//        pthread_cond_signal(combo.cond.ptr)
//        pthread_mutex_unlock(combo.mutex.ptr)
        posixWake(comboPtr)
        return 0
    }

    override fun manualDeallocate(futexPrt: Long) {
        val comboPtr = futexPrt.toCPointer<posix_combo_t>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
//        val combo = comboPtr.pointed
//        pthread_mutex_destroy(combo.mutex.ptr)
//        pthread_cond_destroy(combo.cond.ptr)
        posixDestroy(comboPtr)
        nativeHeap.free(comboPtr)
    }

}


@OptIn(ExperimentalForeignApi::class)
internal object FutexDelegator: ParkingDelegator {
    override fun createFutexPtr(): Long {
        val signal = nativeHeap.alloc<UInt64Var>()
        signal.value = 0u
        return signal.ptr.toLong()
    }

    override fun wait(futexPrt: Long): Boolean {
        val cPointer = futexPrt.toCPointer<UInt64Var>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
        val result = platform.darwin.ulock.__ulock_wait(UL_COMPARE_AND_WAIT, cPointer, 0u, 0u)
        nativeHeap.free(cPointer)
        // THere is very little information about ulock so not sure what returned int stands for an interrupt
        // In any case it should be 0
        return result != 0
    }

    override fun wake(futexPrt: Long): Int {
        return platform.darwin.ulock.__ulock_wake(UL_COMPARE_AND_WAIT, futexPrt.toCPointer<UInt64Var>(), 0u)
    }

    override fun manualDeallocate(futexPrt: Long) {
        val cPointer = futexPrt.toCPointer<UInt64Var>() ?: throw IllegalStateException("Could not create C Pointer from futex ref")
        nativeHeap.free(cPointer)
    }

    private const val UL_COMPARE_AND_WAIT: UInt32 = 1u
}
