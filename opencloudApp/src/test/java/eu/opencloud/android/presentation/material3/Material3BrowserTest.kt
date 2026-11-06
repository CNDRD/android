package eu.opencloud.android.presentation.material3

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import eu.opencloud.android.R
import eu.opencloud.android.presentation.files.SortOptionsView.AdditionalView
import eu.opencloud.android.presentation.files.SortOrder
import eu.opencloud.android.presentation.files.SortType
import eu.opencloud.android.presentation.files.ViewType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class Material3BrowserTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `sort stays left and column settings sit on the right of the same row`() {
        controls(onDensity = {})
        val sort = compose.onNodeWithTag("browser_sort").fetchSemanticsNode().boundsInRoot
        val density = compose.onNodeWithTag("browser_density").fetchSemanticsNode().boundsInRoot
        val view = compose.onNodeWithTag("browser_view").fetchSemanticsNode().boundsInRoot
        assertTrue(density.left >= sort.right)
        assertTrue(view.left >= density.right)
        assertEquals(sort.center.y, density.center.y, 1f)
        assertEquals(density.center.y, view.center.y, 1f)
    }

    @Test
    fun `density selection reaches the file browser callback`() {
        var columns = 0
        controls(onDensity = { columns = it })
        compose.onNodeWithTag("browser_density").performClick()
        compose.onNodeWithTag("density_3").performClick()
        compose.runOnIdle { assertEquals(3, columns) }
        compose.onNodeWithTag("density_3").assertDoesNotExist()
    }

    @Test
    fun `sort selection preserves the order and reaches the browser callback`() {
        var selection: Pair<SortType, SortOrder>? = null
        controls(onSort = { type, order -> selection = type to order })
        compose.onNodeWithTag("browser_sort").performClick()
        compose.onNodeWithTag("sort_SORT_TYPE_BY_SIZE").performClick()
        compose.runOnIdle { assertEquals(SortType.SORT_TYPE_BY_SIZE to SortOrder.SORT_ORDER_ASCENDING, selection) }
    }

    @Test
    @Config(qualifiers = "night")
    fun `view toggle works in dark mode`() {
        var changed = false
        controls(onViewType = { changed = true })
        compose.onNodeWithTag("browser_view").performClick()
        compose.runOnIdle { assertTrue(changed) }
    }

    @Test
    fun `folder picker keeps create folder action without grid density controls`() {
        var created = false
        controls(mode = AdditionalView.CREATE_FOLDER, onCreateFolder = { created = true })
        compose.onNodeWithContentDescription(text(R.string.content_description_create_new_folder)).performClick()
        compose.runOnIdle { assertTrue(created) }
        compose.onNodeWithTag("browser_density").assertDoesNotExist()
        compose.onNodeWithTag("browser_view").assertDoesNotExist()
    }

    @Test
    fun `upload files card invokes the file picker action`() {
        var files = 0
        var camera = 0
        sheet(onFiles = { files++ }, onCamera = { camera++ })
        compose.onNodeWithText(text(R.string.upload_files_title)).performClick()
        compose.runOnIdle { assertEquals(1, files); assertEquals(0, camera) }
    }

    @Test
    fun `upload camera card invokes the camera action`() {
        var files = 0
        var camera = 0
        sheet(onFiles = { files++ }, onCamera = { camera++ })
        compose.onNodeWithText(text(R.string.upload_from_camera_title)).performClick()
        compose.runOnIdle { assertEquals(0, files); assertEquals(1, camera) }
    }

    private fun text(id: Int) = ApplicationProvider.getApplicationContext<Application>().getString(id)

    private fun controls(
        mode: AdditionalView = AdditionalView.VIEW_TYPE,
        onSort: (SortType, SortOrder) -> Unit = { _, _ -> },
        onDensity: (Int) -> Unit = {},
        onViewType: () -> Unit = {},
        onCreateFolder: () -> Unit = {},
    ) {
        compose.setContent {
            OpenCloudMaterialTheme {
                BrowserControls(
                    sortType = SortType.SORT_TYPE_BY_NAME,
                    sortOrder = SortOrder.SORT_ORDER_ASCENDING,
                    viewType = ViewType.VIEW_TYPE_LIST,
                    columns = 0,
                    additionalView = mode,
                    onSort = onSort,
                    onLegacySort = {},
                    onViewType = onViewType,
                    onDensity = onDensity,
                    onCreateFolder = onCreateFolder,
                )
            }
        }
    }

    private fun sheet(onFiles: () -> Unit, onCamera: () -> Unit) {
        compose.setContent {
            OpenCloudMaterialTheme {
                UploadSourceSheet(destination = "Photos", onDismiss = {}, onFiles = onFiles, onCamera = onCamera)
            }
        }
    }
}
