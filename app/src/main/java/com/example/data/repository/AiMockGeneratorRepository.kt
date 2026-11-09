package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.Question
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

private const val TAG = "AiMockGenerator"

data class ExtractedMockDraft(
    val title: String,
    val category: String,
    val durationMinutes: Int,
    val totalMarks: Double,
    val questions: List<Question>
)

class AiMockGeneratorRepository {

    private val questionItemSchema = Schema.obj(
        properties = mapOf(
            "number" to Schema.integer(description = "Question number starting from 1"),
            "section" to Schema.string(description = "Subject section like Physics, Mathematics, English, Reasoning, General Awareness"),
            "textEnglish" to Schema.string(description = "Question text in English"),
            "textHindi" to Schema.string(description = "Question text in Hindi if available or translated"),
            "optionsEnglish" to Schema.array(items = Schema.string(), description = "Exactly 4 options in English [A, B, C, D]"),
            "optionsHindi" to Schema.array(items = Schema.string(), description = "Exactly 4 options in Hindi [A, B, C, D]"),
            "correctOptionIndex" to Schema.integer(description = "0 for A, 1 for B, 2 for C, 3 for D"),
            "explanationEnglish" to Schema.string(description = "Detailed rational step-by-step explanation in English"),
            "explanationHindi" to Schema.string(description = "Detailed explanation in Hindi"),
            "isUncertain" to Schema.boolean(description = "True if the answer key or options are doubtful, ambiguous or missing in the source document")
        )
    )

    private val mockExtractionSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Title for the mock exam"),
            "category" to Schema.string(description = "Category: Air Force Group X, Air Force Group Y, Air Force X + Y, Indian Navy, Indian Coast Guard, etc."),
            "durationMinutes" to Schema.integer(description = "Suggested duration in minutes"),
            "totalMarks" to Schema.double(description = "Suggested total marks"),
            "questions" to Schema.array(items = questionItemSchema, description = "List of extracted questions")
        )
    )

    private val config = generationConfig {
        responseMimeType = "application/json"
        responseSchema = mockExtractionSchema
    }

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    suspend fun extractQuestionsFromText(rawText: String): Result<ExtractedMockDraft> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                You are an expert Indian Defence examination mock paper parser for FUTURE AIRMEN.
                Extract questions, options (A, B, C, D), answer keys, and step-by-step explanations from this test document or text.
                Bilingual support: provide both English and Hindi where possible.
                CRITICAL INSTRUCTION: If an answer key is unclear, doubtful, or not provided in the source text, set "isUncertain": true so the admin can review it.
                
                Document Content:
                $rawText
            """.trimIndent()

            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }

            val jsonString = response.text ?: throw IllegalStateException("Model returned empty text.")
            Result.success(parseJsonToDraft(jsonString))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed extracting questions: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun extractQuestionsFromImage(bitmap: Bitmap): Result<ExtractedMockDraft> = withContext(Dispatchers.IO) {
        try {
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        text(
                            """
                            Perform OCR and extract questions, options (A, B, C, D), answer keys, and explanations from this exam question paper image.
                            Support Indian Air Force, Navy, and Coast Guard format.
                            Flag any ambiguous or guessed questions with "isUncertain": true.
                            """.trimIndent()
                        )
                        image(bitmap)
                    }
                )
            }

            val jsonString = response.text ?: throw IllegalStateException("Model returned empty text from image OCR.")
            Result.success(parseJsonToDraft(jsonString))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed OCR extraction from image: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseJsonToDraft(jsonString: String): ExtractedMockDraft {
        val json = JSONObject(jsonString)
        val title = json.optString("title", "Defence Mock Test")
        val category = json.optString("category", "Air Force Group X")
        val durationMinutes = json.optInt("durationMinutes", 60)
        val totalMarks = json.optDouble("totalMarks", 50.0)

        val questionsArray = json.optJSONArray("questions")
        val questionList = mutableListOf<Question>()

        if (questionsArray != null) {
            for (i in 0 until questionsArray.length()) {
                val qObj = questionsArray.getJSONObject(i)
                val optEngArray = qObj.optJSONArray("optionsEnglish")
                val optEng = mutableListOf<String>()
                if (optEngArray != null) {
                    for (j in 0 until optEngArray.length()) {
                        optEng.add(optEngArray.getString(j))
                    }
                }
                while (optEng.size < 4) {
                    optEng.add("Option ${('A' + optEng.size)}")
                }

                val optHinArray = qObj.optJSONArray("optionsHindi")
                val optHin = mutableListOf<String>()
                if (optHinArray != null) {
                    for (j in 0 until optHinArray.length()) {
                        optHin.add(optHinArray.getString(j))
                    }
                }
                while (optHin.size < 4) {
                    optHin.add(optEng.getOrElse(optHin.size) { "विकल्प" })
                }

                questionList.add(
                    Question(
                        id = UUID.randomUUID().toString().take(8),
                        number = qObj.optInt("number", i + 1),
                        section = qObj.optString("section", "General"),
                        textEnglish = qObj.optString("textEnglish", "Question text"),
                        textHindi = qObj.optString("textHindi", ""),
                        optionsEnglish = optEng,
                        optionsHindi = optHin,
                        correctOptionIndex = qObj.optInt("correctOptionIndex", 0).coerceIn(0, 3),
                        explanationEnglish = qObj.optString("explanationEnglish", ""),
                        explanationHindi = qObj.optString("explanationHindi", ""),
                        marks = 1.0,
                        negativeMarks = 0.25,
                        isUncertain = qObj.optBoolean("isUncertain", false)
                    )
                )
            }
        }

        return ExtractedMockDraft(
            title = title,
            category = category,
            durationMinutes = durationMinutes,
            totalMarks = if (questionList.isNotEmpty()) questionList.size.toDouble() else totalMarks,
            questions = questionList
        )
    }
}
