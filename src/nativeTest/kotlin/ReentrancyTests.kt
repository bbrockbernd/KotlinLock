import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertFails

@Ignore
class ReentrancyTests {
    
    @Test
    fun reentrantTest() {
        val lock = NativeMutex { NativeParkingDelegator }
        lock.lock()
        lock.lock()
        lock.unlock()
        lock.unlock()
    }
    
    @Test
    fun reentrantTest2() {
        val lock = NativeMutex { NativeParkingDelegator }
        lock.lock()
        lock.lock()
        lock.unlock()
        lock.unlock()
        assertFails {
            lock.unlock()
        }
    }
}