import kotlinx.atomicfu.AtomicInt
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicInteger

class NativeMutexOnJvm {
    private val parkingQueue = ImprovedParkingQueue()
    private val owningThread = AtomicLong(-1)
    private val state = AtomicInteger(0)
    private val holdCount = AtomicInteger(0)


    fun lock() {
        val currentThreadId = currentThreadId()
        // Has to be checked in this order!
        // Is reentring thread 
        if (holdCount.get() > 0 && currentThreadId == owningThread.get()) {
            holdCount.incrementAndGet()
            return
        }

        // Other wise try acquire lock
        val newState = state.incrementAndGet()
        // If new state 1 than I have acquired lock skipping queue.
        if (newState == 1) {
            owningThread.set(currentThreadId)
            holdCount.incrementAndGet()
            return
        }

        // If state larger than 1 -> enqueue and park
        // When woken up thread has acquired lock and his node in the queue is therefore at the head.
        // Remove head
        if (newState > 1) {
            val prevNode = parkingQueue.enqueue()
            prevNode.parker.park()
            parkingQueue.dequeue()
            owningThread.set(currentThreadId)
            holdCount.incrementAndGet()
            return
        }
    }

    fun unlock() {
        val currentThreadId = currentThreadId()
        val currentOwnerId = owningThread.get()
        if (currentThreadId != currentOwnerId) throw IllegalStateException("Thread is not holding the lock")

        // dec hold count
        val newHoldCount = holdCount.decrementAndGet()
        if (newHoldCount > 0) return
        if (newHoldCount < 0) throw IllegalStateException("Thread unlocked more than it locked")

        // Lock is released by decrementing (only if decremented to 0)
        val currentState = state.decrementAndGet()
        if (currentState == 0) return

        // If waiters wake up the first in line. The woken up thread will dequeue the node.
        if (currentState > 0) {
            val nextParker = parkingQueue.getHead()
            nextParker.parker.unpark()
            return
        }
    }

    fun isLocked(): Boolean {
        return state.get() > 0
    }

    fun tryLock(): Boolean {
        if (state.compareAndSet(0, 1)) {
            owningThread.set(currentThreadId())
            return true
        }
        return false
    }

}
