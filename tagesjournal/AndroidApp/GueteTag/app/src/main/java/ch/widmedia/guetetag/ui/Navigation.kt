package ch.widmedia.guetetag.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ch.widmedia.guetetag.ui.screens.EinstellungenScreen
import ch.widmedia.guetetag.ui.screens.EintragScreen
import ch.widmedia.guetetag.ui.screens.HauptScreen

sealed class Ziel(val route: String) {
    data object Haupt : Ziel("haupt")
    data object Eintrag : Ziel("eintrag/{datum}") {
        fun mitDatum(datum: String) = "eintrag/$datum"
    }
    data object Einstellungen : Ziel("einstellungen")
}

@Composable
fun GueteTagNavigation(
    viewModel: MainViewModel,
    onLock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Ziel.Haupt.route,
        modifier = modifier
    ) {
        composable(Ziel.Haupt.route) {
            HauptScreen(
                viewModel = viewModel,
                onEintragKlick = { datum ->
                    navController.navigate(Ziel.Eintrag.mitDatum(datum))
                },
                onEinstellungen = {
                    navController.navigate(Ziel.Einstellungen.route)
                },
                onLock = onLock
            )
        }

        composable(
            route = Ziel.Eintrag.route,
            arguments = listOf(navArgument("datum") { type = NavType.StringType })
        ) { backStackEntry ->
            val datum = backStackEntry.arguments?.getString("datum") ?: return@composable
            EintragScreen(
                datum = datum,
                viewModel = viewModel,
                onZurueck = { navController.popBackStack() }
            )
        }

        composable(Ziel.Einstellungen.route) {
            EinstellungenScreen(
                viewModel = viewModel,
                onZurueck = { navController.popBackStack() }
            )
        }
    }
}
