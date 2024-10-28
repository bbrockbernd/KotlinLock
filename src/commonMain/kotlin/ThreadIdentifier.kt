import kotlinx.atomicfu.atomic

private val threadCounter = atomic(0L)

@kotlin.native.concurrent.ThreadLocal
private var threadId: Long = threadCounter.addAndGet(1)

internal fun currentThreadId(): Long = threadId

internal fun currentThreadName(): String = "thread@$threadId"



