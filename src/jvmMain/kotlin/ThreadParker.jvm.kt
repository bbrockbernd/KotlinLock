import java.util.concurrent.locks.LockSupport

actual class ThreadParker {
    var thread: Thread? = null
    actual fun park() {
        thread = Thread() 
        LockSupport.park()
        thread = null
    }

    actual fun unpark() {
        LockSupport.unpark(thread)
    }

}