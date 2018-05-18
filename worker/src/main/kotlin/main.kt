import platform.posix.*
import konan.worker.*

fun sleep(seconds: Int) {
    usleep(seconds * 1000000)
}

class Singleton {
    var age = 20

    init {
        println("singleton is created.")
    }

    companion object {
        private var me = Singleton()
        fun get() = me
    }
}

// スタンダードなWorkerの利用
fun standardSample() {
    val worker = startWorker()

    val producer = { 18782 }

    val future = worker.schedule(TransferMode.CHECKED, producer) { input: Int ->
        // 2秒後に終了するジョブ
        sleep(2)
        println("worker done!")
        input + input
    }

    // 0秒で終了する処理
    println("process 1 done!")

    // 1秒で終了する処理
    sleep(1)
    println("process 2 done!")

    // 2秒で終了する処理
    sleep(2)
    println("process 3 done!")

    // 結果を確認(ブロッキング)
    future.consume { println("sync result = $it") }
}

// 通常jobラムダ式内では, jobラムダ式外のオブジェクトを参照することが出来ないがグローバル変数ではできる(複製されている)
var globalInt = 10

fun globalVarSample() {
    globalInt = 20
    println("main1: globalInt = $globalInt!")

    val worker = startWorker()

    val future = worker.schedule(TransferMode.CHECKED, { 18782 }) {
        sleep(1)
        println("worker: globalInt = $globalInt!")
        it + it
    }

    println("main2: globalInt = $globalInt!")

    future.consume { println("sync result = $it") }
}

// グローバル変数の利用は初期値が代入されたオブジェクトを複製してjob内で利用するが, シングルトンの場合はどうなるのかの検証
fun singletonSample() {
    Singleton.get().age = 30
    println("main1: singleton.age = ${Singleton.get().age}")

    val worker = startWorker()

    val future = worker.schedule(TransferMode.CHECKED, { 18782 }) {
        sleep(1)
        println("worker: singleton.age = ${Singleton.get().age}")
        it + it
    }

    println("main2: singleton.age = ${Singleton.get().age}")

    future.consume { println("sync result = $it") }
}


// consume, resultを行うとブロッキング処理が起きるため, 時間単位で監視してノンブロッキングにさせるサンプル
fun nonBlockingSample() {
    val workers = List(10, { _ -> startWorker() })
    val futures = workers.mapIndexed { index, worker ->
        worker.schedule(konan.worker.TransferMode.CHECKED, { index }) {
            sleep(it)
            kotlin.io.println("\ndone1![$it]")
            "done2![$it]"
        }
    }

    var consumed = 0
    while (consumed < futures.size) {
        val notWaited = futures
                .waitForMultipleFutures(100)
                .map {
                    println(it.result())
                    consumed++
                }
                .isEmpty()
        if (notWaited) {
            usleep(1000 * 100)
        }
        print(".") // if show '.', it is not blocking
    }

    workers.forEach { it.requestTermination().consume { _ -> } }
}

fun main(args: Array<String>) {
    println("-- standard ------------------------------------->\n")
    standardSample()
    println("\n-- global value ------------------------------------->\n")
    globalVarSample()
    println("\n-- global object ------------------------------------->\n")
    singletonSample()
    println("\n-- non blocking ------------------------------------->\n")
    nonBlockingSample()
}