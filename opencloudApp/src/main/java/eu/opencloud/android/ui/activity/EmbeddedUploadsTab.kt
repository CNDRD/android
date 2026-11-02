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
            // Hidden fragments retain their views but stop contributing browser-only menu actions.
            manager.fragments.filter { it.id == R.id.left_fragment_container || it.id == R.id.right_fragment_container }
                .forEach { hide(it) }
            setMaxLifecycle(fragment, Lifecycle.State.RESUMED)
        }.commitNow()
        activity.findViewById<View>(R.id.uploads_fragment_container).isVisible = true
    }

    fun hide() {
        activity.findViewById<View>(R.id.uploads_fragment_container).isVisible = false
        val manager = activity.supportFragmentManager
        manager.beginTransaction().apply {
            manager.findFragmentByTag(TAG)?.let { setMaxLifecycle(it, Lifecycle.State.STARTED) }
            manager.fragments.filter { it.id == R.id.left_fragment_container || it.id == R.id.right_fragment_container }
                .forEach { show(it) }
        }.commitNow()
    }

    companion object {
        private const val TAG = "EMBEDDED_UPLOADS_TAB"
    }
}
