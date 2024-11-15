import kotlinx.atomicfu.locks.ReentrantLock
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.Test
import kotlin.time.measureTime

class FutexMutexTest {
    @Test
    fun compareSingle() {
        repeat(3) {
            val time1 = measureTime {
                singleOld()
            }
            println("Old $time1")
            
            val time2 = measureTime {
                singleNew()
            }
            println("New $time2")
        }
    }

    @Test
    fun compareMulti() {
        repeat(3) {
            val timeNew = measureTime {
                val newLock = OldLockTest()
                mulitTestLock(newLock)
            }
            println("Old $timeNew")

            val timeOld = measureTime {
                val oldLock = NewLockTest()
                mulitTestLock(oldLock)
            }
            println("New $timeOld")
        }
    }

    fun singleOld() {
        val nativeMutex = NativeMutex()
        repeat(1000000) {
            nativeMutex.lock()
            nativeMutex.unlock()
        }
    }

    fun singleNew() {
        val appleMutex: AppleMutex = AppleMutex()
        repeat(1000000) {
            appleMutex.lock()
            appleMutex.unlock()
        }
    }

    fun mulitTestLock(lockInt: LockInt) {
        val nThreads = 7
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

    class OldLockTest: LockInt{
        private val lock = NativeMutex()
        override var n = 0
        override fun lock() = lock.lock()
        override fun unlock() = lock.unlock()
    }

    class NewLockTest: LockInt {
        private val lock = AppleMutex()
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
