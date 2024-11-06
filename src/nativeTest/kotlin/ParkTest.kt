import platform.posix.sleep
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.Test
import kotlin.test.assertTrue

class ParkTest {
    
    @Test
    fun testPark() {
        val parker = ParkingDelegator()
        val ptr = parker.createFutexPtr()
        
        val worker = Worker.start()
        worker.execute(TransferMode.UNSAFE, { Pair(ptr, parker) }) { pair ->
            sleep(5u)
            pair.second.wake(pair.first)
        }
        
        parker.wait(ptr, {})
        assertTrue(true)
    }
}