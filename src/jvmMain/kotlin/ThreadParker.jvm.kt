import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

actual class ThreadParker {
    private val state = AtomicInteger(STATE_FREE)
    private var atomicRef: AtomicReference<LongRef?> = AtomicReference(null) 
    private val delegator = JvmParkerWrapper()

    actual fun park() {
        while (true) {
            val currentState = state.get()
            if (currentState == STATE_FREE) {
                if (!state.compareAndSet(currentState, STATE_PARKED)) continue
                initLongIfAbsent()
                delegator.wait(atomicRef.get()!!) { res ->
                    state.set(STATE_FREE)
                    atomicRef.get()!!.value.set(0)
                }
                return
            }
            if (currentState == STATE_UNPARKED) {
                if (!state.compareAndSet(currentState, STATE_FREE)) continue
                return
            }
            if (currentState == STATE_PARKED) {
                throw IllegalStateException("Thread should not be able to call park when it is already parked")
            }
        }
    }

    // Is probably not thread safe
    actual fun unpark() {
        while (true) {
            val currentState = state.get()
            if (currentState == STATE_UNPARKED) return
            if (currentState == STATE_FREE) {
                if (!state.compareAndSet(currentState, STATE_UNPARKED)) continue
                return
            }
            if (currentState == STATE_PARKED) {
                initLongIfAbsent()
                val result = delegator.wake(atomicRef.get()!!)
                return
            }
        }
    }
    
    private fun initLongIfAbsent() {
        val ptrVal = atomicRef.get()
        if (ptrVal == null) {
            val currentPtr = delegator.createFutexPtr()
            if (atomicRef.compareAndSet(null, currentPtr)) return
            // Manual deallocate is not necessary on jvm
        }
    }
}
const val STATE_UNPARKED = 0
const val STATE_FREE = 1
const val STATE_PARKED = 2
