package ch.widmedia.tageswert.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ch.widmedia.tageswert.ui.screens.AlleEintraegeScreen
import ch.widmedia.tageswert.ui.screens.EinstellungenScreen
import ch.widmedia.tageswert.ui.screens.EintragScreen
import ch.widmedia.tageswert.ui.screens.HauptScreen

sealed class Ziel(val route: String) {
    data object Haupt : Ziel("haupt")
    data object Liste : Ziel("liste")
    data object Eintrag : Ziel("eintrag/{datum}") {
        fun mitDatum(datum: String) = "eintrag/$datum"
    }
    data object Einstellungen : Ziel("einstellungen")
}

@Composable
fun TagesWertNavigation(
    viewModel: MainViewModel,
    onLock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadLastExportTime(context)
    }

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
                onAlleEintraege = {
                    navController.navigate(Ziel.Liste.route)
                },
                onEinstellungen = {
                    navController.navigate(Ziel.Einstellungen.route)
                },
                onLock = onLock
            )
        }

        composable(Ziel.Liste.route) {
            AlleEintraegeScreen(
                viewModel = viewModel,
                onEintragKlick = { datum ->
                    navController.navigate(Ziel.Eintrag.mitDatum(datum))
                },
                onZurueck = { navController.popBackStack() }
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
