package com.example.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.QueryBuilder
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.LeaderboardEntry
import com.example.data.model.UserProfile
import com.example.data.repository.ExamRepository
import com.example.data.repository.MockRepository
import com.example.ui.components.DefenceHeader
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldWing
import com.example.ui.theme.NavyPrimary

@Composable
fun LeaderboardScreen(
    userProfile: UserProfile?,
    examRepository: ExamRepository,
    mockRepository: MockRepository,
    modifier: Modifier = Modifier
) {
    val publishedMocks by mockRepository.observePublishedMocks().collectAsState(initial = emptyList())
    var selectedMockIdFilter by remember { mutableStateOf<String?>(null) }

    val leaderboardEntries by examRepository.observeLeaderboard(selectedMockIdFilter).collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DefenceHeader(isAdmin = userProfile?.isAdmin == true)

        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "All-India Defence Leaderboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Real-time national ranking with tie-breaking by time taken.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = GoldWing,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Test filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedMockIdFilter == null,
                    onClick = { selectedMockIdFilter = null },
                    label = { Text("All Defence Mocks", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NavyPrimary,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                publishedMocks.forEach { mock ->
                    val isSelected = selectedMockIdFilter == mock.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedMockIdFilter = mock.id },
                        label = { Text(mock.title, fontSize = 11.sp, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NavyPrimary,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        if (leaderboardEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MilitaryTech,
                        contentDescription = null,
                        tint = GoldWing,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No rankings yet for this test",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Be the first to attempt and claim Rank 1!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top 3 Podium
                if (leaderboardEntries.size >= 3) {
                    item {
                        PodiumView(
                            first = leaderboardEntries.getOrNull(0),
                            second = leaderboardEntries.getOrNull(1),
                            third = leaderboardEntries.getOrNull(2)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                itemsIndexed(leaderboardEntries, key = { _, item -> item.id }) { index, entry ->
                    LeaderboardRowCard(
                        rank = index + 1,
                        entry = entry,
                        isCurrentUser = entry.userId == userProfile?.userId
                    )
                }
            }
        }
    }
}

@Composable
private fun PodiumView(
    first: LeaderboardEntry?,
    second: LeaderboardEntry?,
    third: LeaderboardEntry?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NATIONAL TOP ACHIEVERS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = GoldWing
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Rank 2 Silver
                second?.let {
                    PodiumPillar(
                        rank = 2,
                        name = it.userName,
                        score = it.score,
                        badgeColor = Color(0xFFC0C0C0),
                        height = 80.dp
                    )
                }

                // Rank 1 Gold
                first?.let {
                    PodiumPillar(
                        rank = 1,
                        name = it.userName,
                        score = it.score,
                        badgeColor = GoldWing,
                        height = 110.dp
                    )
                }

                // Rank 3 Bronze
                third?.let {
                    PodiumPillar(
                        rank = 3,
                        name = it.userName,
                        score = it.score,
                        badgeColor = Color(0xFFCD7F32),
                        height = 65.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun PodiumPillar(
    rank: Int,
    name: String,
    score: Double,
    badgeColor: Color,
    height: androidx.compose.ui.unit.Dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        Surface(
            color = badgeColor,
            shape = CircleShape,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$rank",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = NavyPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Text(
            text = "$score M",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = GoldWing
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .width(60.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(Color(0xFF1E3A5F)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#$rank",
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun LeaderboardRowCard(
    rank: Int,
    entry: LeaderboardEntry,
    isCurrentUser: Boolean
) {
    val (rankBg, rankText) = when (rank) {
        1 -> GoldWing to NavyPrimary
        2 -> Color(0xFFC0C0C0) to NavyPrimary
        3 -> Color(0xFFCD7F32) to Color.White
        else -> Color(0xFFEEF2F6) to Color(0xFF334E68)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leaderboard_row_$rank"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) NavyPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentUser) 3.dp else 1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Rank Number Badge
            Surface(
                color = rankBg,
                shape = CircleShape,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$rank",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = rankText
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User Photo or Initial
            if (!entry.userPhoto.isNullOrBlank()) {
                AsyncImage(
                    model = entry.userPhoto,
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )
            } else {
                Surface(
                    color = NavyPrimary.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = entry.userName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.userName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = GoldWing,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "YOU",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = NavyPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = entry.mockTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.score} Marks",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = NavyPrimary
                )

                val mins = entry.timeTakenSeconds / 60
                val secs = entry.timeTakenSeconds % 60
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QueryBuilder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${mins}m ${secs}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
