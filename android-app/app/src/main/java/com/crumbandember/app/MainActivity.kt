package com.crumbandember.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.crumbandember.app.ui.navigation.BakeryNavGraph
import com.crumbandember.app.ui.theme.CrumbAndEmberTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as BakeryApplication

        setContent {
            CrumbAndEmberTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BakeryNavGraph(app = app)
                }
            }
        }
    }
}
