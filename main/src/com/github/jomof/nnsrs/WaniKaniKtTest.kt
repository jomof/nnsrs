package com.github.jomof.nnsrs

import org.junit.Test
import java.io.File
import java.net.URL

class WaniKaniKtTest {
    val reviewsFile = File("data/wanikani/reviews.txt")

    @Test
    fun user() {
        val user = wanikaniUser()
        println(user)
    }

    @Test
    fun reviews() {
        reviewsFile.parentFile.mkdirs()
        println(reviewsFile.absoluteFile)
        var reviews = wanikaniReviews()
        val result = mutableListOf<Review>()
        do {
            result.addAll(reviews.data)
            if (reviews.pages.nextUrl == null) break
            reviews = wanikaniReviews(URL(reviews.pages.nextUrl))
        } while (true)
        writeJson(result, reviewsFile)
        val rt = readJson(reviewsFile, Array<Review>::class.java)

        println(reviews.data.asSequence().filter { it.data.assignmentId == 76351697 }.sortedBy { it.dataUpdatedAt }.toList())
    }

    @Test
    fun assignments() {
        val assignments = wanikaniAssignments()
        println(assignments.data.asSequence().filter { it.id == 76351697 }.sortedBy { it.dataUpdatedAt }.toList())
    }

}