package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.ExamResult
import com.example.data.model.LeaderboardEntry
import com.example.data.model.MockExam
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val TAG = "ExamRepository"

class ExamRepository(private val context: Context) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.applicationContext.getString(R.string.firestore_database_id)
    )

    suspend fun submitExam(
        mock: MockExam,
        userAnswers: Map<String, Int>, // questionId to selectedOptionIndex (-1 for unattempted)
        timeTakenSeconds: Int
    ): ExamResult = withContext(Dispatchers.IO) {
        val user = Firebase.auth.currentUser
            ?: throw IllegalStateException("User must be signed in to submit an exam.")
        val uid = user.uid

        val questions = mock.parsedQuestions()
        var correctCount = 0
        var incorrectCount = 0
        var unattemptedCount = 0

        for (q in questions) {
            val selected = userAnswers[q.id] ?: -1
            if (selected == -1) {
                unattemptedCount++
            } else if (selected == q.correctOptionIndex) {
                correctCount++
            } else {
                incorrectCount++
            }
        }

        val positive = mock.positiveMark
        val negative = mock.negativeMark
        val rawScore = (correctCount * positive) - (incorrectCount * negative)
        val finalScore = String.format("%.2f", rawScore.coerceAtLeast(0.0)).toDouble()

        val attemptedTotal = correctCount + incorrectCount
        val accuracy = if (attemptedTotal > 0) {
            String.format("%.1f", (correctCount.toDouble() / attemptedTotal) * 100.0).toDouble()
        } else {
            0.0
        }

        // Serialize answers for question review
        val answersJsonObject = JSONObject()
        userAnswers.forEach { (qId, optionIdx) ->
            answersJsonObject.put(qId, optionIdx)
        }
        val answersJson = answersJsonObject.toString()

        val resultDocRef = db.collection("users").document(uid).collection("results").document()
        val resultPayload = mutableMapOf<String, Any>(
            "userId" to uid,
            "mockId" to mock.id,
            "mockTitle" to mock.title,
            "category" to mock.category,
            "score" to finalScore,
            "totalMarks" to mock.totalMarks,
            "correctCount" to correctCount,
            "incorrectCount" to incorrectCount,
            "unattemptedCount" to unattemptedCount,
            "accuracy" to accuracy,
            "timeTakenSeconds" to timeTakenSeconds,
            "answersJson" to answersJson,
            "createdAt" to FieldValue.serverTimestamp()
        )
        resultDocRef.set(resultPayload).await()

        // Submit to All India Leaderboard
        val leaderboardDocRef = db.collection("leaderboard").document()
        val leaderboardPayload = mutableMapOf<String, Any>(
            "userId" to uid,
            "userName" to (user.displayName ?: "Aspirant"),
            "mockId" to mock.id,
            "mockTitle" to mock.title,
            "category" to mock.category,
            "score" to finalScore,
            "totalMarks" to mock.totalMarks,
            "timeTakenSeconds" to timeTakenSeconds,
            "createdAt" to FieldValue.serverTimestamp()
        )
        user.photoUrl?.toString()?.let { leaderboardPayload["userPhoto"] = it }
        leaderboardDocRef.set(leaderboardPayload).await()

        // Update user stats
        try {
            val userDocRef = db.collection("users").document(uid)
            val userSnap = userDocRef.get().await()
            val currentBest = (userSnap.get("bestScore") as? Number)?.toDouble() ?: 0.0
            val newBest = maxOf(currentBest, finalScore)

            userDocRef.update(
                mapOf(
                    "testsAttempted" to FieldValue.increment(1),
                    "bestScore" to newBest,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update user score stats: ${e.message}")
        }

        ExamResult(
            id = resultDocRef.id,
            userId = uid,
            mockId = mock.id,
            mockTitle = mock.title,
            category = mock.category,
            score = finalScore,
            totalMarks = mock.totalMarks,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            unattemptedCount = unattemptedCount,
            accuracy = accuracy,
            timeTakenSeconds = timeTakenSeconds,
            answersJson = answersJson
        )
    }

    fun observeUserResults(userId: String): Flow<List<ExamResult>> {
        return db.collection("users").document(userId).collection("results")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ExamResult::class.java)
            }
            .catch { error ->
                Log.e(TAG, "Error observing user results: ${error.message}", error)
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    fun observeLeaderboard(mockIdFilter: String? = null): Flow<List<LeaderboardEntry>> {
        val query = if (!mockIdFilter.isNullOrBlank()) {
            db.collection("leaderboard")
                .whereEqualTo("mockId", mockIdFilter)
                .orderBy("score", Query.Direction.DESCENDING)
                .orderBy("timeTakenSeconds", Query.Direction.ASCENDING)
                .limit(100)
        } else {
            db.collection("leaderboard")
                .orderBy("score", Query.Direction.DESCENDING)
                .orderBy("timeTakenSeconds", Query.Direction.ASCENDING)
                .limit(100)
        }

        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(LeaderboardEntry::class.java)
            }
            .catch { error ->
                Log.e(TAG, "Error observing leaderboard: ${error.message}", error)
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }
}
