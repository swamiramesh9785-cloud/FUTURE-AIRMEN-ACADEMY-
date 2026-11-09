package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Question
import com.example.ui.theme.AirForceBlue
import com.example.ui.theme.ExamReviewPurple
import com.example.ui.theme.ExamSuccessGreen
import com.example.ui.theme.ExamUnvisitedGrey
import com.example.ui.theme.NavyPrimary

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ExamPaletteSheet(
    questions: List<Question>,
    currentIndex: Int,
    userAnswers: Map<String, Int>,
    markedForReview: Set<String>,
    onSelectQuestion: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val answeredCount = questions.count { userAnswers.containsKey(it.id) && userAnswers[it.id] != -1 }
    val reviewCount = questions.count { markedForReview.contains(it.id) }
    val unattemptedCount = questions.size - answeredCount

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Question Palette",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_palette_button")) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Palette")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend indicators
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                StatusBadge(color = ExamSuccessGreen, label = "Answered", count = answeredCount)
                StatusBadge(color = ExamReviewPurple, label = "Review", count = reviewCount)
                StatusBadge(color = ExamUnvisitedGrey, label = "Unanswered", count = unattemptedCount)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = DividerDefaults.color
            )

            Text(
                text = "Tap any question to jump directly:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Grid of Question Circles
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                questions.forEachIndexed { index, question ->
                    val isAnswered = userAnswers.containsKey(question.id) && userAnswers[question.id] != -1
                    val isReview = markedForReview.contains(question.id)
                    val isCurrent = index == currentIndex

                    val backgroundColor = when {
                        isReview -> ExamReviewPurple
                        isAnswered -> ExamSuccessGreen
                        else -> ExamUnvisitedGrey.copy(alpha = 0.35f)
                    }

                    val textColor = when {
                        isReview || isAnswered -> Color.White
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(backgroundColor)
                            .then(
                                if (isCurrent) {
                                    Modifier.border(2.5.dp, NavyPrimary, CircleShape)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable {
                                onSelectQuestion(index)
                                onDismiss()
                            }
                            .testTag("palette_q_${index + 1}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                            fontSize = 14.sp,
                            color = textColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatusBadge(
    color: Color,
    label: String,
    count: Int
) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$label: $count",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
