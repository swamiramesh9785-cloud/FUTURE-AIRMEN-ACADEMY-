package com.example.ui.screens.exam

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.QueryBuilder
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExamResult
import com.example.data.model.MockExam
import com.example.data.model.Question
import com.example.ui.components.DefenceHeader
import com.example.ui.theme.AirForceBlue
import com.example.ui.theme.ExamDangerRed
import com.example.ui.theme.ExamSuccessGreen
import com.example.ui.theme.ExamUnvisitedGrey
import com.example.ui.theme.GoldWing
import com.example.ui.theme.NavyPrimary
import org.json.JSONObject

@Composable
fun ResultReviewScreen(
    result: ExamResult,
    mock: MockExam,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val questions = remember { mock.parsedQuestions() }
    val userAnswers = remember {
        val map = mutableMapOf<String, Int>()
        if (result.answersJson.isNotBlank()) {
            try {
                val json = JSONObject(result.answersJson)
                json.keys().forEach { key ->
                    map[key] = json.getInt(key)
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
        map
    }

    var selectedFilter by remember { mutableStateOf("All") }
    var isHindiLanguage by remember { mutableStateOf(false) }

    val filteredQuestions = questions.filter { q ->
        val selected = userAnswers[q.id] ?: -1
        when (selectedFilter) {
            "Correct" -> selected == q.correctOptionIndex
            "Incorrect" -> selected != -1 && selected != q.correctOptionIndex
            "Unattempted" -> selected == -1
            else -> true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DefenceHeader(isAdmin = false)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                // Score Overview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = result.mockTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "CATEGORY: ${result.category}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldWing,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large Score Pill
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${result.score}",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = GoldWing
                            )
                            Text(
                                text = " / ${result.totalMarks.toInt()} Marks",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD6E4F0),
                                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Performance metrics row
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ScoreMetric(
                                label = "Accuracy",
                                value = "${result.accuracy}%",
                                color = GoldWing
                            )
                            val minutes = result.timeTakenSeconds / 60
                            val seconds = result.timeTakenSeconds % 60
                            ScoreMetric(
                                label = "Time Taken",
                                value = "${minutes}m ${seconds}s",
                                color = Color.White
                            )
                            ScoreMetric(
                                label = "Questions",
                                value = "${questions.size}",
                                color = Color(0xFFB0C4DE)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Attempt counts bar
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ResultPill(
                                count = result.correctCount,
                                label = "Correct",
                                color = ExamSuccessGreen,
                                modifier = Modifier.weight(1f)
                            )
                            ResultPill(
                                count = result.incorrectCount,
                                label = "Incorrect",
                                color = ExamDangerRed,
                                modifier = Modifier.weight(1f)
                            )
                            ResultPill(
                                count = result.unattemptedCount,
                                label = "Unattempted",
                                color = ExamUnvisitedGrey,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // CTA Navigation Buttons
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onNavigateToLeaderboard,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D5D9B)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("view_leaderboard_from_results_button")
                    ) {
                        Icon(imageVector = Icons.Default.Leaderboard, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("All-India Rank", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onNavigateToDashboard,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("back_to_dashboard_button")
                    ) {
                        Icon(imageVector = Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dashboard", fontSize = 12.sp)
                    }
                }
            }

            // Question Review Filter and Language Switcher
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Detailed Solutions & Explanations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Surface(
                        color = if (isHindiLanguage) GoldWing else Color(0xFFEEF2F6),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { isHindiLanguage = !isHindiLanguage }
                    ) {
                        Text(
                            text = if (isHindiLanguage) "हिन्दी" else "EN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isHindiLanguage) NavyPrimary else Color(0xFF334E68),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Review Filter Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("All", "Correct", "Incorrect", "Unattempted").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }
            }

            // Questions list
            itemsIndexed(filteredQuestions, key = { _, q -> q.id }) { _, question ->
                QuestionReviewCard(
                    question = question,
                    selectedOption = userAnswers[question.id] ?: -1,
                    isHindi = isHindiLanguage,
                    positiveMark = mock.positiveMark,
                    negativeMark = mock.negativeMark
                )
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun ScoreMetric(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFFB0C4DE)
        )
    }
}

@Composable
private fun ResultPill(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(
                text = "$count",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun QuestionReviewCard(
    question: Question,
    selectedOption: Int,
    isHindi: Boolean,
    positiveMark: Double,
    negativeMark: Double
) {
    val isCorrect = selectedOption == question.correctOptionIndex
    val isUnattempted = selectedOption == -1

    val (statusLabel, statusColor) = when {
        isCorrect -> "CORRECT (+${positiveMark})" to ExamSuccessGreen
        isUnattempted -> "UNATTEMPTED (0.0)" to ExamUnvisitedGrey
        else -> "INCORRECT (-${negativeMark})" to ExamDangerRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Q.${question.number} • ${question.section}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val questionText = if (isHindi && question.textHindi.isNotBlank()) question.textHindi else question.textEnglish
            Text(
                text = questionText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Options with highlighting
            val options = if (isHindi && question.optionsHindi.isNotEmpty()) question.optionsHindi else question.optionsEnglish

            options.forEachIndexed { idx, optText ->
                val isAnswerSelectedByStudent = selectedOption == idx
                val isTheCorrectOption = question.correctOptionIndex == idx

                val (borderColor, bgColor, icon) = when {
                    isTheCorrectOption -> Triple(ExamSuccessGreen, ExamSuccessGreen.copy(alpha = 0.12f), Icons.Default.Check)
                    isAnswerSelectedByStudent && !isCorrect -> Triple(ExamDangerRed, ExamDangerRed.copy(alpha = 0.12f), Icons.Default.Close)
                    else -> Triple(Color(0xFFE2E8F0), Color.Transparent, null)
                }

                Surface(
                    color = bgColor,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Surface(
                            color = if (isTheCorrectOption) ExamSuccessGreen else if (isAnswerSelectedByStudent) ExamDangerRed else Color(0xFFE2E8F0),
                            shape = CircleShape,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = ('A' + idx).toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTheCorrectOption || isAnswerSelectedByStudent) Color.White else Color(0xFF2C3E50)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = optText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isTheCorrectOption) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isTheCorrectOption) ExamSuccessGreen else ExamDangerRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Explanation Section
            val explanation = if (isHindi && question.explanationHindi.isNotBlank()) question.explanationHindi else question.explanationEnglish
            if (explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFFFFF9E6),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp)) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFD4AC0D),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "EXPLANATION:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color(0xFF7D6608)
                            )
                            Text(
                                text = explanation,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = Color(0xFF2C3E50)
                            )
                        }
                    }
                }
            }
        }
    }
}
