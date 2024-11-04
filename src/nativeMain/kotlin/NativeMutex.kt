import kotlin.concurrent.AtomicReference

// TODO Check and fix reentrancy
class NativeMutex {
    private val parkingQueue = ParkingQueue()
    private val owningThread = AtomicReference(NO_THREAD)
    private var parkerToUnpark: ThreadParker? = null
    
    
    fun lock() {
        val currentThreadId = currentThreadId()
        val prevNode = parkingQueue.enqueue()
        // Only actually parks when locked, otherwise parker was pre-unparked
        prevNode.parker.park()
        
        // When I am here I have acquired the lock.
        parkerToUnpark = prevNode.next.value!!.parker
        if (!owningThread.compareAndSet(NO_THREAD, currentThreadId)) throw IllegalStateException("Entered critical section but lock was not free")
    }
    
    fun unlock() {
        val currentThreadId = currentThreadId()
        val currentOwnerId = owningThread.value
        
        if (currentThreadId != currentOwnerId) throw IllegalStateException("Thread is not holding the lock")
        
        // Remove head (which is my node)
        parkingQueue.dequeue()
        
        //set lock to free
        owningThread.value = NO_THREAD
        val nextParker = parkerToUnpark
        parkerToUnpark = null
        nextParker!!.unpark()
    }
    
    fun isLocked(): Boolean {
        return owningThread.value != NO_THREAD
    }
}

const val NO_THREAD = -1L