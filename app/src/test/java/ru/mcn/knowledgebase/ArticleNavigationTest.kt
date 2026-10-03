package ru.mcn.knowledgebase

import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavType
import androidx.navigation.createGraph
import androidx.navigation.navArgument
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.mcn.knowledgebase.core.navigation.openArticle

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class ArticleNavigationTest {
    private fun controller() = TestNavHostController(RuntimeEnvironment.getApplication()).apply {
        setViewModelStore(ViewModelStore())
        navigatorProvider.addNavigator(ComposeNavigator())
        graph = createGraph(startDestination = "catalog") {
            composable("catalog") {}
            composable("article/{articleId}?anchor={anchor}", arguments = listOf(
                navArgument("articleId") { type = NavType.StringType },
                navArgument("anchor") { type = NavType.StringType; nullable = true; defaultValue = null }
            )) {}
        }
    }

    @Test fun `back visits C B A before returning to catalog`() {
        val nav = controller()
        listOf("A", "B", "C").forEach { nav.openArticle(it) }
        for (id in listOf("C", "B", "A")) {
            assertEquals(id, nav.currentBackStackEntry!!.arguments!!.getString("articleId"))
            assertTrue(nav.popBackStack())
        }
        assertEquals("catalog", nav.currentDestination!!.route)
    }

    @Test fun `repeated taps on current article do not add history entries`() {
        val nav = controller()
        nav.openArticle("A")
        repeat(5) { nav.openArticle("A") }
        nav.popBackStack()
        assertEquals("catalog", nav.currentDestination!!.route)
    }

    @Test fun `return link to earlier article is a new visit with its own anchor`() {
        val nav = controller()
        nav.openArticle("A")
        nav.openArticle("B")
        nav.openArticle("A", "шаг 2")
        assertEquals("шаг 2", nav.currentBackStackEntry!!.arguments!!.getString("anchor"))
        nav.popBackStack()
        assertEquals("B", nav.currentBackStackEntry!!.arguments!!.getString("articleId"))
        nav.popBackStack()
        assertEquals("A", nav.currentBackStackEntry!!.arguments!!.getString("articleId"))
    }
}
