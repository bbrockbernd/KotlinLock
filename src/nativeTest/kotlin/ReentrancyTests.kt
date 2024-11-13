import kotlin.test.Test
import kotlin.test.assertFails

class ReentrancyTests {
    
    @Test
    fun reentrantTest() {
        val lock = NativeMutex()
        lock.lock()
        lock.lock()
        lock.unlock()
        lock.unlock()
        
    }
    
    @Test
    fun reentrantTest2() {
        val lock = NativeMutex()
        lock.lock()
        lock.lock()
        lock.unlock()
        lock.unlock()
        assertFails {
            lock.unlock()
        }
    }
}