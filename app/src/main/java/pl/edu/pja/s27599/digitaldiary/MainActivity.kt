package pl.edu.pja.s27599.digitaldiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import pl.edu.pja.s27599.digitaldiary.ui.auth.AuthScreen
import pl.edu.pja.s27599.digitaldiary.ui.auth.AuthViewModel
import pl.edu.pja.s27599.digitaldiary.ui.entry_detail.ENTRY_ID_ARG
import pl.edu.pja.s27599.digitaldiary.ui.entry_detail.EntryDetailScreen
import pl.edu.pja.s27599.digitaldiary.ui.entry_detail.EntryDetailViewModel
import pl.edu.pja.s27599.digitaldiary.ui.entry_list.EntryListScreen
import pl.edu.pja.s27599.digitaldiary.ui.entry_list.EntryListViewModel
import pl.edu.pja.s27599.digitaldiary.ui.theme.DigitalDiaryTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DigitalDiaryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    DigitalDiaryNavHost(navController = navController)
                }
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object EntryList : Screen("entry_list")

    object EntryDetail : Screen("entry_detail/{$ENTRY_ID_ARG}") {
        fun createRoute(entryId: Int? = -1) = "entry_detail/$entryId"
    }
}

@Composable
fun DigitalDiaryNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Auth.route) {
        composable(Screen.Auth.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            AuthScreen(
                viewModel = authViewModel,
                onAuthenticated = {
                    navController.navigate(Screen.EntryList.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.EntryList.route) {
            val entryListViewModel: EntryListViewModel = hiltViewModel()
            EntryListScreen(
                viewModel = entryListViewModel,
                onNavigateToCreate = { navController.navigate(Screen.EntryDetail.createRoute()) },
                onNavigateToDetail = { entryId -> navController.navigate(Screen.EntryDetail.createRoute(entryId)) }
            )
        }
        composable(
            route = Screen.EntryDetail.route,
            arguments = listOf(navArgument(ENTRY_ID_ARG) {
                type = NavType.IntType
                defaultValue = -1
            })
        ) {
            val entryDetailViewModel: EntryDetailViewModel = hiltViewModel()
            EntryDetailScreen(
                viewModel = entryDetailViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}