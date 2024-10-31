expect class ParkingDelegator() {
    fun createFutexPtr(): Long
    fun wait(futexPrt: Long, notifyWake: (Int) -> Unit)
    fun wake(futexPrt: Long): Int
}