package eu.opencloud.android.ui.activity

import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import eu.opencloud.android.R
import eu.opencloud.android.presentation.transfers.TransferListFragment

/** Keeps uploads in the browser window, retaining the fragment and its scroll position between visits. */
internal class EmbeddedUploadsTab(
    private val activity: FragmentActivity,
    private val createFragment: () -> Fragment = { TransferListFragment() },
) {
    fun show() {
        val manager = activity.supportFragmentManager
        val existing = manager.findFragmentByTag(TAG)
        val fragment = existing ?: createFragment()
        manager.beginTransaction().apply {
            setCustomAnimations(0, 0)
            if (existing == null) add(R.id.uploads_fragment_container, fragment, TAG)
            setMaxLifecycle(fragment, Lifecycle.State.RESUMED)
        }.commitNow()
        activity.findViewById<View>(R.id.uploads_fragment_container).isVisible = true
    }

    fun hide() {
        activity.findViewById<View>(R.id.uploads_fragment_container).isVisible = false
        activity.supportFragmentManager.findFragmentByTag(TAG)?.let { fragment ->
            activity.supportFragmentManager.beginTransaction()
                .setMaxLifecycle(fragment, Lifecycle.State.STARTED)
                .commitNow()
        }
    }

    companion object {
        private const val TAG = "EMBEDDED_UPLOADS_TAB"
    }
}
