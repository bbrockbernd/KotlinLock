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
    
    
    @Test 
    fun compareMultiThread() {
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
    
    fun mulitTestLock(lockInt: LockInt) {
        val nThreads = 1 
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