package ch.simibu.sport.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import ch.simibu.sport.presentation.auth.AuthScreen
import ch.simibu.sport.presentation.dashboard.DashboardScreen
import ch.simibu.sport.presentation.exercises.ExerciseDetailsScreen
import ch.simibu.sport.presentation.exercises.ExerciseFormScreen
import ch.simibu.sport.presentation.exercises.ExerciseHistoryScreen
import ch.simibu.sport.presentation.exercises.ExercisesScreen
import ch.simibu.sport.presentation.journal.JournalScreen
import ch.simibu.sport.presentation.journal.WorkoutDetailsScreen
import ch.simibu.sport.presentation.journal.WorkoutFormScreen
import ch.simibu.sport.presentation.profile.BodyHistoryScreen
import ch.simibu.sport.presentation.profile.ProfileScreen
import ch.simibu.sport.presentation.profile.SettingsScreen
import ch.simibu.sport.presentation.templates.ProgramFormScreen
import ch.simibu.sport.presentation.templates.TemplateFormScreen
import ch.simibu.sport.presentation.templates.TemplatesScreen

import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import ch.simibu.sport.R

data class BottomNavItem(
    val route: Any,
    @StringRes val titleRes: Int,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(DashboardDestination, R.string.nav_home, Icons.Default.Speed),
    BottomNavItem(JournalDestination, R.string.nav_journal, Icons.AutoMirrored.Filled.MenuBook),
    BottomNavItem(ExercisesDestination, R.string.nav_exercises, Icons.Default.FitnessCenter),
    BottomNavItem(TemplatesDestination, R.string.nav_routines, Icons.Default.Bookmark),
    BottomNavItem(ProfileDestination, R.string.nav_profile, Icons.Default.Person)
)

