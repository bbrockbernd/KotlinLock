expect object ParkingUtils {
    fun createFutexPtr(): Long
    fun wait(futexPrt: Long, notifyWake: (interrupted: Boolean) -> Unit)
    fun wake(futexPrt: Long): Int
    fun manualDeallocate(futexPrt: Long)
}