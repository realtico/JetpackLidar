package com.example.lidar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

/**
 * Activity principal: inicializa o DataStore, cria o ViewModel e desenha a UI.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: LidarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa utilitário de DataStore (evita leaks)
        DataStoreUtil.init(applicationContext)

        // Opcional: conteúdo sob status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LidarViewerScreen(viewModel)
                }
            }
        }
    }
}
