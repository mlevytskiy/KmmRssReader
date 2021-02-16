package com.github.jetbrains.rssreader

import kotlin.test.Test
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SharedFlowTest {

    @Test
    fun f() {
        runBlocking {
            val f = MutableSharedFlow<Int>()
            launch {
                println("collecting")
                f.take(2).collect { println(it) }
                println("collect ended")
            }

            launch {
                println("subscription count = ${f.subscriptionCount.value}")
                println("emitting")
                f.emit(1)
                println(f.tryEmit(2))
                f.emit(3)
                println("emitted")
            }
        }
    }
}