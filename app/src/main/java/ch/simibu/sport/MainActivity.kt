package ch.simibu.sport

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import ch.simibu.sport.core.designsystem.SportTheme
import ch.simibu.sport.data.repository.LanguageMode
import ch.simibu.sport.data.repository.LanguageRepository
import ch.simibu.sport.data.repository.ThemeMode
import ch.simibu.sport.data.repository.ThemeRepository
import ch.simibu.sport.domain.repository.AuthRepository
import ch.simibu.sport.presentation.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

class LocalizedContextWrapper(
    base: Context,
    private val localizedContext: Context
) : ContextWrapper(base) {
    override fun getResources(): Resources = localizedContext.resources
    override fun getAssets(): AssetManager = localizedContext.assets
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var themeRepository: ThemeRepository

    @Inject
    lateinit var languageRepository: LanguageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentUser by authRepository.currentUser.collectAsState(initial = null)
            val themeMode by themeRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val languageMode by languageRepository.languageMode.collectAsState(initial = LanguageMode.SYSTEM)

            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemDark
            }

            val locale = remember(languageMode) {
                when (languageMode) {
                    LanguageMode.SYSTEM -> Locale.getDefault()
                    LanguageMode.ENGLISH -> Locale("en")
                    LanguageMode.GERMAN -> Locale("de")
                    LanguageMode.FRENCH -> Locale("fr")
                    LanguageMode.SPANISH -> Locale("es")
                    LanguageMode.ITALIAN -> Locale("it")
                }
            }

            val baseContext = LocalContext.current
            val baseConfig = LocalConfiguration.current

            val configuration = remember(locale, baseConfig) {
                Configuration(baseConfig).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
            }
            val localizedContext = remember(locale, baseContext, configuration) {
                val configContext = baseContext.createConfigurationContext(configuration)
                LocalizedContextWrapper(baseContext, configContext)
            }

            LaunchedEffect(locale) {
                Locale.setDefault(locale)
            }

            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalContext provides localizedContext
            ) {
                val navController = rememberNavController()

                SportTheme(
                    darkTheme = isDarkTheme
                ) {
                    AppNavigation(
                        navController = navController,
                        isAuthenticated = currentUser != null
                    )
                }
            }
        }
    }
}
