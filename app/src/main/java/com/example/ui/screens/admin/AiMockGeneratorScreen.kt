package com.example.ui.screens.admin

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MockExam
import com.example.data.model.Question
import com.example.data.repository.AiMockGeneratorRepository
import com.example.data.repository.MockRepository
import com.example.ui.theme.AirForceBlue
import com.example.ui.theme.ExamDangerRed
import com.example.ui.theme.ExamSuccessGreen
import com.example.ui.theme.ExamWarningOrange
import com.example.ui.theme.GoldWing
import com.example.ui.theme.NavyPrimary
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiMockGeneratorScreen(
    aiRepository: AiMockGeneratorRepository,
    mockRepository: MockRepository,
    onMockSaved: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var rawInputText by remember { mutableStateOf("") }
    var isExtracting by remember { mutableStateOf(false) }
    var extractionError by remember { mutableStateOf<String?>(null) }

    // Editable draft state
    var draftTitle by remember { mutableStateOf("") }
    var draftCategory by remember { mutableStateOf("Air Force Group X") }
    var draftDurationMinutes by remember { mutableIntStateOf(60) }
    val draftQuestions = remember { mutableStateListOf<Question>() }

    var isSaving by remember { mutableStateOf(false) }

    // Photo picker for OCR
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    isExtracting = true
                    extractionError = null
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (bitmap != null) {
                        val result = aiRepository.extractQuestionsFromImage(bitmap)
                        result.onSuccess { draft ->
                            draftTitle = draft.title
                            draftCategory = draft.category
                            draftDurationMinutes = draft.durationMinutes
                            draftQuestions.clear()
                            draftQuestions.addAll(draft.questions)
                        }.onFailure { err ->
                            extractionError = err.localizedMessage ?: "OCR extraction failed"
                        }
                    } else {
                        extractionError = "Could not decode question paper image."
                    }
                } catch (e: Exception) {
                    extractionError = e.localizedMessage ?: "Failed reading image"
                } finally {
                    isExtracting = false
                }
            }
        }
    }

    fun handleExtractFromText() {
        if (rawInputText.isBlank()) return
        scope.launch {
            isExtracting = true
            extractionError = null
            val result = aiRepository.extractQuestionsFromText(rawInputText)
            result.onSuccess { draft ->
                draftTitle = draft.title
                draftCategory = draft.category
                draftDurationMinutes = draft.durationMinutes
                draftQuestions.clear()
                draftQuestions.addAll(draft.questions)
            }.onFailure { err ->
                extractionError = err.localizedMessage ?: "Extraction failed"
            }
            isExtracting = false
        }
    }

    fun saveDraft(isPublished: Boolean) {
        if (draftTitle.isBlank() || draftQuestions.isEmpty() || isSaving) return
        scope.launch {
            isSaving = true
            try {
                val mock = MockExam(
                    id = "",
                    title = draftTitle,
                    description = "AI-generated exam approved by admin on ${java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())}.",
                    category = draftCategory,
                    durationMinutes = draftDurationMinutes,
                    totalMarks = draftQuestions.size.toDouble(),
                    positiveMark = 1.0,
                    negativeMark = 0.25,
                    isPublished = isPublished,
                    questionCount = draftQuestions.size,
                    questions = draftQuestions.map { it.toMap() },
                    createdBy = "admin"
                )
                mockRepository.saveMock(mock)
                isSaving = false
                onMockSaved()
            } catch (e: Exception) {
                extractionError = "Failed saving mock: ${e.message}"
                isSaving = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Mock Generator",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Extract from PDF, DOCX, HTML, Text & Images",
                            fontSize = 11.sp,
                            color = GoldWing
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyPrimary)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AirForceBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Document Input & Document Parser",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Paste text copied from a PDF, HTML test, or DOCX syllabus file. Or run OCR on a question paper picture.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick sample insert helper
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    rawInputText = """
                                        INDIAN AIR FORCE AGNIVEERVAYU MODEL TEST 2026
                                        Category: Air Force Group X
                                        Time: 45 Minutes | Marks: 30
                                        
                                        1. The dimensional formula for electric potential is:
                                        (A) [M L² T⁻³ A⁻¹]
                                        (B) [M L T⁻² A⁻¹]
                                        (C) [M L² T⁻² A]
                                        (D) [M⁻¹ L² T⁻³ A]
                                        Answer: A
                                        Explanation: Potential V = Work / Charge = [M L² T⁻²] / [A T] = [M L² T⁻³ A⁻¹].
                                        
                                        2. If log₂(x) + log₂(x - 2) = 3, what is the value of x?
                                        (A) 4
                                        (B) 2
                                        (C) -2
                                        (D) 8
                                        Answer: A
                                        Explanation: log₂(x(x - 2)) = 3 => x(x - 2) = 2³ = 8 => x² - 2x - 8 = 0 => (x - 4)(x + 2) = 0. Since x > 2, x = 4.
                                        
                                        3. Choose the antonym of 'BENEVOLENT':
                                        (A) Malevolent
                                        (B) Kind
                                        (C) Generous
                                        (D) Helpful
                                        Answer: A
                                        Explanation: Benevolent means well-meaning and kindly; malevolent means having or showing a wish to do evil.
                                    """.trimIndent()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("load_sample_exam_button")
                            ) {
                                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Insert Sample Exam", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("upload_ocr_image_button")
                            ) {
                                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Upload Question Image", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = rawInputText,
                            onValueChange = { rawInputText = it },
                            placeholder = { Text("Paste PDF text, questions, or exam document here...") },
                            minLines = 5,
                            maxLines = 10,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("raw_text_input_field")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (extractionError != null) {
                            Surface(
                                color = ExamDangerRed.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = extractionError ?: "",
                                    color = ExamDangerRed,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Button(
                            onClick = { handleExtractFromText() },
                            enabled = rawInputText.isNotBlank() && !isExtracting,
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("extract_mock_gemini_button")
                        ) {
                            if (isExtracting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Parsing Questions with Gemini AI...", fontSize = 13.sp)
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = GoldWing, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Extract & Generate Mock Test", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // Editable Preview Section
            if (draftQuestions.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Editable Test Preview (${draftQuestions.size} Questions)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Surface(
                                    color = ExamWarningOrange.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Admin Review Mode",
                                        color = ExamWarningOrange,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = draftTitle,
                                onValueChange = { draftTitle = it },
                                label = { Text("Mock Test Title") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = draftCategory,
                                    onValueChange = { draftCategory = it },
                                    label = { Text("Exam Category") },
                                    modifier = Modifier.weight(1.2f)
                                )
                                OutlinedTextField(
                                    value = "$draftDurationMinutes",
                                    onValueChange = { draftDurationMinutes = it.toIntOrNull() ?: 60 },
                                    label = { Text("Duration (Mins)") },
                                    modifier = Modifier.weight(0.8f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action buttons
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { saveDraft(isPublished = false) },
                                    enabled = !isSaving,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save Draft", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { saveDraft(isPublished = true) },
                                    enabled = !isSaving,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ExamSuccessGreen),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("publish_approved_mock_button")
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Approve & Publish", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Question Cards Editor
                itemsIndexed(draftQuestions) { index, question ->
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
                                    text = "Question ${index + 1} (${question.section})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                IconButton(
                                    onClick = { draftQuestions.removeAt(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = ExamDangerRed, modifier = Modifier.size(18.dp))
                                }
                            }

                            // Uncertainty indicator banner per requirements
                            if (question.isUncertain) {
                                Surface(
                                    color = Color(0xFFFFF3CD),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = ExamWarningOrange, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "⚠️ Uncertain answer in source — verify correct option below.",
                                            color = Color(0xFF856404),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = question.textEnglish,
                                onValueChange = { newText ->
                                    draftQuestions[index] = question.copy(textEnglish = newText)
                                },
                                label = { Text("Question (English)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Select Correct Option (A, B, C, D):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )

                            question.optionsEnglish.forEachIndexed { optIdx, optText ->
                                val isSelected = question.correctOptionIndex == optIdx
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            draftQuestions[index] = question.copy(correctOptionIndex = optIdx, isUncertain = false)
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = ExamSuccessGreen)
                                    )
                                    OutlinedTextField(
                                        value = optText,
                                        onValueChange = { updatedOpt ->
                                            val newOptions = question.optionsEnglish.toMutableList()
                                            newOptions[optIdx] = updatedOpt
                                            draftQuestions[index] = question.copy(optionsEnglish = newOptions)
                                        },
                                        label = { Text("Option ${('A' + optIdx)}") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = question.explanationEnglish,
                                onValueChange = { exp ->
                                    draftQuestions[index] = question.copy(explanationEnglish = exp)
                                },
                                label = { Text("Explanation") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
