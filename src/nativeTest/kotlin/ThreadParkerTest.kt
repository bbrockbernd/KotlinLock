import platform.posix.sleep
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.measureTime

@OptIn(ObsoleteWorkersApi::class)
class ThreadParkerTest {

    @Test
    fun parkUnpark() {
        currentThreadId()
        println("Started Main: ${currentThreadId()}")

        val peter = ThreadParker(NativeParkingDelegator)

        val worker = Worker.start()
        worker.execute(TransferMode.UNSAFE, { peter }) { p ->
            currentThreadId()
            println("Started Worker going to sleep: ${currentThreadId()}")
            sleep(5u)
            println("Unparking from: ${currentThreadId()}")
            p.unpark()
            println("Unparked from: ${currentThreadId()}")
        }

        println("Parking from: ${currentThreadId()}")
        peter.park()
        println("Unparked at: ${currentThreadId()}")


        assertTrue(true)
    }

    @Test
    fun unparkPark() {
        currentThreadId()
        println("Started Main: ${currentThreadId()}")

        val peter = ThreadParker(NativeParkingDelegator)

        val worker = Worker.start()
        worker.execute(TransferMode.UNSAFE, { peter }) { p ->
            currentThreadId()
            println("Unparking from: ${currentThreadId()}")
            p.unpark()
            println("Unparked from: ${currentThreadId()}")
        }
        
        println("Main going to sleep before park: ${currentThreadId()}")
        sleep(5u)

        println("Parking thread: ${currentThreadId()}")
        peter.park()
        println("Continued thread: ${currentThreadId()}")


        assertTrue(true)
    }
    
    @Test
    fun speedTest() {
        val nIterations = 1000
        
            
        println("Starting test")
        val time1 = measureTime { 
            val peter = ThreadParker(NativeParkingDelegator)
            val wrapper: MutablePair<ThreadParker, Boolean> = MutablePair(peter, false)
            val worker = Worker.start()
            worker.execute(TransferMode.SAFE, { wrapper }) { w ->
                while (!w.second)
                    w.first.unpark()
            }
            
            repeat(nIterations) { i ->
                println("Parking $i")
                peter.park()
                println("Unparked")
            }
            wrapper.second = true
           
        }
        println(time1)
    }

    class MutablePair<A, B>(var first: A, var second: B)
    
}

