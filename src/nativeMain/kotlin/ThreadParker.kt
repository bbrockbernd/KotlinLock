import kotlin.concurrent.AtomicInt
import kotlin.concurrent.AtomicLong

// Requires linux 2.6 (2003)
// Or Darwin 16 (macOS 10.12, iOS 10.0, tvOS 10.0, and watchOS 3.0)
actual class ThreadParker {
    private val state = AtomicInt(STATE_FREE)
    private val atomicPtr = AtomicLong(0)
    private val delegator = ParkingDelegator()

    actual fun park() {
        while (true) {
            val currentState = state.value
            if (currentState == STATE_FREE) {
                if (!state.compareAndSet(currentState, STATE_PARKED)) continue
                atomicPtr.value = delegator.createFutexPtr()
                delegator.wait(atomicPtr.value) { res ->
                    state.value = STATE_FREE
                    println("[DEBUG] Park result: $res")
                    atomicPtr.value = 0
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
            val currentState = state.value
            if (currentState == STATE_UNPARKED) return
            if (currentState == STATE_FREE) {
                if (!state.compareAndSet(currentState, STATE_UNPARKED)) continue
                return
            }
            if (currentState == STATE_PARKED) {
                val result = delegator.wake(atomicPtr.value)
                println("[DEBUG] Unpark result: $result")
                if (result == 0) return
            }
        }
    }
}

const val STATE_UNPARKED = 0
const val STATE_FREE = 1
const val STATE_PARKED = 2

