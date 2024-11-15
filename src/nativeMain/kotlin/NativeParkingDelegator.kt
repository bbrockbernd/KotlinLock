internal expect object NativeParkingDelegator: ParkingDelegator {
    override fun createFutexPtr(): Long
    override fun wait(futexPrt: Long): Boolean
    override fun wake(futexPrt: Long): Int
    override fun manualDeallocate(futexPrt: Long)
}