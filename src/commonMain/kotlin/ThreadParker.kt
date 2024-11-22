import kotlinx.atomicfu.atomic

/**
 * This is defined in common to be testable with lincheck.
 * Should in practice never be used on jvm.
 * 
 * Requires linux 2.6 (2003)
 * Or Darwin 16 (macOS 10.12, iOS 10.0, tvOS 10.0, and watchOS 3.0)
 * Or Windows 8 and Windows Server 2012
 */

internal class ThreadParker(private val delegator: ParkingDelegator) {
    private val state = atomic(STATE_FREE)
    private val atomicPtr = atomic(-1L)

    fun park() {
        while (true) {
            when (val currentState = state.value) {
                
                STATE_FREE -> {
                    if (!state.compareAndSet(currentState, STATE_PARKED)) continue
                    initPtrIfAbsent()
                    val interrupted = delegator.wait(atomicPtr.value)
                    // Interrupted exception does not exist on native
                    if (interrupted) throw IllegalStateException("Thread was interrupted")
                    state.value = STATE_FREE
                    atomicPtr.value = -1L
                    return
                }
                
                STATE_UNPARKED -> {
                    if (!state.compareAndSet(currentState, STATE_FREE)) continue
                    return
                }
                
                STATE_PARKED -> 
                    throw IllegalStateException("Thread should not be able to call park when it is already parked")
                
            }
        }
    }

    // Avoid calling this multiple times, not thread safe.
    // Enough for mutex impl.
    fun unpark() {
        while (true) {
            when (val currentState = state.value) {
                
                STATE_UNPARKED -> return
                
                STATE_FREE -> {
                    if (!state.compareAndSet(currentState, STATE_UNPARKED)) continue
                    return
                }
                
                STATE_PARKED -> {
                    initPtrIfAbsent()
                    val result = delegator.wake(atomicPtr.value)
                    if (result == 0) return
                }
            }
        }
    }
    
    private fun initPtrIfAbsent() {
        val ptrVal = atomicPtr.value
        if (ptrVal == -1L) {
            val currentPtr = delegator.createFutexPtr()
            if (atomicPtr.compareAndSet(ptrVal, currentPtr)) return
            delegator.manualDeallocate(currentPtr)
        }
    }
}

const val STATE_UNPARKED = 0
const val STATE_FREE = 1
const val STATE_PARKED = 2

