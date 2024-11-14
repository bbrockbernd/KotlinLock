import kotlinx.atomicfu.locks.ReentrantLock
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.Test
import kotlin.time.measureTime

class CompareToAtomicFU {
    @Test
    fun compareWithAtomicFUSingleThread() {
        repeat(3) {
            val time1 = measureTime {
                singleTOld()
            }
            println("Old $time1")
            val time2 = measureTime {
                singleTNew()
            }
            println("New $time2")
        }
    }
    
    @Test
    fun compareAtomicFUMultiThread() {
        repeat(3) {
            val timeNew = measureTime {
                val newLock = NewLockInt()
                mulitTestLock(newLock)
            }
            println("New $timeNew")
            
            val timeOld = measureTime {
                val oldLock = OldLockInt()
                mulitTestLock(oldLock)
            }
            println("Old $timeOld")
        }
    }

    fun singleTNew() {
        val nativeMutex = NativeMutex()
        repeat(1000000) {
            nativeMutex.lock()
            nativeMutex.unlock()
        }
    }

    fun singleTOld() {
        val reentrantLock: ReentrantLock = ReentrantLock()
        repeat(1000000) {
            reentrantLock.lock()
            reentrantLock.unlock()
        }
    }
    
    fun mulitTestLock(lockInt: LockInt) {
        val nThreads = 5
        val countTo = 100000
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

    class NewLockInt: LockInt{
        private val lock = NativeMutex()
        override var n = 0
        override fun lock() = lock.lock()
        override fun unlock() = lock.unlock()
    }

    class OldLockInt: LockInt {
        private val lock = ReentrantLock()
        override var n = 0
        override fun lock() = lock.lock()
        override fun unlock() = lock.unlock()
    }

    interface LockInt {
        fun lock()
        fun unlock()
        var n: Int
    }
}