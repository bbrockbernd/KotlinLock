import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toCPointer
import platform.windows.UINT64Var
import platform.windows.WaitOnAddress

@OptIn(ExperimentalForeignApi::class)
actual class ParkingDelegator actual constructor() {
    actual fun createFutexPtr(): Long {
        TODO("Not yet implemented")
    }

    actual fun wait(futexPrt: Long, notifyWake: (result: Int) -> Unit) {
//        val ptr = futexPrt.toCPointer<UINT64Var>()
//        WaitOnAddress(ptr, , 8, )
    }

    actual fun wake(futexPrt: Long): Int {
        TODO("Not yet implemented")
    }
}