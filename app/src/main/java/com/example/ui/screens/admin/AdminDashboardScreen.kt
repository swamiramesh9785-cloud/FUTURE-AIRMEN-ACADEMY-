package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.data.model.UserProfile
import com.example.data.repository.MockRepository
import com.example.ui.components.DefenceHeader
import com.example.ui.theme.ExamDangerRed
import com.example.ui.theme.ExamSuccessGreen
import com.example.ui.theme.GoldWing
import com.example.ui.theme.NavyPrimary
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(
    userProfile: UserProfile?,
    mockRepository: MockRepository,
    onNavigateToAiGenerator: () -> Unit,
    onNavigateToCreateManual: () -> Unit,
    onEditMock: (MockExam) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val allMocks by mockRepository.observeAllMocksForAdmin().collectAsState(initial = emptyList())
    var mockToDelete by remember { mutableStateOf<MockExam?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DefenceHeader(isAdmin = true)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))

                // Admin Security Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = GoldWing,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "ADMINISTRATOR CONSOLE",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        letterSpacing = 1.sp,
                                        color = GoldWing
                                    )
                                    Text(
                                        text = "swamiramesh9785@gmail.com",
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            Surface(
                                color = ExamSuccessGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = ExamSuccessGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "VERIFIED",
                                        color = ExamSuccessGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats Summary
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val publishedCount = allMocks.count { it.isPublished }
                            val draftsCount = allMocks.size - publishedCount

                            AdminMetric(label = "Total Tests", value = "${allMocks.size}")
                            AdminMetric(label = "Published", value = "$publishedCount", color = ExamSuccessGreen)
                            AdminMetric(label = "Drafts", value = "$draftsCount", color = GoldWing)
                        }
                    }
                }
            }

            // Action Buttons Section
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // AI Mock Generator Button
                    Button(
                        onClick = onNavigateToAiGenerator,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1D5D9B),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("ai_mock_generator_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GoldWing,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("AI Mock Generator", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("PDF / DOCX / OCR", fontSize = 10.sp, color = GoldWing)
                        }
                    }

                    // Manual Mock Creation Button
                    OutlinedButton(
                        onClick = onNavigateToCreateManual,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("create_mock_manual_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Create Manually", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Custom questions", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Mocks Management Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Manage Examination Mocks (${allMocks.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (allMocks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "No mock tests configured yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Use the AI Mock Generator to import from PDF/HTML or create tests manually.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(allMocks, key = { it.id }) { mock ->
                    AdminMockCard(
                        mock = mock,
                        onTogglePublish = {
                            scope.launch {
                                mockRepository.togglePublishStatus(mock.id, mock.isPublished)
                            }
                        },
                        onEdit = { onEditMock(mock) },
                        onDelete = { mockToDelete = mock }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    mockToDelete?.let { mock ->
        AlertDialog(
            onDismissRequest = { mockToDelete = null },
            title = { Text("Delete Mock Test?") },
            text = {
                Text("Are you sure you want to permanently delete '${mock.title}'? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            mockRepository.deleteMock(mock.id)
                            mockToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExamDangerRed)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { mockToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AdminMetric(
    label: String,
    value: String,
    color: Color = Color.White
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
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
private fun AdminMockCard(
    mock: MockExam,
    onTogglePublish: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_mock_row_${mock.id}"),
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
                Surface(
                    color = NavyPrimary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = mock.category,
                        color = NavyPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Published Status Toggle Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (mock.isPublished) "Published" else "Draft",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (mock.isPublished) ExamSuccessGreen else Color(0xFF7F8C8D)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = mock.isPublished,
                        onCheckedChange = { onTogglePublish() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ExamSuccessGreen
                        ),
                        modifier = Modifier.testTag("publish_switch_${mock.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = mock.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "${mock.questions.size} Questions • ${mock.durationMinutes} Mins • ${mock.totalMarks.toInt()} Marks • -${mock.negativeMark} Neg",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_mock_${mock.id}")) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = NavyPrimary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_mock_${mock.id}")) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = ExamDangerRed)
                }
            }
        }
    }
}
