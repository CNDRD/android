package eu.opencloud.android.presentation.startup

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import eu.opencloud.android.R
import eu.opencloud.android.ui.activity.EmbeddedUploadsTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertSame
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class EmbeddedUploadsTabTest {
    @Test
    fun `repeated tab switches retain the uploads view and browser without starting another activity`() {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        val activity = controller.get()
        val root = FrameLayout(activity)
        val browser = FrameLayout(activity).apply { id = R.id.left_fragment_container }
        val navigation = View(activity)
        val uploads = FrameLayout(activity).apply { id = R.id.uploads_fragment_container }
        root.addView(browser)
        root.addView(uploads)
        root.addView(navigation)
        activity.setContentView(root)
        val browserFragment = TestUploadsFragment()
        activity.supportFragmentManager.beginTransaction().add(R.id.left_fragment_container, browserFragment).commitNow()
        val browserView = browserFragment.requireView()
        val tab = EmbeddedUploadsTab(activity) { TestUploadsFragment() }
        tab.show()
        val uploadView = uploads.getChildAt(0)
        repeat(10) {
            tab.hide()
            assertEquals(View.GONE, uploads.visibility)
            assertFalse(browserFragment.isHidden)
            tab.show()
            assertTrue(browserFragment.isHidden)
            assertSame(browserView, browserFragment.requireView())
            assertEquals(View.VISIBLE, uploads.visibility)
            assertSame(uploadView, uploads.getChildAt(0))
            assertSame(browser, root.getChildAt(0))
            assertSame(navigation, root.getChildAt(2))
        }
        // Recreating the controller (as on restoration) must find the retained fragment.
        EmbeddedUploadsTab(activity) { error("Must reuse the existing fragment") }.show()
        assertSame(uploadView, uploads.getChildAt(0))
        assertNull(shadowOf(activity).nextStartedActivity)
        controller.pause().stop().destroy()
    }

    class TestUploadsFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            TextView(requireContext()).apply { text = "upload state" }
    }
}
