actual class Mutex {
    private val mutex = NativeMutex { NativeParkingDelegator }
    actual fun isLocked(): Boolean = mutex.isLocked()
    actual fun tryLock(): Boolean = mutex.tryLock()
    actual fun lock() = mutex.lock()
    actual fun unlock() = mutex.unlock()
}