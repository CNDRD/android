package eu.opencloud.android.presentation.startup

import android.app.Application
import android.content.Intent
import eu.opencloud.android.domain.files.model.FileListOption
import eu.opencloud.android.ui.activity.FileActivity
import eu.opencloud.android.ui.activity.FileDisplayActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class BottomNavigationTest {
    @Test
    fun `returning from uploads reuses the browser without a window animation for every file tab`() {
        // Attach without creating the account-dependent UI: exercise the real navigation method.
        val activity = Robolectric.buildActivity(FileActivity::class.java).get()
        FileListOption.values().forEach { option ->
            activity.navigateToOption(option)
            val intent = shadowOf(activity).nextStartedActivity
            assertEquals(FileDisplayActivity::class.java.name, intent.component?.className)
            assertEquals(option, intent.getParcelableExtra<FileListOption>(FileActivity.EXTRA_FILE_LIST_OPTION))
            assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
            assertTrue(intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
            assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NO_ANIMATION != 0)
        }
    }
}
