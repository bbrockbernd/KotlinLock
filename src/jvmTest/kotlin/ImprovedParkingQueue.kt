import java.util.concurrent.atomic.AtomicReference

// Based on Micheal-Scott Queue
class ImprovedParkingQueue {
    private val head: AtomicReference<Node>
    private val tail: AtomicReference<Node>

    init {
        val first = Node()
        head = AtomicReference(first)
        tail = AtomicReference(first)
    }

    fun getHead(): Node {
        return head.get()
    }

    fun enqueue(): Node {
        while (true) {
            val node = Node()
            val curTail = tail.get()
            if (curTail.next.compareAndSet(null, node)) {
                tail.compareAndSet(curTail, node)
                return curTail
            }
            else tail.compareAndSet(curTail, curTail.next.get()!!)
        }
    }

    fun dequeue() {
        while (true) {
            val currentHead = head.get()
            val currentHeadNext = currentHead.next.get() ?: throw IllegalStateException("Dequeing parker but already empty, should not be possible")
            if (head.compareAndSet(currentHead, currentHeadNext)) return
        }
    }

    class Node {
        val parker = ThreadParker()
        val next = AtomicReference<Node?>(null)
    }
}
