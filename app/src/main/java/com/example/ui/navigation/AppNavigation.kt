package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExamResult
import com.example.data.model.MockExam
import com.example.data.repository.AiMockGeneratorRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.ExamRepository
import com.example.data.repository.MockRepository
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.admin.AiMockGeneratorScreen
import com.example.ui.screens.admin.CreateEditMockScreen
import com.example.ui.screens.exam.ExamScreen
import com.example.ui.screens.exam.ResultReviewScreen
import com.example.ui.screens.leaderboard.LeaderboardScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.student.StudentDashboardScreen
import com.example.ui.theme.GoldWing
import com.example.ui.theme.NavyPrimary
import kotlinx.coroutines.launch

enum class MainTab {
    TESTS,
    LEADERBOARD,
    PROFILE,
    ADMIN
}

sealed interface AppDestination {
    data class TabView(val tab: MainTab) : AppDestination
    data class TakingExam(val mock: MockExam) : AppDestination
    data class ViewingResult(val result: ExamResult, val mock: MockExam) : AppDestination
    data object AiGenerator : AppDestination
    data class CreateEditMock(val mock: MockExam?) : AppDestination
}

@Composable
fun AppNavigation(
    authRepository: AuthRepository,
    mockRepository: MockRepository,
    examRepository: ExamRepository,
    aiRepository: AiMockGeneratorRepository
) {
    val userProfile by authRepository.userProfile.collectAsState()
    val scope = rememberCoroutineScope()

    var currentDestination by remember { mutableStateOf<AppDestination>(AppDestination.TabView(MainTab.TESTS)) }

    // Seed default mocks if admin
    LaunchedEffect(userProfile) {
        if (userProfile?.isAdmin == true) {
            mockRepository.seedInitialMocksIfEmpty()
        }
    }

    // Handle back button for secondary screens
    when (currentDestination) {
        is AppDestination.TabView -> {
            val tab = (currentDestination as AppDestination.TabView).tab
            if (tab != MainTab.TESTS) {
                BackHandler {
                    currentDestination = AppDestination.TabView(MainTab.TESTS)
                }
            }
        }
        is AppDestination.TakingExam -> {
            // Handled within ExamScreen with exit dialog
        }
        is AppDestination.ViewingResult,
        is AppDestination.AiGenerator,
        is AppDestination.CreateEditMock -> {
            BackHandler {
                currentDestination = AppDestination.TabView(MainTab.TESTS)
            }
        }
    }

    when (val dest = currentDestination) {
        is AppDestination.TakingExam -> {
            ExamScreen(
                mock = dest.mock,
                examRepository = examRepository,
                onExamCompleted = { result ->
                    currentDestination = AppDestination.ViewingResult(result, dest.mock)
                },
                onCancelExam = {
                    currentDestination = AppDestination.TabView(MainTab.TESTS)
                }
            )
        }
        is AppDestination.ViewingResult -> {
            ResultReviewScreen(
                result = dest.result,
                mock = dest.mock,
                onNavigateToLeaderboard = {
                    currentDestination = AppDestination.TabView(MainTab.LEADERBOARD)
                },
                onNavigateToDashboard = {
                    currentDestination = AppDestination.TabView(MainTab.TESTS)
                }
            )
        }
        is AppDestination.AiGenerator -> {
            AiMockGeneratorScreen(
                aiRepository = aiRepository,
                mockRepository = mockRepository,
                onMockSaved = {
                    currentDestination = AppDestination.TabView(MainTab.ADMIN)
                },
                onBack = {
                    currentDestination = AppDestination.TabView(MainTab.ADMIN)
                }
            )
        }
        is AppDestination.CreateEditMock -> {
            CreateEditMockScreen(
                initialMock = dest.mock,
                mockRepository = mockRepository,
                onSaved = {
                    currentDestination = AppDestination.TabView(MainTab.ADMIN)
                },
                onBack = {
                    currentDestination = AppDestination.TabView(MainTab.ADMIN)
                }
            )
        }
        is AppDestination.TabView -> {
            val currentTab = dest.tab
            val isAdmin = userProfile?.isAdmin == true

            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = NavyPrimary,
                        contentColor = Color.White,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("main_bottom_nav")
                    ) {
                        NavigationBarItem(
                            selected = currentTab == MainTab.TESTS,
                            onClick = { currentDestination = AppDestination.TabView(MainTab.TESTS) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MainTab.TESTS) Icons.Default.Assignment else Icons.Outlined.Assignment,
                                    contentDescription = "Tests"
                                )
                            },
                            label = { Text("Tests", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NavyPrimary,
                                selectedTextColor = GoldWing,
                                indicatorColor = GoldWing,
                                unselectedIconColor = Color(0xFFB0C4DE),
                                unselectedTextColor = Color(0xFFB0C4DE)
                            ),
                            modifier = Modifier.testTag("nav_tab_tests")
                        )

                        NavigationBarItem(
                            selected = currentTab == MainTab.LEADERBOARD,
                            onClick = { currentDestination = AppDestination.TabView(MainTab.LEADERBOARD) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MainTab.LEADERBOARD) Icons.Default.Leaderboard else Icons.Outlined.Leaderboard,
                                    contentDescription = "Rankings"
                                )
                            },
                            label = { Text("Ranks", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NavyPrimary,
                                selectedTextColor = GoldWing,
                                indicatorColor = GoldWing,
                                unselectedIconColor = Color(0xFFB0C4DE),
                                unselectedTextColor = Color(0xFFB0C4DE)
                            ),
                            modifier = Modifier.testTag("nav_tab_ranks")
                        )

                        NavigationBarItem(
                            selected = currentTab == MainTab.PROFILE,
                            onClick = { currentDestination = AppDestination.TabView(MainTab.PROFILE) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MainTab.PROFILE) Icons.Default.Person else Icons.Outlined.Person,
                                    contentDescription = "Profile"
                                )
                            },
                            label = { Text("Profile", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NavyPrimary,
                                selectedTextColor = GoldWing,
                                indicatorColor = GoldWing,
                                unselectedIconColor = Color(0xFFB0C4DE),
                                unselectedTextColor = Color(0xFFB0C4DE)
                            ),
                            modifier = Modifier.testTag("nav_tab_profile")
                        )

                        if (isAdmin) {
                            NavigationBarItem(
                                selected = currentTab == MainTab.ADMIN,
                                onClick = { currentDestination = AppDestination.TabView(MainTab.ADMIN) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == MainTab.ADMIN) Icons.Default.AdminPanelSettings else Icons.Outlined.AdminPanelSettings,
                                        contentDescription = "Admin"
                                    )
                                },
                                label = { Text("Admin", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NavyPrimary,
                                    selectedTextColor = GoldWing,
                                    indicatorColor = GoldWing,
                                    unselectedIconColor = Color(0xFFB0C4DE),
                                    unselectedTextColor = Color(0xFFB0C4DE)
                                ),
                                modifier = Modifier.testTag("nav_tab_admin")
                            )
                        }
                    }
                }
            ) { scaffoldPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding)
                ) {
                    when (currentTab) {
                        MainTab.TESTS -> {
                            StudentDashboardScreen(
                                userProfile = userProfile,
                                mockRepository = mockRepository,
                                onStartExam = { selectedMock ->
                                    currentDestination = AppDestination.TakingExam(selectedMock)
                                }
                            )
                        }
                        MainTab.LEADERBOARD -> {
                            LeaderboardScreen(
                                userProfile = userProfile,
                                examRepository = examRepository,
                                mockRepository = mockRepository
                            )
                        }
                        MainTab.PROFILE -> {
                            ProfileScreen(
                                userProfile = userProfile,
                                authRepository = authRepository,
                                examRepository = examRepository,
                                onViewPastResult = { pastResult ->
                                    scope.launch {
                                        val mock = mockRepository.getMockById(pastResult.mockId)
                                        if (mock != null) {
                                            currentDestination = AppDestination.ViewingResult(pastResult, mock)
                                        }
                                    }
                                },
                                onSignOut = {
                                    // Signed out
                                }
                            )
                        }
                        MainTab.ADMIN -> {
                            AdminDashboardScreen(
                                userProfile = userProfile,
                                mockRepository = mockRepository,
                                onNavigateToAiGenerator = {
                                    currentDestination = AppDestination.AiGenerator
                                },
                                onNavigateToCreateManual = {
                                    currentDestination = AppDestination.CreateEditMock(null)
                                },
                                onEditMock = { mockToEdit ->
                                    currentDestination = AppDestination.CreateEditMock(mockToEdit)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
