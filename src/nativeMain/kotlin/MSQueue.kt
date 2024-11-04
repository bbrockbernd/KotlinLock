import kotlin.concurrent.AtomicReference

// Based on Micheal-Scott Queue
class ParkingQueue {
    private val head: AtomicReference<Node>
    private val tail: AtomicReference<Node>

    init {
        val first = Node()
        // Free up lock for first thread
        first.parker.unpark()
        head = AtomicReference(first)
        tail = AtomicReference(first)
    }

    // Returns node with a parker and garuanteed next node.
    // Should park with parker in current node and unpark the parker in next (after critical section!!)
    fun enqueue(): Node {
        while (true) {
            val node = Node()
            val curTail = tail.value
            if (curTail.next.compareAndSet(null, node)) {
                tail.compareAndSet(curTail, node)
                return curTail 
            }
            else tail.compareAndSet(curTail, curTail.next.value!!)
        }
    }

    fun dequeue() {
        while (true) {
            val currentHead = head.value
            val currentHeadNext = currentHead.next.value ?: throw IllegalStateException("Dequeing parker but already empty, should not be possible")
            if (head.compareAndSet(currentHead, currentHeadNext)) return 
        }
    }
    
    class Node {
        val parker = ThreadParker()
        val next = AtomicReference<Node?>(null)
    }
}