import kotlinx.atomicfu.locks.ReentrantLock
import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.Test
import kotlin.test.assertTrue

class VaryingContentionTest {
    
    @Test
    fun compareAtomicFUMultiThread() {
        val lockInt = NewLockInt2()
        mulitTestLock(lockInt, 10, 100000)
        println("1")
        mulitTestLock(lockInt, 1, 200000)
        println("2")
        mulitTestLock(lockInt, 20, 300000)
        println("3")
        mulitTestLock(lockInt, 1, 400000)
        println("4")
        mulitTestLock(lockInt, 30, 800000)
        println("done")
    }

    
    fun mulitTestLock(lockInt: LockInt, nThreads: Int, countTo: Int) {
        val futureList = mutableListOf<Future<Unit>>()
        repeat(nThreads) { i ->
            val test = LockIntTest(lockInt, countTo, nThreads, i)
            futureList.add(testWithWorker(test))
        }
        futureList.forEach {
            it.result
        }
    }

    fun testWithWorker(test: LockIntTest): Future<Unit> {
        val worker = Worker.start()
        return worker.execute(TransferMode.UNSAFE, { test }) { t ->
            while (true) {
                t.lockInt.lock()
                if (t.lockInt.n % t.mod == t.id) t.lockInt.n++
                if (t.lockInt.n >= t.max) {
                    t.lockInt.unlock()
                    break
                }
                t.lockInt.unlock()
            }
        }
    }
    
    data class LockIntTest(
        val lockInt: LockInt,
        val max: Int,
        val mod: Int,
        val id: Int,
    )
    
    class NewLockInt2: LockInt{
        private val lock = NativeMutex()
        private val check = AtomicInt(0)
        override var n = 0
        override fun lock() {
            lock.lock()
            assertTrue(check.incrementAndGet() == 1)
        }
        
        override fun unlock() {
            assertTrue(check.decrementAndGet() == 0)
            lock.unlock()
        }
        
    }

    class NewLockInt: LockInt{
        private val lock = NativeMutex()
        override var n = 0
        private val check = AtomicInt(0)
        override fun lock() {
            lock.lock()
            assertTrue(check.incrementAndGet() == 1)
        }

        override fun unlock() {
            assertTrue(check.decrementAndGet() == 0)
            lock.unlock()
        }
    }

    class OldLockInt: LockInt {
        private val lock = ReentrantLock()
        override var n = 0
        private val check = AtomicInt(0)
        override fun lock() {
            lock.lock()
            assertTrue(check.incrementAndGet() == 1)
        }

        override fun unlock() {
            assertTrue(check.decrementAndGet() == 0)
            lock.unlock()
        }
    }

    interface LockInt {
        fun lock()
        fun unlock()
        var n: Int
    }
}