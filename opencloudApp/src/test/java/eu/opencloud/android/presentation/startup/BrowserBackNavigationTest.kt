package eu.opencloud.android.presentation.startup

import android.app.Application
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import eu.opencloud.android.domain.files.model.OCFile
import eu.opencloud.android.domain.files.model.OCFile.Companion.ROOT_PARENT_ID
import eu.opencloud.android.presentation.files.filelist.MainFileListFragment
import eu.opencloud.android.ui.activity.FileDisplayActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class BrowserBackNavigationTest {
    @Test
    fun `system back navigates up exactly one folder without finishing the activity`() {
        val (activity, browser) = browser(parentId = 42L)

        activity.onBackPressedDispatcher.onBackPressed()

        verify(exactly = 1) { browser.onBrowseUp() }
        assertFalse(activity.isFinishing)
    }

    @Test
    fun `legacy back uses the same folder navigation`() {
        val (activity, browser) = browser(parentId = 42L)

        activity.onBackPressed()

        verify(exactly = 1) { browser.onBrowseUp() }
        assertFalse(activity.isFinishing)
    }

    @Test
    fun `back closes the drawer before navigating the folder`() {
        val (activity, browser) = browser(parentId = 42L)
        every { activity.isDrawerOpen() } returns true
        every { activity.closeDrawer() } returns Unit

        activity.onBackPressedDispatcher.onBackPressed()

        verify(exactly = 1) { activity.closeDrawer() }
        verify(exactly = 0) { browser.onBrowseUp() }
        assertFalse(activity.isFinishing)
    }

    @Test
    fun `back at the personal root still exits`() {
        val (activity, browser) = browser(parentId = ROOT_PARENT_ID)

        activity.onBackPressedDispatcher.onBackPressed()

        assertTrue(activity.isFinishing)
        verify(exactly = 0) { browser.onBrowseUp() }
    }

    private fun browser(parentId: Long): Pair<FileDisplayActivity, MainFileListFragment> {
        // Attach the real activity without starting account and network initialization.
        val activity = spyk(Robolectric.buildActivity(FileDisplayActivity::class.java).get())
        val lifecycle = LifecycleRegistry(activity)
        val fragments = mockk<FragmentManager>()
        val browser = mockk<MainFileListFragment>(relaxed = true)
        every { activity.lifecycle } returns lifecycle
        every { activity.supportFragmentManager } returns fragments
        every { activity.isDrawerOpen() } returns false
        every { fragments.findFragmentByTag("LIST_OF_FILES") } returns browser
        every { fragments.findFragmentByTag("SECOND_FRAGMENT") } returns null
        every { browser.getCurrentFile() } returns mockk<OCFile> {
            every { this@mockk.parentId } returns parentId
        }
        every { browser.getCurrentSpace() } returns null
        every { browser.isFabExpanded() } returns false
        activity.registerBackNavigation()
        lifecycle.currentState = Lifecycle.State.STARTED
        return activity to browser
    }
}
