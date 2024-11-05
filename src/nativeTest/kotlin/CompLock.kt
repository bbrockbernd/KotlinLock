@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

class CompLock {
    private val syncObj = androidx.compose.runtime.SynchronizedObject()
    fun synchronized(block: () -> Unit) = androidx.compose.runtime.synchronized(syncObj, block)
}