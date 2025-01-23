import java.util.UUID

actual class KThread internal actual constructor() {
    internal val parker = ThreadParker(JvmParkingDelegator())
    val id = UUID.randomUUID().toString()
    actual companion object {
        actual fun currentThread(): KThread = localKThread.get()
    }
}

actual class Parker private actual constructor() {
    actual companion object {
        actual fun park(): Unit = localKThread.get().parker.park()
        actual fun parkNanos(nanos: Long): Unit = localKThread.get().parker.parkNanos(nanos)
        actual fun unpark(kThread: KThread): Unit = kThread.parker.unpark()
    }
}

private val localKThread: ThreadLocal<KThread> = ThreadLocal.withInitial { KThread() }
actual fun currentThreadId(): Long = Thread.currentThread().id
