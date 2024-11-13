import kotlinx.atomicfu.atomic
import kotlin.concurrent.getOrSet

//private val threadCounter = atomic(0L)
//
//private val threadLocal = ThreadLocal<Long>()
//
//private var threadId: Long = threadLocal.getOrSet { threadCounter.addAndGet(1) }

actual fun currentThreadId(): Long = Thread.currentThread().id

//internal fun currentThreadName(): String = "thread@$threadId"


