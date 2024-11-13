import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.LockSupport

class JvmParkerWrapper {
    private var thread: Thread? = null
    
    
    fun createFutexPtr(): LongRef {
        thread = Thread.currentThread()
        return LongRef()
    }
    fun wait(signal: LongRef, notifyWake: (Int) -> Unit) {
        while (signal.value.get() == 0L) {
            LockSupport.park()
        }
        notifyWake(0)
        thread = null
    }
    
    fun wake(signal: LongRef) {
        if (signal.value.compareAndSet(0L, 1L)) {
            LockSupport.unpark(thread)
        }
    }
    
}

class LongRef(val value: AtomicLong = AtomicLong(0L)) 
    