package eu.opencloud.android.presentation.startup

import android.app.Application
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import eu.opencloud.android.R
import eu.opencloud.android.databinding.NavCoordinatorLayoutBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class StartupLayoutTest {
    private fun context() = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext<Application>(),
        R.style.Theme_openCloud_Toolbar,
    )

    @Test
    fun `main screen layout inflates with the real app theme`() {
        checkStartupLayout()
    }

    @Test
    @Config(qualifiers = "night")
    fun `main screen layout inflates in dark mode`() {
        checkStartupLayout()
    }

    private fun checkStartupLayout() {
        val binding = NavCoordinatorLayoutBinding.inflate(LayoutInflater.from(context()))
        assertNotNull(binding.root.findViewById<View>(R.id.root_toolbar))
        assertNotNull(binding.root.findViewById<MaterialButton>(R.id.upload_feedback))
        assertEquals(View.GONE, binding.uploadFeedback.visibility)
        var clicked = false
        binding.uploadFeedback.setOnClickListener { clicked = true }
        binding.uploadFeedback.visibility = View.VISIBLE
        binding.uploadFeedback.performClick()
        assertTrue(clicked)
    }
}
