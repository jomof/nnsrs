package com.github.jomof.nnsrs

import com.google.gson.*
import org.joda.time.DateTime
import java.io.File
import java.io.InputStreamReader
import java.io.BufferedReader
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.net.URL
import com.google.gson.GsonBuilder
import org.joda.time.DateTimeZone


data class UserData(
        val id : String,
        val username : String,
        val level : Int,
        val maxLevelGrantedBySubscription : Int,
        val profileUrl : String,
        val startedAt : DateTime,
        val subscribed : Boolean,
        val currentVersionStartedAt : DateTime?
) {
    override fun toString() : String = gb.toJson(this)
}

data class User(
        val `object` : String,
        val url : String,
        val dataUpdatedAt : DateTime,
        val data : UserData
) {
    override fun toString() : String = gb.toJson(this)
}

data class Pages(
        val perPage : Int,
        val nextUrl : String?,
        val previousUrl : String?
) {
    override fun toString() : String = gb.toJson(this)
}

enum class SrsStageName(val text : String) {
    APPRENTICE_1("Apprentice I"),
    APPRENTICE_2("Apprentice II"),
    APPRENTICE_3("Apprentice III"),
    APPRENTICE_4("Apprentice IV"),
    GURU_1("Guru I"),
    GURU_2("Guru II"),
    MASTER("Master"),
    ENLIGHTENED("Enlightened"),
    BURNED("Burned"),
}

data class ReviewData(
        val createdAtt : DateTime,
        val assignmentId : Int,
        val subjectId : Int,
        val startingSrsStage : Int,
        val startingSrsStageName : SrsStageName,
        val endingSrsStage : Int,
        val endingSrsStageName : SrsStageName,
        val incorrectMeaningAnswers : Int,
        val incorrectReadingAnswers: Int
) {
    override fun toString() : String = gb.toJson(this)
}

data class Review(
        val id : Int,
        val `object` : String,
        val url : String,
        val dataUpdatedAt : DateTime,
        val data : ReviewData
) {
    override fun toString() : String = gb.toJson(this)
}

data class Reviews(
        val `object` : String,
        val url : String,
        val pages : Pages,
        val totalCount : Int,
        val dataUpdatedAt : DateTime,
        val data : List<Review>
) {
    override fun toString() : String = gb.toJson(this)
}
//val CompactReviewData(
//
//)

val key = File(File(System.getProperty("user.home")), "wanikani.txt").readText().trim('\n').trim('\r')

private fun request(url: String): String {
    val obj = URL("https://api.wanikani.com/v2/$url")
    val connection = obj.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.doOutput = true;
    connection.setRequestProperty("Content-Type", "application/json")
    connection.setRequestProperty("Authorization", "Bearer $key")

    BufferedReader(InputStreamReader(connection.inputStream)).use { inputBuffer ->
        val response = StringBuilder()
        var input = inputBuffer.readLine()

        while (input != null) {
            response.append(input)
            input = inputBuffer.readLine()

        }
        println("$response")
        return response.toString()
    }
}

class DateTimeDeserializer : JsonDeserializer<DateTime> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): DateTime {
        return DateTime(json!!.asString).withZone(DateTimeZone.UTC)
    }
}

class DateTimeSerializer : JsonSerializer<DateTime> {
    override fun serialize(src: DateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive("$src")
    }
}

class SrsStageNameDeserializer : JsonDeserializer<SrsStageName> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): SrsStageName {
        val result = SrsStageName.values().filter { it.text == json!!.asString }
        if (result.size != 1) throw RuntimeException("$json")
        return result.single()
    }
}

class SrsStageNameSerializer : JsonSerializer<SrsStageName> {
    override fun serialize(src: SrsStageName?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src!!.text)
    }
}
private val gb = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(DateTime::class.java, DateTimeDeserializer())
        .registerTypeAdapter(DateTime::class.java, DateTimeSerializer())
        .registerTypeAdapter(SrsStageName::class.java, SrsStageNameDeserializer())
        .registerTypeAdapter(SrsStageName::class.java, SrsStageNameSerializer())
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()!!

fun <T> deserialize(string : String, type : Class<T>) : T {
    return gb.fromJson(string, type)

}

fun wanikaniUser() = deserialize(request("user"), User::class.java)
fun wanikaniReviews() = deserialize(request("reviews"), Reviews::class.java)

fun printKey() {
    println(key)
}
