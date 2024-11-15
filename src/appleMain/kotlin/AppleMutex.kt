import kotlinx.cinterop.*
import platform.darwin.UInt64Var
import platform.darwin.stdatomic.*
import platform.darwin.ulock.__ulock_wait
import platform.darwin.ulock.__ulock_wake
import platform.posix.uint64_tVar
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.createCleaner

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
class AppleMutex {
    private val unlockedRef = nativeHeap.alloc<uint64_tVar>()
    private val lockedRef = nativeHeap.alloc<uint64_tVar>()
    private val futex = nativeHeap.alloc<UInt64Var>()
    private val cleaner1 = createCleaner(unlockedRef) { nativeHeap.free(it) }
    private val cleaner2 = createCleaner(lockedRef) { nativeHeap.free(it) }
    private val cleaner3 = createCleaner(futex) { nativeHeap.free(it) }
    
    
    init {
        unlockedRef.value = 0u
        lockedRef.value = 1u
        futex.value = 0u
    }
    
    fun lock() {
        while(!native_cas(futex.ptr, unlockedRef.ptr, lockedRef.value)) {
            __ulock_wait(UL_COMPARE_AND_WAIT, futex.ptr, lockedRef.value, 0u)
        }
    }
    
    fun unlock() {
        native_atomic_write(futex.ptr, unlockedRef.value)
        __ulock_wake(UL_COMPARE_AND_WAIT, futex.ptr, 0u)
    }
    
    
}
