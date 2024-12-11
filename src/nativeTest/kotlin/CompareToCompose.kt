//import kotlin.native.concurrent.Future
//import kotlin.native.concurrent.TransferMode
//import kotlin.native.concurrent.Worker
//import kotlin.test.Test
//import kotlin.test.assertTrue
//import kotlin.time.DurationUnit
//import kotlin.time.measureTime
//
////@Ignore
//class CompareToCompose {
//    @Test
//    fun compareWithComposeSingleThread() {
//        var accumulatedDifference = 0L
//        repeat(3) {
//            val time1 = measureTime {
//                singleTComp()
//            }
//            println("Old $time1")
//            val time2 = measureTime {
//                singleTNew2()
//            }
//            println("New $time2")
//            accumulatedDifference += time1.toLong(DurationUnit.MILLISECONDS) - time2.toLong(DurationUnit.MILLISECONDS)
//        }
//        assertTrue(accumulatedDifference > 0)
//    }
//
//    fun singleTComp() {
//        val compLock = CompLock()
//        repeat(1000000) {
//            compLock.synchronized {
//                val a = 1
//            }
//        }
//    }
//    
//    fun singleTNew2() {
//        val nativeMutex = NativeMutex { PosixParkingDelegator }
//        repeat(1000000) {
//            nativeMutex.lock()
//            nativeMutex.unlock()
//        }
//    }
//    
//    @Test
//    fun compareCompose2ThreadsFair() = compareComposeMultiThread(2, true)
//    
//    @Test
//    fun compareCompose3ThreadsFair() = compareComposeMultiThread(3, true)
//
//    @Test
//    fun compareCompose5ThreadsFair() = compareComposeMultiThread(5, true)
//    
//    @Test
//    fun compareCompose7ThreadsFair() = compareComposeMultiThread(7, true)
//    
//    @Test
//    fun compareCompose2ThreadsRandom() = compareComposeMultiThread(2, false)
//    
//    @Test
//    fun compareCompose3ThreadsRandom() = compareComposeMultiThread(3, false)
//    
//    @Test
//    fun compareCompose5ThreadsRandom() = compareComposeMultiThread(5, false)
//    
//    @Test
//    fun compareCompose7ThreadsRandom() = compareComposeMultiThread(7, false)
//
//    fun compareComposeMultiThread(nThreads: Int, fair: Boolean) {
//        var accumulatedDifference = 0L
//        repeat(3) {
//            val timeNew = measureTime {
//                val newLock = NewSyncInt()
//                mulitTestLock(newLock, nThreads, fair)
//            }
//            println("New $timeNew")
//            val timeOld = measureTime {
//                val oldLock = OldSyncInt()
//                mulitTestLock(oldLock, nThreads, fair)
//            }
//            println("Old $timeOld")
//            accumulatedDifference += timeOld.toLong(DurationUnit.MILLISECONDS) - timeNew.toLong(DurationUnit.MILLISECONDS)
//        }
//        assertTrue(accumulatedDifference > 0)
//    }
//
//    fun mulitTestLock(lockInt: SyncInt, nThreads: Int, fair: Boolean) {
//        val countTo = 100000
//        val futureList = mutableListOf<Future<Unit>>()
//        repeat(nThreads) { i ->
//            val test = SyncIntTest(lockInt, countTo, nThreads, i, fair)
//            futureList.add(testWithWorker(test))
//        }
//        futureList.forEach {
//            it.result
//        }
//    }
//
//    fun testWithWorker(test: SyncIntTest): Future<Unit> {
//        val worker = Worker.start()
//        return worker.execute(TransferMode.UNSAFE, { test }) { t ->
//            var done = false
//            while (!done) {
//                t.syncInt.synchronized {
//                    if (t.fair && t.syncInt.n % t.mod == t.id) t.syncInt.n++
//                    if (!t.fair && t.syncInt.rand == t.id) {
//                        t.syncInt.n++
//                        t.syncInt.rand = (0..< t.mod).random()
//                    }
//                    if (t.syncInt.n >= t.max) {
//                        done = true
//                    }
//                }
//            }
//        }
//    }
//
//    data class SyncIntTest(
//        val syncInt: SyncInt,
//        val max: Int,
//        val mod: Int,
//        val id: Int,
//        val fair: Boolean,
//    )
//
//    class OldSyncInt(): SyncInt {
//        private val lock = CompLock()
//        override var n = 0
//        override var rand = 0
//        override fun synchronized(block: () -> Unit) = lock.synchronized(block)
//    }
//
//    class NewSyncInt(): SyncInt {
//        private val lock = NativeMutex { PosixParkingDelegator }
//        override var n = 0
//        override var rand = 0
//        override fun synchronized(block: () -> Unit) {
//            lock.lock()
//            block()
//            lock.unlock()
//        }
//    }
//
//    interface SyncInt {
//        fun synchronized(block: () -> Unit)
//        var n: Int
//        var rand: Int
//    }
//
//}
//
//@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
//class CompLock {
//    private val syncObj = androidx.compose.runtime.SynchronizedObject()
//    fun synchronized(block: () -> Unit) = androidx.compose.runtime.synchronized(syncObj, block)
//}
