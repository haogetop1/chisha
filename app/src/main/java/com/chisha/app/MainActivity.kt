package com.chisha.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chisha.app.domain.MealSlot
import com.chisha.app.ui.AppViewModel
import com.chisha.app.ui.screens.CollectionScreen
import com.chisha.app.ui.screens.CuisineScreen
import com.chisha.app.ui.screens.DrawScreen
import com.chisha.app.ui.screens.HistoryScreen
import com.chisha.app.ui.screens.HomeScreen
import com.chisha.app.ui.screens.ProfileScreen
import com.chisha.app.ui.screens.RatingScreen
import com.chisha.app.ui.screens.RecipeScreen
import com.chisha.app.ui.screens.ResultScreen
import com.chisha.app.ui.screens.TutorialScreen
import com.chisha.app.ui.screens.UploadScreen
import com.chisha.app.ui.theme.ChishaTheme
import com.chisha.app.ui.theme.InkSecondary
import com.chisha.app.ui.theme.Ochre
import com.chisha.app.ui.theme.WarmBg
import com.chisha.app.ui.theme.WarmSurface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChishaTheme {
                val vm: AppViewModel = viewModel()
                ChishaApp(vm)
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "今天", Icons.Outlined.Home),
    Tab("collection", "图鉴", Icons.Outlined.Collections),
    Tab("history", "记录", Icons.Outlined.Receipt),
    Tab("profile", "我的", Icons.Outlined.Person)
)

@Composable
fun ChishaApp(vm: AppViewModel) {
    val state = vm.state
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "home"
    val showBar = tabs.any { it.route == route }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Scaffold(
        containerColor = WarmBg,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = WarmSurface, tonalElevation = 0.dp) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                if (route != tab.route) {
                                    nav.navigate(tab.route) {
                                        popUpTo("home") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text(tab.label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Ochre,
                                selectedTextColor = Ochre,
                                unselectedIconColor = InkSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Ochre)
                }
                return@Box
            }
            NavHost(navController = nav, startDestination = "home") {
                composable("home") {
                    HomeScreen(
                        state = state,
                        onPickSlot = { slot -> nav.navigate("cuisine/${slot.key}") },
                        onOpenCollection = { nav.navigate("collection") },
                        onOpenProfile = { nav.navigate("profile") }
                    )
                }
                composable("collection") {
                    CollectionScreen(state) { recipe ->
                        nav.navigate("peek/${recipe.id}")
                    }
                }
                composable("history") { HistoryScreen(state) }
                composable("profile") {
                    ProfileScreen(state, onSetNickname = vm::setNickname)
                }
                composable("cuisine/{slot}") { entry ->
                    val slot = MealSlot.of(entry.arguments?.getString("slot"))
                    FlowFrame(onBack = { nav.popBackStack() }) {
                        CuisineScreen(
                            slot = slot,
                            onPick = { cuisine ->
                                vm.draw(slot, cuisine)
                                nav.navigate("draw")
                            },
                            onBack = { nav.popBackStack() }
                        )
                    }
                }
                composable("draw") {
                    if (state.session == null) {
                        LaunchedEffect(Unit) { nav.popBackStack() }
                    } else {
                        FlowFrame(onBack = { nav.popBackStack() }) {
                            DrawScreen(
                                state = state,
                                onReroll = { vm.reroll() },
                                onConfirm = {
                                    state.session?.let { s ->
                                        // 把 cuisine / draw 从栈里弹掉：
                                        // 从菜谱页返回应该回首页，而不是退回抽奖页再「重抽一次」
                                        nav.navigate("recipe/${s.outcome.recipe.id}") {
                                            popUpTo("home") { inclusive = false }
                                        }
                                    }
                                },
                                onAnimated = { vm.markSessionAnimated() }
                            )
                        }
                    }
                }
                composable("recipe/{id}") { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    val recipe = state.recipes.firstOrNull { it.id == id }
                    if (recipe == null) {
                        LaunchedEffect(Unit) { nav.popBackStack() }
                    } else {
                        FlowFrame(onBack = { nav.popBackStack() }) {
                            RecipeScreen(
                                recipe = recipe,
                                lastRating = vm.ratingFor(recipe.id)?.stars,
                                onStart = { nav.navigate("tutorial") }
                            )
                        }
                    }
                }
                composable("peek/{id}") { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    val recipe = state.recipes.firstOrNull { it.id == id }
                    if (recipe == null) {
                        LaunchedEffect(Unit) { nav.popBackStack() }
                    } else {
                        FlowFrame(onBack = { nav.popBackStack() }) {
                            RecipeScreen(
                                recipe = recipe,
                                lastRating = vm.ratingFor(recipe.id)?.stars,
                                onStart = {},
                                readOnly = true
                            )
                        }
                    }
                }
                composable("tutorial") {
                    val recipe = state.session?.outcome?.recipe
                    if (recipe == null) {
                        LaunchedEffect(Unit) { nav.popBackStack() }
                    } else {
                        FlowFrame(onBack = { nav.popBackStack() }) {
                            TutorialScreen(recipe = recipe, onFinish = { nav.navigate("upload") })
                        }
                    }
                }
                composable("upload") {
                    val recipe = state.session?.outcome?.recipe
                    if (recipe == null) {
                        LaunchedEffect(Unit) { nav.popBackStack() }
                    } else {
                        FlowFrame(onBack = { nav.popBackStack() }) {
                            UploadScreen(recipe = recipe) { photo, source ->
                                vm.completeCooking(photo, source) { result ->
                                    if (result != null) nav.navigate("result") else nav.popBackStack()
                                }
                            }
                        }
                    }
                }
                composable("result") {
                    val result = state.lastResult
                    if (result == null) {
                        LaunchedEffect(Unit) { nav.popBackStack() }
                    } else {
                        ResultScreen(
                            result = result,
                            onRate = { nav.navigate("rating") },
                            onDone = { goHome(nav, vm) }
                        )
                    }
                }
                composable("rating") {
                    val recipe = state.lastResult?.recipe
                    if (recipe == null) {
                        LaunchedEffect(Unit) { nav.popBackStack() }
                    } else {
                        FlowFrame(onBack = { nav.popBackStack() }) {
                            RatingScreen(
                                recipeName = recipe.name,
                                onSubmit = { stars, comment ->
                                    vm.submitRating(recipe.id, stars, comment) { goHome(nav, vm) }
                                },
                                onSkip = { goHome(nav, vm) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun goHome(nav: NavHostController, vm: AppViewModel) {
    vm.clearSession()
    vm.clearResult()
    nav.navigate("home") { popUpTo("home") { inclusive = true } }
}

@Composable
private fun FlowFrame(onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, top = 6.dp)
        ) {
            TextButton(onClick = onBack) {
                Text("‹ 返回", fontSize = 14.sp, color = InkSecondary)
            }
        }
        Box(Modifier.weight(1f)) { content() }
    }
}
