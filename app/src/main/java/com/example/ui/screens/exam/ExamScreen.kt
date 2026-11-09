package com.example.ui.screens.exam

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.QueryBuilder
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.repository.ExamRepository
import com.example.ui.components.ExamPaletteSheet
import com.example.ui.theme.AirForceBlue
import com.example.ui.theme.ExamDangerRed
import com.example.ui.theme.ExamReviewPurple
import com.example.ui.theme.ExamSuccessGreen
import com.example.ui.theme.ExamWarningOrange
import com.example.ui.theme.GoldWing
import com.example.ui.theme.NavyPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    mock: MockExam,
    examRepository: ExamRepository,
    onExamCompleted: (ExamResult) -> Unit,
    onCancelExam: () -> Unit
) {
    val questions = remember { mock.parsedQuestions() }
    val scope = rememberCoroutineScope()

    var currentIndex by remember { mutableIntStateOf(0) }
    val userAnswers = remember { mutableStateMapOf<String, Int>() }
    val markedForReview = remember { mutableStateOf(setOf<String>()) }
    var isHindiLanguage by remember { mutableStateOf(false) }

    var isPaletteOpen by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }
    var showExitWarningDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Countdown timer logic
    val initialSeconds = remember { mock.durationMinutes * 60 }
    var remainingSeconds by remember { mutableIntStateOf(initialSeconds) }

    fun doSubmit() {
        if (isSubmitting) return
        isSubmitting = true
        scope.launch {
            val timeTaken = (initialSeconds - remainingSeconds).coerceAtLeast(1)
            val result = examRepository.submitExam(
                mock = mock,
                userAnswers = userAnswers.toMap(),
                timeTakenSeconds = timeTaken
            )
            isSubmitting = false
            onExamCompleted(result)
        }
    }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        }
        // Auto-submit when time expires
        doSubmit()
    }

    BackHandler {
        showExitWarningDialog = true
    }

    val currentQuestion = questions.getOrNull(currentIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = mock.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Q ${currentIndex + 1} of ${questions.size}",
                                fontSize = 11.sp,
                                color = GoldWing
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• ${currentQuestion?.section ?: "General"}",
                                fontSize = 11.sp,
                                color = Color(0xFFB0C4DE)
                            )
                        }
                    }
                },
                actions = {
                    // Language Switcher Toggle
                    IconButton(
                        onClick = { isHindiLanguage = !isHindiLanguage },
                        modifier = Modifier.testTag("lang_toggle_button")
                    ) {
                        Surface(
                            color = if (isHindiLanguage) GoldWing else Color(0x33FFFFFF),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isHindiLanguage) "हिन्दी" else "EN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isHindiLanguage) NavyPrimary else Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Countdown Timer Display
                    val minutes = remainingSeconds / 60
                    val seconds = remainingSeconds % 60
                    val timerColor = when {
                        remainingSeconds < 300 -> ExamDangerRed
                        remainingSeconds < 600 -> ExamWarningOrange
                        else -> GoldWing
                    }

                    Surface(
                        color = Color(0x22000000),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueryBuilder,
                                contentDescription = null,
                                tint = timerColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%02d:%02d", minutes, seconds),
                                color = timerColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Question Palette Toggle Button
                    IconButton(
                        onClick = { isPaletteOpen = true },
                        modifier = Modifier.testTag("open_palette_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Palette",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyPrimary)
            )
        },
        bottomBar = {
            // Exam Bottom Action Controls
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Previous Button
                        OutlinedButton(
                            onClick = {
                                if (currentIndex > 0) currentIndex--
                            },
                            enabled = currentIndex > 0,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("prev_question_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Prev", fontSize = 12.sp)
                        }

                        // Clear Response Button
                        TextButton(
                            onClick = {
                                currentQuestion?.let { userAnswers.remove(it.id) }
                            },
                            enabled = currentQuestion != null && userAnswers.containsKey(currentQuestion.id),
                            modifier = Modifier.testTag("clear_response_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Clear", fontSize = 12.sp)
                        }

                        // Mark for Review Toggle
                        val isMarked = currentQuestion != null && markedForReview.value.contains(currentQuestion.id)
                        OutlinedButton(
                            onClick = {
                                if (currentQuestion != null) {
                                    val currentSet = markedForReview.value.toMutableSet()
                                    if (isMarked) {
                                        currentSet.remove(currentQuestion.id)
                                    } else {
                                        currentSet.add(currentQuestion.id)
                                    }
                                    markedForReview.value = currentSet
                                    // Move to next after marking
                                    if (currentIndex < questions.size - 1) {
                                        currentIndex++
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isMarked) ExamReviewPurple else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.testTag("mark_review_button")
                        ) {
                            Icon(
                                imageVector = if (isMarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isMarked) ExamReviewPurple else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isMarked) "Marked" else "Review",
                                fontSize = 12.sp
                            )
                        }

                        // Next or Submit Button
                        if (currentIndex < questions.size - 1) {
                            Button(
                                onClick = { currentIndex++ },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                modifier = Modifier.testTag("next_question_button")
                            ) {
                                Text("Next", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = { showSubmitDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ExamSuccessGreen),
                                modifier = Modifier.testTag("submit_exam_trigger_button")
                            ) {
                                Text("Submit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Secondary full Submit CTA
                    Button(
                        onClick = { showSubmitDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E3A5F),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("bottom_submit_all_button")
                    ) {
                        Text("Finish & Submit Examination", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (currentQuestion != null) {
                // Section & Marks Ribbon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = NavyPrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Section: ${currentQuestion.section}",
                            color = NavyPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "+${mock.positiveMark} / -${mock.negativeMark} Marks",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Question Statement Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val questionText = if (isHindiLanguage && currentQuestion.textHindi.isNotBlank()) {
                            currentQuestion.textHindi
                        } else {
                            currentQuestion.textEnglish
                        }

                        Text(
                            text = "Q.${currentQuestion.number} $questionText",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Secondary language preview if available
                        val secondaryText = if (isHindiLanguage) currentQuestion.textEnglish else currentQuestion.textHindi
                        if (secondaryText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = secondaryText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4 Options A, B, C, D
                val options = if (isHindiLanguage && currentQuestion.optionsHindi.isNotEmpty()) {
                    currentQuestion.optionsHindi
                } else {
                    currentQuestion.optionsEnglish
                }

                val selectedOption = userAnswers[currentQuestion.id]

                options.forEachIndexed { optIndex, optionText ->
                    val isSelected = selectedOption == optIndex
                    val optLetter = ('A' + optIndex).toString()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                userAnswers[currentQuestion.id] = optIndex
                            }
                            .testTag("option_${currentQuestion.id}_$optLetter"),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) NavyPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) {
                            CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyPrimary), width = 2.dp)
                        } else {
                            CardDefaults.outlinedCardBorder()
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Surface(
                                color = if (isSelected) NavyPrimary else Color(0xFFEEF2F6),
                                shape = CircleShape,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = optLetter,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.White else NavyPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = optionText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            RadioButton(
                                selected = isSelected,
                                onClick = { userAnswers[currentQuestion.id] = optIndex },
                                colors = RadioButtonDefaults.colors(selectedColor = NavyPrimary)
                            )
                        }
                    }
                }
            }
        }
    }

    // Question Palette Bottom Sheet
    if (isPaletteOpen) {
        ExamPaletteSheet(
            questions = questions,
            currentIndex = currentIndex,
            userAnswers = userAnswers.toMap(),
            markedForReview = markedForReview.value,
            onSelectQuestion = { selectedIdx ->
                currentIndex = selectedIdx
            },
            onDismiss = { isPaletteOpen = false }
        )
    }

    // Submit Confirmation Dialog
    if (showSubmitDialog) {
        val answeredCount = questions.count { userAnswers.containsKey(it.id) && userAnswers[it.id] != -1 }
        val reviewCount = questions.count { markedForReview.value.contains(it.id) }
        val unattemptedCount = questions.size - answeredCount

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showSubmitDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ExamWarningOrange,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Submit Examination?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Review your attempt summary before final submission:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Total Questions:", fontWeight = FontWeight.SemiBold)
                        Text("${questions.size}", fontWeight = FontWeight.Bold)
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Answered:", color = ExamSuccessGreen)
                        Text("$answeredCount", color = ExamSuccessGreen, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Unanswered:", color = ExamDangerRed)
                        Text("$unattemptedCount", color = ExamDangerRed, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Marked for Review:", color = ExamReviewPurple)
                        Text("$reviewCount", color = ExamReviewPurple, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitDialog = false
                        doSubmit()
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = ExamSuccessGreen),
                    modifier = Modifier.testTag("confirm_submit_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Yes, Submit Exam", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSubmitDialog = false },
                    enabled = !isSubmitting
                ) {
                    Text("Resume Exam")
                }
            }
        )
    }

    // Exit Warning Dialog
    if (showExitWarningDialog) {
        AlertDialog(
            onDismissRequest = { showExitWarningDialog = false },
            title = { Text("Quit Examination?") },
            text = {
                Text("Your current exam progress will be lost if you leave without submitting.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitWarningDialog = false
                        onCancelExam()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExamDangerRed)
                ) {
                    Text("Quit Exam")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitWarningDialog = false }) {
                    Text("Continue Exam")
                }
            }
        )
    }
}
