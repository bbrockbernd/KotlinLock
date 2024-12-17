import kotlinx.atomicfu.atomic
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.Test
import platform.posix.*
import kotlin.random.Random
import kotlin.random.nextUInt

class ThreadParkingStressTest {
    
    @Test
    fun lotsOfParking() {
        println("Starting test")
        val parker = ThreadParker(PosixParkingDelegator)
        val pt = ParkTest(parker)

        val worker1 = Worker.start()
        val future1 = worker1.execute(TransferMode.UNSAFE, { pt }) { pt ->
            repeat(10000) { i ->
                println("Iteration $i")
                if (Random.nextBoolean()) {
                    usleep(Random.nextUInt(0u, 500u))
                    pt.parker.park()
                } else {
                    pt.parker.parkNanos(Random.nextLong(0, 500))
                }
            }
            pt.done.value = true
        }
        val worker2 = Worker.start()
        val future2 = worker2.execute(TransferMode.UNSAFE, { pt }) { pt ->
            while(!pt.done.value) {
                usleep(Random.nextUInt(0u, 500u))
                pt.parker.unpark()
            }
        }
        val worker3 = Worker.start()
        val future3 = worker3.execute(TransferMode.UNSAFE, { pt }) { pt ->
            while(!pt.done.value) {
                usleep(Random.nextUInt(0u, 500u))
                pt.parker.unpark()
            }
        }
        
        future1.result
        future2.result
        future3.result
    }
    
    internal class ParkTest(val parker: ThreadParker) {
        val done = atomic(false)
    }
        
    
}