import kotlinx.atomicfu.locks.ReentrantLock
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.DurationUnit
import kotlin.time.measureTime

/**
 * Compares to the atomicfu implementation.
 * Each thread has a number id, n threads have numbers 0 until n.
 * A counter protected by mutex needs to be incremented unttil 10.000
 * A thread can only increment when the counter is counter mod id. This tests fariness and progress. For each thread.
 */
class CompareToAtomicFU {
    @Test
    fun compareWithAtomicFUSingleThread() {
        var accumulatedDifference = 0L
        repeat(3) {
            val time1 = measureTime {
                singleTOld()
            }
            println("Old $time1")
            val time2 = measureTime {
                singleTNew()
            }
            println("New $time2")
            accumulatedDifference += time1.toLong(DurationUnit.MILLISECONDS) - time2.toLong(DurationUnit.MILLISECONDS)
        }
        assertTrue(accumulatedDifference > 0)
    }
    
    @Test
    fun compareAtomicFU3Threads() = compareAtomicFUMultiThread(3)
    
    @Test
    fun compareAtomicFU5Threads() = compareAtomicFUMultiThread(5)
    
    @Test
    fun compareAtomicFU7Threads() = compareAtomicFUMultiThread(7)
    
    fun compareAtomicFUMultiThread(nThreads: Int) {
        var accumulatedDifference = 0L
        repeat(3) {
            val timeNew = measureTime {
                val newLock = NewLockInt()
                mulitTestLock(newLock, nThreads)
            }
            println("New $timeNew")
            
            val timeOld = measureTime {
                val oldLock = OldLockInt()
                mulitTestLock(oldLock, nThreads)
            }
            println("Old $timeOld")
            accumulatedDifference += timeOld.toLong(DurationUnit.MILLISECONDS) - timeNew.toLong(DurationUnit.MILLISECONDS)
        }
        assertTrue(accumulatedDifference > 0)
    }

    fun singleTNew() {
        val nativeMutex = NativeMutex { FutexParkingDelegator }
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
    
    fun mulitTestLock(lockInt: LockInt, nThreads: Int) {
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
        private val lock = NativeMutex { FutexParkingDelegator }
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