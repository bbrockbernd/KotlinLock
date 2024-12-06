import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.DurationUnit
import kotlin.time.measureTime

//@Ignore
class CompareToCompose {
    @Test
    fun compareWithComposeSingleThread() {
        var accumulatedDifference = 0L
        repeat(3) {
            val time1 = measureTime {
                singleTComp()
            }
            println("Old $time1")
            val time2 = measureTime {
                singleTNew2()
            }
            println("New $time2")
            accumulatedDifference += time1.toLong(DurationUnit.MILLISECONDS) - time2.toLong(DurationUnit.MILLISECONDS)
        }
        assertTrue(accumulatedDifference > 0)
    }

    fun singleTComp() {
        val compLock = CompLock()
        repeat(1000000) {
            compLock.synchronized {
                val a = 1
            }
        }
    }
    
    fun singleTNew2() {
        val nativeMutex = NativeMutex { NativeParkingDelegator }
        repeat(1000000) {
            nativeMutex.lock()
            nativeMutex.unlock()
        }
    }
    
    @Test
    fun compareCompose2Threads() = compareComposeMultiThread(2)
    
    @Test
    fun compareCompose3Threads() = compareComposeMultiThread(3)

    @Test
    fun compareCompose5Threads() = compareComposeMultiThread(5)
    
    @Test
    fun compareCompose7Threads() = compareComposeMultiThread(7)

    fun compareComposeMultiThread(nThreads: Int) {
        var accumulatedDifference = 0L
        repeat(3) {
            val timeNew = measureTime {
                val newLock = NewSyncInt()
                mulitTestLock(newLock, nThreads)
            }
            println("New $timeNew")
            val timeOld = measureTime {
                val oldLock = OldSyncInt()
                mulitTestLock(oldLock, nThreads)
            }
            println("Old $timeOld")
            accumulatedDifference += timeOld.toLong(DurationUnit.MILLISECONDS) - timeNew.toLong(DurationUnit.MILLISECONDS)
        }
        assertTrue(accumulatedDifference > 0)
    }

    fun mulitTestLock(lockInt: SyncInt, nThreads: Int) {
        val countTo = 100000
        val futureList = mutableListOf<Future<Unit>>()
        repeat(nThreads) { i ->
            val test = SyncIntTest(lockInt, countTo, nThreads, i)
            futureList.add(testWithWorker(test))
        }
        futureList.forEach {
            it.result
        }
    }

    fun testWithWorker(test: SyncIntTest): Future<Unit> {
        val worker = Worker.start()
        return worker.execute(TransferMode.UNSAFE, { test }) { t ->
            var done = false
            while (!done) {
                t.syncInt.synchronized {
                    if (t.syncInt.n % t.mod == t.id) t.syncInt.n++
                    if (t.syncInt.n >= t.max) {
                        done = true
                    }
                }
            }
        }
    }

    data class SyncIntTest(
        val syncInt: SyncInt,
        val max: Int,
        val mod: Int,
        val id: Int,
    )

    class OldSyncInt(): SyncInt {
        private val lock = CompLock()
        override var n = 0
        override fun synchronized(block: () -> Unit) = lock.synchronized(block)
    }

    class NewSyncInt(): SyncInt {
        private val lock = NativeMutex { NativeParkingDelegator }
        override var n = 0
        override fun synchronized(block: () -> Unit) {
            lock.lock()
            block()
            lock.unlock()
        }
    }

    interface SyncInt {
        fun synchronized(block: () -> Unit)
        var n: Int
    }

}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
class CompLock {
    private val syncObj = androidx.compose.runtime.SynchronizedObject()
    fun synchronized(block: () -> Unit) = androidx.compose.runtime.synchronized(syncObj, block)
}
