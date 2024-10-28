import platform.posix.sleep
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ObsoleteWorkersApi::class)
class ThreadParkerTest {

    @Test
    fun parkUnpark() {
        currentThreadId()
        println("Started Main: ${currentThreadId()}")

        val peter = ThreadParker()

        val worker = Worker.start()
        worker.execute(TransferMode.UNSAFE, { peter }) { p ->
            currentThreadId()
            println("Started Worker going to sleep: ${currentThreadId()}")
            sleep(5u)
            println("Unparking from: ${currentThreadId()}")
            p.unpark()
            println("Unparked from: ${currentThreadId()}")
        }

        println("Parking from: ${currentThreadId()}")
        peter.park()
        println("Unparked at: ${currentThreadId()}")


        assertTrue(false)

    }
}