@Composable
fun AppNavigation(
    navController: NavHostController,
    isAuthenticated: Boolean,
    activeWorkoutViewModel: ActiveWorkoutViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val activeSession by activeWorkoutViewModel.activeSession.collectAsStateWithLifecycle()
    val elapsedSeconds by activeWorkoutViewModel.elapsedSeconds.collectAsStateWithLifecycle()

    val isTopLevelDestination = bottomNavItems.any { item ->
        currentDestination?.hasRoute(item.route::class) == true
    }
    val isOnWorkoutForm = currentDestination?.hasRoute(WorkoutFormDestination::class) == true
    val showBanner = activeSession != null && !isOnWorkoutForm
    val showBottomNav = isTopLevelDestination && isAuthenticated

    Scaffold(
        bottomBar = {
            if (showBanner || showBottomNav) {
                Column {
                    val session = activeSession
                    if (showBanner && session != null) {
                        ActiveWorkoutBanner(
                            elapsedSeconds = elapsedSeconds,
                            showTimer = session.takeWorkoutTime,
                            exerciseCount = session.state.exercises.size,
                            templateName = session.templateName,
                            onClick = {
                                navController.navigate(WorkoutFormDestination(templateId = session.templateId)) {
                                    launchSingleTop = true
                                }
                            },
                            onDiscard = {
                                activeWorkoutViewModel.discardWorkout()
                            }
                        )
                    }
                    if (showBottomNav) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            bottomNavItems.forEach { item ->
                                val selected = currentDestination?.hasRoute(item.route::class) == true
                                val title = stringResource(item.titleRes)

                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(item.icon, contentDescription = title)
                                    },
                                    label = { Text(title) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isAuthenticated) DashboardDestination else AuthDestination,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(280)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(280)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(280)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(280)
                )
            }
        ) {
            // Auth
            composable<AuthDestination> {
                AuthScreen(
                    onAuthSuccess = {
                        navController.navigate(DashboardDestination) {
                            popUpTo(AuthDestination) { inclusive = true }
                        }
                    }
                )
            }

            // Dashboard
            composable<DashboardDestination> {
                DashboardScreen(
                    onNavigateToNewWorkout = { templateId ->
                        navController.navigate(WorkoutFormDestination(templateId = templateId))
                    },
                    onNavigateToWorkoutDetails = { id ->
                        navController.navigate(WorkoutDetailsDestination(id))
                    },
                    onNavigateToTemplates = {
                        navController.navigate(TemplatesDestination)
                    },
                    onNavigateToWeightHistory = {
                        navController.navigate(BodyHistoryDestination)
                    },
                    onNavigateToExerciseDetails = { exerciseId ->
                        navController.navigate(ExerciseDetailsDestination(exerciseId))
                    }
                )
            }

            // Journal
            composable<JournalDestination> {
                JournalScreen(
                    onNavigateToWorkoutDetails = { id ->
                        navController.navigate(WorkoutDetailsDestination(id))
                    },
                    onNavigateToEditWorkout = { id ->
                        navController.navigate(WorkoutFormDestination(id = id))
                    },
                    onNavigateToNewWorkout = {
                        navController.navigate(WorkoutFormDestination())
                    }
                )
            }

            // Workout Details
            composable<WorkoutDetailsDestination> { backStackEntry ->
                val destination = backStackEntry.toRoute<WorkoutDetailsDestination>()
                WorkoutDetailsScreen(
                    workoutId = destination.id,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id ->
                        navController.navigate(WorkoutFormDestination(id = id))
                    }
                )
            }

            // Active Workout Form
            composable<WorkoutFormDestination> { backStackEntry ->
                val destination = backStackEntry.toRoute<WorkoutFormDestination>()
                WorkoutFormScreen(
                    workoutId = destination.id,
                    templateId = destination.templateId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Exercises
            composable<ExercisesDestination> {
                ExercisesScreen(
                    onNavigateToDetails = { id ->
                        navController.navigate(ExerciseDetailsDestination(id))
                    },
                    onNavigateToNewExercise = {
                        navController.navigate(ExerciseFormDestination())
                    }
                )
            }

            // Exercise Details
            composable<ExerciseDetailsDestination> { backStackEntry ->
                val destination = backStackEntry.toRoute<ExerciseDetailsDestination>()
                ExerciseDetailsScreen(
                    exerciseId = destination.id,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHistory = { id ->
                        navController.navigate(ExerciseHistoryDestination(id))
                    }
                )
            }

            // Exercise History
            composable<ExerciseHistoryDestination> { backStackEntry ->
                val destination = backStackEntry.toRoute<ExerciseHistoryDestination>()
                ExerciseHistoryScreen(
                    exerciseId = destination.id,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // New Exercise
            composable<ExerciseFormDestination> {
                ExerciseFormScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Templates
            composable<TemplatesDestination> {
                TemplatesScreen(
                    onNavigateToStartWorkout = { templateId ->
                        navController.navigate(WorkoutFormDestination(templateId = templateId))
                    },
                    onNavigateToNewTemplate = {
                        navController.navigate(TemplateFormDestination())
                    },
                    onNavigateToEditTemplate = { templateId ->
                        navController.navigate(TemplateFormDestination(id = templateId))
                    },
                    onNavigateToNewProgram = {
                        navController.navigate(ProgramFormDestination())
                    },
                    onNavigateToEditProgram = { programId ->
                        navController.navigate(ProgramFormDestination(id = programId))
                    }
                )
            }

            // Template Form
            composable<TemplateFormDestination> { backStackEntry ->
                val destination = backStackEntry.toRoute<TemplateFormDestination>()
                TemplateFormScreen(
                    templateId = destination.id,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Program Form
            composable<ProgramFormDestination> { backStackEntry ->
                val destination = backStackEntry.toRoute<ProgramFormDestination>()
                ProgramFormScreen(
                    programId = destination.id,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Profile
            composable<ProfileDestination> {
                ProfileScreen(
                    onNavigateToBodyHistory = {
                        navController.navigate(BodyHistoryDestination)
                    },
                    onNavigateToSettings = {
                        navController.navigate(SettingsDestination)
                    }
                )
            }

            // Settings
            composable<SettingsDestination> {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSignOutSuccess = {
                        navController.navigate(AuthDestination) {
                            popUpTo(DashboardDestination) { inclusive = true }
                        }
                    }
                )
            }

            // Body History
            composable<BodyHistoryDestination> {
                BodyHistoryScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
