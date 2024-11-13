import kotlin.concurrent.AtomicInt
import kotlin.concurrent.AtomicLong

class NativeMutex {
    private val parkingQueue = ImprovedParkingQueue()
    private val owningThread = AtomicLong(-1)
    private val state = AtomicInt(0)
    private val holdCount = AtomicInt(0)


    fun lock() {
        val currentThreadId = currentThreadId()
        // Has to be checked in this order!
        // Is reentring thread 
        if (holdCount.value > 0 && currentThreadId == owningThread.value) {
            holdCount.incrementAndGet()
            return
        }
        
        // Other wise try acquire lock
        val newState = state.incrementAndGet()
        // If new state 1 than I have acquired lock skipping queue.
        if (newState == 1) {
            owningThread.value = currentThreadId
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
            owningThread.value = currentThreadId
            holdCount.incrementAndGet()
            return
        }
    }

    fun unlock() {
        val currentThreadId = currentThreadId()
        val currentOwnerId = owningThread.value
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
        return state.value > 0
    }
    
    fun tryLock(): Boolean {
        if (state.compareAndSet(0, 1)) {
            owningThread.value = currentThreadId()
            return true
        }
        return false
    }

}