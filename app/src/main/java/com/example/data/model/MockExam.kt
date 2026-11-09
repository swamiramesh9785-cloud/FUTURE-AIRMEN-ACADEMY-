package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

data class Question(
    val id: String = "",
    val number: Int = 1,
    val section: String = "General",
    val textEnglish: String = "",
    val textHindi: String = "",
    val optionsEnglish: List<String> = emptyList(),
    val optionsHindi: List<String> = emptyList(),
    val correctOptionIndex: Int = 0,
    val explanationEnglish: String = "",
    val explanationHindi: String = "",
    val marks: Double = 1.0,
    val negativeMarks: Double = 0.25,
    val isUncertain: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "id" to id,
            "number" to number,
            "section" to section,
            "textEnglish" to textEnglish,
            "textHindi" to textHindi,
            "optionsEnglish" to optionsEnglish,
            "optionsHindi" to optionsHindi,
            "correctOptionIndex" to correctOptionIndex,
            "explanationEnglish" to explanationEnglish,
            "explanationHindi" to explanationHindi,
            "marks" to marks,
            "negativeMarks" to negativeMarks,
            "isUncertain" to isUncertain
        )
        return map
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): Question {
            return Question(
                id = map["id"] as? String ?: "",
                number = (map["number"] as? Number)?.toInt() ?: 1,
                section = map["section"] as? String ?: "General",
                textEnglish = map["textEnglish"] as? String ?: "",
                textHindi = map["textHindi"] as? String ?: "",
                optionsEnglish = (map["optionsEnglish"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                optionsHindi = (map["optionsHindi"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                correctOptionIndex = (map["correctOptionIndex"] as? Number)?.toInt() ?: 0,
                explanationEnglish = map["explanationEnglish"] as? String ?: "",
                explanationHindi = map["explanationHindi"] as? String ?: "",
                marks = (map["marks"] as? Number)?.toDouble() ?: 1.0,
                negativeMarks = (map["negativeMarks"] as? Number)?.toDouble() ?: 0.25,
                isUncertain = map["isUncertain"] as? Boolean ?: false
            )
        }
    }
}

@IgnoreExtraProperties
data class MockExam(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Air Force Group X",
    val durationMinutes: Int = 60,
    val totalMarks: Double = 70.0,
    val positiveMark: Double = 1.0,
    val negativeMark: Double = 0.25,
    val isPublished: Boolean = true,
    val questionCount: Int = 0,
    val questions: List<Map<String, Any>> = emptyList(),
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedQuestions(): List<Question> {
        return questions.map { Question.fromMap(it) }
    }
}
