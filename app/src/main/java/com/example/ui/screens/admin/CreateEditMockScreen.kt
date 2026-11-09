package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MockExam
import com.example.data.model.Question
import com.example.data.repository.MockRepository
import com.example.ui.theme.ExamDangerRed
import com.example.ui.theme.ExamSuccessGreen
import com.example.ui.theme.GoldWing
import com.example.ui.theme.NavyPrimary
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditMockScreen(
    initialMock: MockExam?,
    mockRepository: MockRepository,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(initialMock?.title ?: "") }
    var description by remember { mutableStateOf(initialMock?.description ?: "") }
    var category by remember { mutableStateOf(initialMock?.category ?: "Air Force Group X") }
    var durationMinutes by remember { mutableIntStateOf(initialMock?.durationMinutes ?: 60) }
    var positiveMark by remember { mutableDoubleStateOf(initialMock?.positiveMark ?: 1.0) }
    var negativeMark by remember { mutableDoubleStateOf(initialMock?.negativeMark ?: 0.25) }

    val questions = remember {
        val list = mutableStateListOf<Question>()
        if (initialMock != null) {
            list.addAll(initialMock.parsedQuestions())
        }
        list
    }

    var isSaving by remember { mutableStateOf(false) }

    fun save(isPublished: Boolean) {
        if (title.isBlank() || isSaving) return
        scope.launch {
            isSaving = true
            val mockToSave = MockExam(
                id = initialMock?.id ?: "",
                title = title,
                description = description,
                category = category,
                durationMinutes = durationMinutes,
                totalMarks = if (questions.isNotEmpty()) questions.size * positiveMark else 50.0,
                positiveMark = positiveMark,
                negativeMark = negativeMark,
                isPublished = isPublished,
                questionCount = questions.size,
                questions = questions.map { it.toMap() },
                createdBy = initialMock?.createdBy ?: "admin"
            )
            mockRepository.saveMock(mockToSave)
            isSaving = false
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialMock != null) "Edit Mock Exam" else "Create Mock Exam",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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
            // General Details
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Exam Specifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title (e.g. IAF Group X Physics Full Mock 02)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description / Syllabus") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = { category = it },
                                label = { Text("Category") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = "$durationMinutes",
                                onValueChange = { durationMinutes = it.toIntOrNull() ?: 60 },
                                label = { Text("Duration (Mins)") },
                                modifier = Modifier.weight(0.6f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = "$positiveMark",
                                onValueChange = { positiveMark = it.toDoubleOrNull() ?: 1.0 },
                                label = { Text("+ Mark") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = "$negativeMark",
                                onValueChange = { negativeMark = it.toDoubleOrNull() ?: 0.25 },
                                label = { Text("- Negative") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Questions Section Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Questions (${questions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Button(
                        onClick = {
                            questions.add(
                                Question(
                                    id = UUID.randomUUID().toString().take(8),
                                    number = questions.size + 1,
                                    section = "General",
                                    textEnglish = "Enter question statement here...",
                                    optionsEnglish = listOf("Option A", "Option B", "Option C", "Option D"),
                                    optionsHindi = listOf("विकल्प A", "विकल्प B", "विकल्प C", "विकल्प D"),
                                    correctOptionIndex = 0,
                                    marks = positiveMark,
                                    negativeMarks = negativeMark
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Question", fontSize = 12.sp)
                    }
                }
            }

            // Questions List
            itemsIndexed(questions) { index, question ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Q.${index + 1}",
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                            IconButton(onClick = { questions.removeAt(index) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = ExamDangerRed)
                            }
                        }

                        OutlinedTextField(
                            value = question.textEnglish,
                            onValueChange = { questions[index] = question.copy(textEnglish = it) },
                            label = { Text("Question Text (English)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Options (Click radio to select correct answer):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        question.optionsEnglish.forEachIndexed { optIdx, optText ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                RadioButton(
                                    selected = question.correctOptionIndex == optIdx,
                                    onClick = { questions[index] = question.copy(correctOptionIndex = optIdx) },
                                    colors = RadioButtonDefaults.colors(selectedColor = ExamSuccessGreen)
                                )
                                OutlinedTextField(
                                    value = optText,
                                    onValueChange = { newOpt ->
                                        val mutableOpts = question.optionsEnglish.toMutableList()
                                        mutableOpts[optIdx] = newOpt
                                        questions[index] = question.copy(optionsEnglish = mutableOpts)
                                    },
                                    label = { Text("Option ${('A' + optIdx)}") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = question.explanationEnglish,
                            onValueChange = { questions[index] = question.copy(explanationEnglish = it) },
                            label = { Text("Explanation (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Save Buttons
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { save(isPublished = false) },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save as Draft")
                    }

                    Button(
                        onClick = { save(isPublished = true) },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ExamSuccessGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Publish Exam", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
