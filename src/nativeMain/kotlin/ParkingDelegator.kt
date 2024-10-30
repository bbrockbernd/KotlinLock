expect class ParkingDelegator() {
    fun createFutexPtr(): Long
    fun wait(futexPrt: Long, notifyWake: (result: Int) -> Unit)
    fun wake(futexPrt: Long): Int
}