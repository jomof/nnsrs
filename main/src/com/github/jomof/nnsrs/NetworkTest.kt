package com.github.jomof.nnsrs

import org.junit.Test
import java.lang.Math.abs

import java.sql.DriverManager
import com.sun.xml.internal.ws.streaming.XMLStreamReaderUtil.close
import org.tensorflow.Graph
import org.tensorflow.Session
import org.tensorflow.Tensor
import java.awt.SystemColor.window
import java.io.File


class NetworkTest {
    @Test
    fun trainLine() {
        val nodeCounts = arrayOf(1, 1)
        val network = Network.fromNodeCounts(nodeCounts)
        val m = 0.5
        val b = 0.1
        fun f(x : Double) = sigmoid(m * x + b)
        val inputs = listOf(-1.0, 1.0)
        val data = inputs.map { Pair(vectorOf(it), vectorOf(f(it))) }
        network.train(data, 1.0, 100, 1, reportBoolCost(network, data))
        val cost = cost(network, data)
        if (abs(cost) > 0.0001) throw RuntimeException()
    }

    @Test
    fun trainXor() {
        val nodeCounts = arrayOf(2, 7, 1)
        val network = Network.fromNodeCounts(nodeCounts)
        val inputs = listOf(
                vectorOf(0.0, 0.0),
                vectorOf(1.0, 0.0),
                vectorOf(0.0, 1.0),
                vectorOf(1.0, 1.0))
        val data = inputs.map { it ->
            val left = it[0] > 0
            val right = it[1] > 0
            if (left xor right) {
                Pair(it, vectorOf(0.66))
            } else {
                Pair(it, vectorOf(0.33))
            }
        }
        do {
            network.train(data, 1.0, 500, 2, reportBoolCost(network, data))
        } while (cost(network, data) > 0.00001)
        val cost = costBool(network, data)
        if (abs(cost) > 0.0001) throw RuntimeException()
    }

    @Test
    fun actorSimulated() {
        val nodeCounts = arrayOf(10, 22, 22, 1)
        val network = Network.fromNodeCounts(nodeCounts)
        val bigSample = inputsWindow(sampleDumbActorInteraction()).take(10000).toMutableList()
        fun doubleOf(boolean : Boolean) : Double = if (boolean) .66 else .33
        val inputs = bigSample.map { window ->
            val now = window[0].day
            val t1 = Math.log(now - window[1].day)
            val a1 = doubleOf(window[1].correct)
            val t2 = Math.log(now - window[2].day)
            val a2 = doubleOf(window[2].correct)
            val t3 = Math.log(now - window[3].day)
            val a3 = doubleOf(window[3].correct)
            val t4 = Math.log(now - window[4].day)
            val a4 = doubleOf(window[4].correct)
            val t5 = Math.log(now - window[5].day)
            val a5 = doubleOf(window[5].correct)
            val result = vectorOf(t1, a1, t2, a2, t3, a3, t4, a4, t5, a5)
            result
        }
        val answers = bigSample.map { window ->
            vectorOf(doubleOf(window[0].correct))
        }
        val data = inputs.zip(answers).toList()
        //val sample = data[2100]
        network.train(data, 1.0, 5000000, 40, reportBoolCost(network, data))
        val cost = costBool(network, data)
        println("Costbool = $cost")
    }

    data class Review(val id : Long, val cid : Long, val ease : Int)
    @Test
    fun anki() {
        val url = "jdbc:sqlite:/Users/jomof/Library/Application Support/Anki2/User 1/collection.anki2"
        val reviews = mutableListOf<Review>()
        DriverManager.getConnection(url).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("select * from revlog order by cid, id desc").use { result ->
                    while (result.next()) {
                        reviews += Review(
                                result.getLong("id"),
                                result.getLong("cid"),
                                result.getInt("ease"))
                    }
                }
            }
        }

        val data = reviews.asSequence().windowed(3)
                .filter{ it.map { it.cid }.toSet().size == 1 }
                .map { reviews ->
                    val now = reviews[0].id
                    fun ans(ease : Int) = ease.toDouble() / 5.0
                    fun time(id : Long) = Math.log((now - id) / 1000.0 / 60.0 / 60.0)
                    val result = DoubleArray(reviews.size * 2 - 2) { i ->
                        when {
                            i % 2 == 0 -> ans(reviews[(i + 2) / 2].ease)
                            i % 2 == 1 -> time(reviews[(i + 2)  / 2].id)
                            else -> throw RuntimeException()
                        }

                    }
                    Pair(Vector(result), vectorOf(ans(reviews[0].ease)))
                }
               // .take(1000)
                .toList()
        val nodeCounts = arrayOf(data[0].first.size, 9, data[0].second.size)
        val network = Network.fromNodeCounts(nodeCounts)
        network.train(data, 1.0, 500, 1, reportBoolCost(network, data))
        network.train(data, 1.0, 500, 2, reportBoolCost(network, data))
        network.train(data, 1.0, 500, 3, reportBoolCost(network, data))
        network.train(data, 1.0, 500, 4, reportBoolCost(network, data))
        network.train(data, 1.0, 500, 5, reportBoolCost(network, data))
        network.train(data, 1.0, 500, 10, reportBoolCost(network, data))
        network.train(data, 1.0, 500, 20, reportBoolCost(network, data))
        network.train(data, 1.0, 500, 30, reportBoolCost(network, data))
        network.train(data, 1.0, 500000, 40, reportBoolCost(network, data))
    }

    @Test
    fun tensorFlow() {
        val url = "jdbc:sqlite:/Users/jomof/Library/Application Support/Anki2/User 1/collection.anki2"
        val reviews = mutableListOf<Review>()
        DriverManager.getConnection(url).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("select * from revlog order by cid, id desc").use { result ->
                    while (result.next()) {
                        reviews += Review(
                                result.getLong("id"),
                                result.getLong("cid"),
                                result.getInt("ease"))
                    }
                }
            }
        }

        fun ans(answer : Int) : Double = answer / 5.0
        val normalize = 3E10
        var max = 0.0
        val data = reviews
                .asSequence()
                .windowed(5)
                .filter{ it.map { it.cid }.toSet().size == 1 }
                .map { windows ->
                    val eases = windows.asSequence().drop(1).map { it -> ans(it.ease) }
                    val deltas = (1 until windows.size).map {
                        val result = (windows[it - 1].id - windows[it].id).toDouble()
                        max = Math.max(max, result)
                        result / normalize
                    }
                    val cid = (windows[0].cid)
                            .toString(radix = 2)
                            .padStart(42, '0')
                            .map { it.toString().toLong().toDouble() }
                    Pair(windows[0].ease, eases + deltas)
                }
                .toList()


        val infile = File("infile.csv")
        infile.writeText("")
        val outfile = File("outfile.csv")
        outfile.writeText("")
        data.onEach { (output, input) ->
            infile.appendText(input.map { it }.joinToString(",") + "\n")
            outfile.appendText("$output\n")
        }
        println(infile.absoluteFile)
        println(max)
    }
}