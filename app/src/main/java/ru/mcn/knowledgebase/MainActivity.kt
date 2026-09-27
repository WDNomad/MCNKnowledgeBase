package ru.mcn.knowledgebase

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ru.mcn.knowledgebase.core.navigation.AppNavGraph
import ru.mcn.knowledgebase.ui.theme.MCNKnowledgeBaseTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.decorView.isForceDarkAllowed = false
        }

        AppContextProvider.context = applicationContext

        setContent {

            MCNKnowledgeBaseTheme {

                AppNavGraph()

            }
        }
    }
}
