package com.github.jomof.nnsrs

import org.joda.time.DateTime
import org.junit.Test

class WaniKaniKtTest {

    @Test
    fun user() {
        val user = wanikaniUser()
        println(user)
    }

    @Test
    fun reviews() {
        val reviews = wanikaniReviews()
        println(reviews.data.filter { it.data.subjectId == 536 })
    }

    @Test
    fun parseDateTime() {
        val dt = DateTime("2018-12-29T17:08:34.578186Z")
        //println(dt)
    }
}