package ru.mcn.knowledgebase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ru.mcn.knowledgebase.core.navigation.AppNavGraph
import ru.mcn.knowledgebase.ui.theme.MCNKnowledgeBaseTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            MCNKnowledgeBaseTheme {

                AppNavGraph()

            }
        }
    }
}