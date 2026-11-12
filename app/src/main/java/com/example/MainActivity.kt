package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.ApplicationViewModel
import com.example.ui.Screen
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val viewModel: ApplicationViewModel = viewModel()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (currentRoute != Screen.Home::class.qualifiedName) {
                            val topBarTitle = when (currentRoute) {
                                Screen.Vault::class.qualifiedName -> "Mein Daten-Tresor"
                                Screen.Scan::class.qualifiedName -> "Notizen scannen"
                                Screen.Editor::class.qualifiedName -> "Anschreiben"
                                Screen.History::class.qualifiedName -> "Gespeicherte Anschreiben"
                                else -> ""
                            }
                            TopAppBar(
                                title = { Text(topBarTitle) },
                                navigationIcon = {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable<Screen.Home> {
                            HomeScreen(onNavigate = { route ->
                                when (route) {
                                    "vault" -> navController.navigate(Screen.Vault)
                                    "scan" -> navController.navigate(Screen.Scan)
                                    "editor" -> navController.navigate(Screen.Editor)
                                    "history" -> navController.navigate(Screen.History)
                                }
                            })
                        }
                        composable<Screen.Vault> {
                            VaultScreen(viewModel)
                        }
                        composable<Screen.Scan> {
                            ScanScreen(viewModel, onNext = { navController.navigate(Screen.Editor) })
                        }
                        composable<Screen.Editor> {
                            EditorScreen(
                                viewModel = viewModel,
                                onFinish = { navController.navigate(Screen.History) },
                                onOpenScanner = { navController.navigate(Screen.Scan) }
                            )
                        }
                        composable<Screen.History> {
                            HistoryScreen(
                                viewModel = viewModel,
                                onOpenInEditor = {
                                    navController.navigate(Screen.Editor) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
