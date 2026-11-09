package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ExamResult(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mockId: String = "",
    val mockTitle: String = "",
    val category: String = "",
    val score: Double = 0.0,
    val totalMarks: Double = 0.0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val unattemptedCount: Int = 0,
    val accuracy: Double = 0.0,
    val timeTakenSeconds: Int = 0,
    val answersJson: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhoto: String? = null,
    val mockId: String = "",
    val mockTitle: String = "",
    val category: String = "",
    val score: Double = 0.0,
    val totalMarks: Double = 0.0,
    val timeTakenSeconds: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null
)
