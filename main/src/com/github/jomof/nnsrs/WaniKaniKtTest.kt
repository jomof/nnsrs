package com.github.jomof.nnsrs

import org.junit.Test
import java.io.File
import java.net.URL

class WaniKaniKtTest {
    val reviewsFile = File("data/wanikani/reviews.txt")
    val subjectsFile = File("data/wanikani/subjects.txt")

    @Test
    fun user() {
        val user = wanikaniUser()
        println(user)
    }

    @Test
    fun reviews() {
        if (!reviewsFile.exists()) {
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
        }
        val rt = readJson(reviewsFile, Array<Review>::class.java)

        println(rt.asSequence().filter { it.data.assignmentId == 76351697 }.sortedBy { it.dataUpdatedAt }.toList())
    }

    @Test
    fun assignments() {
        val assignments = wanikaniAssignments()
        println(assignments.data.asSequence().filter { it.id == 76351697 }.sortedBy { it.dataUpdatedAt }.toList())
    }

    @Test
    fun subjects() {
        if (!subjectsFile.exists()) {
            subjectsFile.parentFile.mkdirs()
            println(subjectsFile.absoluteFile)
            val subjects = wanikaniSubjects().asSequence().flatten().filter { it.id == 7739 }. toList()
            writeJson(subjects, subjectsFile)
        }
    }

    data class Denormalized(
            val output : Long,
            val answers : List<Long>,
            val times : List<Long>,
            val bitField : List<Long>
    ) {
        fun output() : String {
            return "$output"
        }
        fun input(min : Long, max : Long) : String {
            val result = mutableListOf<Double>()
            result.addAll(answers.map { it.toDouble() })
            result.addAll(times.map { (it.toDouble() - min) / (0.0 + max - min) })
            result.addAll(bitField.map { it.toDouble() })
            return result.joinToString(",")
        }
    }

    @Test
    fun windowedReviews() {
        val fullsize = 8
        fun padToFullSize(list : List<Long>, with : Long = 0L) : List<Long> {
            return list + (0 until fullsize - list.size - 1).map { with }
        }
        fun succeed( review : Review) = if (review.data.startingSrsStage < review.data.endingSrsStage) 1L else 0L
        var min = Long.MAX_VALUE
        var max = Long.MIN_VALUE
        val full = mutableListOf<Denormalized>()
        val data = readJson(reviewsFile, Array<Review>::class.java)
                .sortedWith(compareBy({ it.data.subjectId }, { it.data.createdAt }))
        (fullsize downTo 2).onEach { windowSize ->
            full.addAll(data
                    //.filter { it.data.subjectId == 1 }
                    .windowed(windowSize)
                    .filter { windows -> windows.map { it.data.subjectId }.toSet().size == 1 }
                    .map { windows ->
                        val descending = windows.reversed()
                        val now = descending[0].data.createdAt
                        val priors = descending.drop(1)
                        val deltaTimes = padToFullSize(priors.map { now.millis - it.data.createdAt.millis }, now.millis - descending.last().data.createdAt.millis)
                        val successes = padToFullSize(priors.map { succeed(it) }, succeed(descending.last()))
                        val set = padToFullSize(priors.map { 1L }, 0L)
                        val outcome = succeed(descending[0])
                        Denormalized(outcome, successes, deltaTimes, set)
                    }
                    .onEach { (_, _, times) ->
                        min = Math.min(min, times.min()!!)
                        max = Math.max(max, times.max()!!)
                    })
        }
        val outfile = File("outfile.csv")
        val infile = File("infile.csv")
        outfile.writeText("")
        infile.writeText("")
        full.onEach {
            outfile.appendText(it.output() + "\n")
            infile.appendText(it.input(min, max) + "\n")
        }
        println("$min, $max")

    }

}