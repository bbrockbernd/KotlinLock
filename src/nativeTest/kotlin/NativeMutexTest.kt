import platform.posix.sleep
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class NativeMutexTest {
    
    
    @Ignore
    @Test
    fun testNativeMutexSlow() {
        val mutex = NativeMutex { NativeParkingDelegator }

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
        val mutex = NativeMutex { NativeParkingDelegator }
        val resultList = mutableListOf<String>()

        val worker1 = Worker.start()
        val fut1 = worker1.execute(TransferMode.UNSAFE, { Pair(resultList, mutex) }) { (rl, mutex) ->
            repeat(30) { i ->
                mutex.lock()
                rl.add("a$i")
                println("Locked  : A $i")
                println("Unlocked: A $i")
                rl.add("a$i")
                mutex.unlock()
            }
            println("A DONE")
        }

        val worker2 = Worker.start()
        val fut2= worker2.execute(TransferMode.UNSAFE, { Pair(resultList, mutex) }) { (rl, mutex) ->
            repeat(30) { i -> 
                mutex.lock()
                rl.add("b$i")
                println("Locked  : B $i")
                println("Unlocked: B $i")
                rl.add("b$i")
                mutex.unlock()
            }
            println("B DONE")
        }

        repeat(30) { i ->
            mutex.lock()
            resultList.add("c$i")
            println("Locked  : C $i")
            println("Unlocked: C $i")
            resultList.add("c$i")
            mutex.unlock()
        }
        println("C DONE")
        fut1.result
        fut2.result
        
        resultList.filterIndexed { i, _ -> i % 2 == 0 }.zip(resultList.filterIndexed {i, _ -> i % 2 == 1}) { a, b ->
            assertEquals(a, b)
        }
    }
    
    @OptIn(ExperimentalUuidApi::class)
    internal inline fun doCriticalStuff(mutex: NativeMutex) {
        val randomCode = Uuid.random()
        mutex.lock()
        println("Locked  : $randomCode")
        sleep(1u)
        println("Unlocked: $randomCode")
        mutex.unlock()
    }
    
}