import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.Test
import kotlin.time.measureTime

class CompareToCompose {
    @Test
    fun compareWithComposeSingleThread() {
        repeat(3) {
            val time1 = measureTime {
                singleTComp()
            }
            println("Old $time1")
            val time3 = measureTime {
                singleTNew2()
            }
            println("New $time3")
        }
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
//        val nativeMutex = Mutex()
        repeat(1000000) {
            nativeMutex.lock()
            nativeMutex.unlock()
        }
    }

    @Test
    fun compareComposeMultiThread() {
        repeat(3) {
            val timeNew = measureTime {
                val newLock = NewSyncInt()
                mulitTestLock(newLock)
            }
            println("New $timeNew")
            val timeOld = measureTime {
                val oldLock = OldSyncInt()
                mulitTestLock(oldLock)
            }
            println("Old $timeOld")
        }
    }

    fun mulitTestLock(lockInt: SyncInt) {
        val nThreads = 2
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
        private val lock = Mutex()
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