package com.github.jomof.nnsrs

import org.junit.Test

class ActorsKtTest {
    @Test
    fun test() {

        inputsWindow(sampleDumbActorInteraction()).take(1000).forEach {
            println(it)
        }
    }

}