import kotlinx.atomicfu.locks.ReentrantLock
import platform.posix.sleep
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.Test
import kotlin.time.measureTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class NativeMutexTest {
    
    @Test
    fun compareWithComposeSingleThread() {
        repeat(3) {
            val time1 = measureTime {
                singleTComp()
            }
            println("Old $time1")
            val time2 = measureTime {
                singleTNew()
            }
            println("New $time2")
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
    
    fun singleTNew() {
        val nativeMutex = NativeMutex()
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
        val nThreads = 50 
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
        private val lock = NativeMutex()
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
    
    
    
    @Test
    fun testNativeMutexSlow() {
        val mutex = NativeMutex()

        val worker1 = Worker.start()
        worker1.execute(TransferMode.UNSAFE, { mutex }) { mutex ->
            repeat(30) { i ->
                mutex.lock()
                println("Locked  : A $i")
                sleep(1u)
                println("Unlocked: A $i")
                mutex.unlock()
            }
        }

        val worker2 = Worker.start()
        worker2.execute(TransferMode.UNSAFE, { mutex }) { mutex ->
            repeat(30) { i ->
                mutex.lock()
                println("Locked  : B $i")
                sleep(1u)
                println("Unlocked: B $i")
                mutex.unlock()
            }
        }

        repeat(30) { i ->
            mutex.lock()
            println("Locked  : C $i")
            sleep(1u)
            println("Unlocked: C $i")
            mutex.unlock()
        }
    }

    @Test
    fun testNativeMutexFast() {
        val mutex = NativeMutex()

        val worker1 = Worker.start()
        worker1.execute(TransferMode.UNSAFE, { mutex }) { mutex ->
            repeat(30) { i ->
                mutex.lock()
                println("Locked  : A $i")
                println("Unlocked: A $i")
                mutex.unlock()
            }
            println("A DONE")
        }

        val worker2 = Worker.start()
        worker2.execute(TransferMode.UNSAFE, { mutex }) { mutex ->
            repeat(30) { i -> 
                mutex.lock()
                println("Locked  : B $i")
                println("Unlocked: B $i")
                mutex.unlock()
            }
            println("B DONE")
        }

        repeat(30) { i ->
            mutex.lock()
            println("Locked  : C $i")
            println("Unlocked: C $i")
            mutex.unlock()
        }
        println("C DONE")

    }
    
    @OptIn(ExperimentalUuidApi::class)
    inline fun doCriticalStuff(mutex: NativeMutex) {
        val randomCode = Uuid.random()
        mutex.lock()
        println("Locked  : $randomCode")
        sleep(1u)
        println("Unlocked: $randomCode")
        mutex.unlock()
    }
    
}