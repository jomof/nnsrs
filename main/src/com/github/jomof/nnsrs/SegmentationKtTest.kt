package com.github.jomof.nnsrs

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.lang.Integer.min
import java.util.*

val reviews = readJson(File("data/wanikani/reviews.txt"), Array<Review>::class.java)

class SegmentationKtTest {

    @Test
    fun oneOff() {
        assertThat(0.toOneOff(3)).isEqualTo("1,0,0")
        assertThat(1.toOneOff(3)).isEqualTo("0,1,0")
        assertThat(2.toOneOff(3)).isEqualTo("0,0,1")
        assertThat(100.toOneOff(3)).isEqualTo("0,1,0")
    }

    @Test
    fun getMaxReviewHistorySizeTest() {
        val random = Random()
        val groups = reviews.toList().groupBy { it.data.subjectId }
        val windows = groups.map { (id, reviews) -> slideReviews(30, reviews) }.flatten()
        val (passes, fails) = windows.partition { it.currentPass }
        val oversampledFails = mutableListOf<ReviewHistoryWindow>()
        val shuffleableFails = fails.toMutableSet()
        while(oversampledFails.size < passes.size) {
            shuffleableFails.shuffled()
            val need = passes.size - oversampledFails.size
            val have = min(fails.size, need)
            oversampledFails.addAll(shuffleableFails.take(have))
        }
        val balanced = (oversampledFails + passes).toMutableList()
        balanced.shuffle(random)
        //val (minDays, maxDays) = minmaxDays(balanced)
        //val (minId, maxId) = minmaxSubjectId(balanced)
        //val normalized = balanced.map { it.normalize(minDays, maxDays) }
        //println("$minDays $maxDays")
        //println("$minId $maxId")
        val file = File("data.csv")
        file.writeText("")

        balanced.onEach { it ->
            file.appendText("${it.toString(8, random)}\n")
        }
    }
}