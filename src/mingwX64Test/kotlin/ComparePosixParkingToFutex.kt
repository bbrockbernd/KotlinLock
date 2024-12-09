import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.Test
import kotlin.time.DurationUnit
import kotlin.time.measureTime

class ComparePosixParkingToFutex{
    @Test
    fun compareSingleThreadedPosixVsFutexParking() {
        var accumulatedDifference = 0L
        repeat(3) {
            val time1 = measureTime {
                futexTestSingle()
            }
            println("Futex $time1")
            val time2 = measureTime {
                posixTestSingle()
            }
            println("Posix $time2")
            accumulatedDifference += time1.toLong(DurationUnit.MILLISECONDS) - time2.toLong(DurationUnit.MILLISECONDS)
        }
//        assertTrue(accumulatedDifference > 0)
    }

    @Test
    fun comparePosFut3() = comparePosixFutexParking(3)

    @Test
    fun comparePosFut5() = comparePosixFutexParking(5)

    @Test
    fun comparePosFut7() = comparePosixFutexParking(7)

    fun comparePosixFutexParking(nThreads: Int) {
        var accumulatedDifference = 0L
        repeat(3) {

            val timeOld = measureTime {
                val posixLock = PosixLockInt()
                println("Starting test")
                mulitTestLock(posixLock, nThreads)
            }
            println("Posix $timeOld")
            
//            val timeNew = measureTime {
//                val futexLock = FutexLockInt()
//                mulitTestLock(futexLock, nThreads)
//            }
//            println("Futex $timeNew")
//            accumulatedDifference += timeOld.toLong(DurationUnit.MILLISECONDS) - timeNew.toLong(DurationUnit.MILLISECONDS)
        }
//        assertTrue(accumulatedDifference > 0)
    }

    fun posixTestSingle() {
        val nativeMutex = NativeMutex { FutexDelegator }
        repeat(1000000) {
            nativeMutex.lock()
            nativeMutex.unlock()
        }
    }

    fun futexTestSingle() {
        val nativeMutex = NativeMutex { PosixDelegator }
        repeat(1000000) {
            nativeMutex.lock()
            nativeMutex.unlock()
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

    class FutexLockInt: LockInt{
        private val lock = NativeMutex { FutexDelegator }
        override var n = 0
        override fun lock() = lock.lock()
        override fun unlock() = lock.unlock()
    }

    class PosixLockInt: LockInt {
        private val lock = NativeMutex { PosixDelegator }
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
