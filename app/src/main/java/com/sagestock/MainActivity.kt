package com.sagestock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sagestock.data.SettingsManager
import com.sagestock.ui.navigation.SageStockApp
import com.sagestock.ui.theme.SageStockTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val palette by settingsManager.palette.collectAsStateWithLifecycle()
            SageStockTheme(palette = palette) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SageStockApp()
                }
            }
        }
    }
}
