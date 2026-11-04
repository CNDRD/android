package eu.opencloud.android.ui.preview

import android.accounts.Account
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.ui.PlayerView
import com.google.android.material.snackbar.Snackbar
import eu.opencloud.android.R
import eu.opencloud.android.domain.files.model.OCFile
import eu.opencloud.android.extensions.filterMenuOptions
import eu.opencloud.android.extensions.sendDownloadedFilesByShareSheet
import eu.opencloud.android.presentation.files.operations.FileOperation
import eu.opencloud.android.presentation.files.operations.FileOperationsViewModel
import eu.opencloud.android.presentation.files.removefile.RemoveFilesDialogFragment
import eu.opencloud.android.presentation.previews.PreviewVideoViewModel
import eu.opencloud.android.ui.fragment.FileFragment
import eu.opencloud.android.utils.PreferenceUtils
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

/** A video page owns a player only while it is the resumed (selected) gallery page. */
@OptIn(UnstableApi::class)
class PreviewVideoFragment : FileFragment(), Player.Listener, PrepareVideoPlayerAsyncTask.OnPrepareVideoPlayerTaskListener {
    private val previewViewModel by viewModel<PreviewVideoViewModel> { parametersOf(file) }
    private val operationsViewModel: FileOperationsViewModel by inject()
    private var playerView: PlayerView? = null
    private var player: ExoPlayer? = null
    private var prepareTask: PrepareVideoPlayerAsyncTask? = null
    private var playbackPosition = 0L
    private var playWhenReady = true
    private var playbackSpeed = 1f
    private var errorView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        file = requireArguments().getParcelable(ARG_FILE)
        playbackPosition = savedInstanceState?.getLong(ARG_POSITION)
            ?: if (file == activity?.intent?.getParcelableExtra<OCFile>(eu.opencloud.android.ui.activity.FileActivity.EXTRA_FILE)) {
                activity?.intent?.getLongExtra(PreviewVideoActivity.EXTRA_PLAY_POSITION, 0L) ?: 0L
            } else {
                0L
            }
        playWhenReady = savedInstanceState?.getBoolean(ARG_PLAY, true) ?: true
        playbackSpeed = savedInstanceState?.getFloat(ARG_SPEED, 1f) ?: 1f
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.preview_video_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.filterTouchesWhenObscured = PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(requireContext())
        errorView = view.findViewById<TextView>(R.id.video_error).apply {
            setOnClickListener { releasePlayer(); initializePlayer() }
        }
        playerView = view.findViewById<PlayerView>(R.id.gallery_video_player).apply {
            setShowPreviousButton(false)
            setShowNextButton(false)
            setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                if (isResumed) gallery().setVideoControlsVisible(visibility == View.VISIBLE)
            })
            setFullscreenButtonClickListener { gallery().toggleFullScreen() }
        }
        // Keep the video edge-to-edge, but leave playback controls clear of system bars/cutouts.
        val controls = view.findViewById<View>(R.id.gallery_video_controls)
        ViewCompat.setOnApplyWindowInsetsListener(controls) { target, insets ->
            val bars = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            target.updatePadding(left = bars.left, right = bars.right, bottom = bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(controls)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    previewViewModel.getCurrentFile().collect { updatedFile ->
                        if (updatedFile == null) {
                            requireActivity().finish()
                        } else {
                            file = updatedFile
                            previewViewModel.filterMenuOptions(file, mContainerActivity.storageManager.account.name)
                            requireActivity().invalidateOptionsMenu()
                        }
                    }
                }
                launch { previewViewModel.menuOptions.collect { requireActivity().invalidateOptionsMenu() } }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        PreviewVideoActivity.isOpen = true
        PreviewVideoActivity.currentFilePreviewing = file
        initializePlayer()
        gallery().setVideoControlsVisible(true)
    }

    override fun onPause() {
        releasePlayer()
        PreviewVideoActivity.isOpen = false
        PreviewVideoActivity.currentFilePreviewing = null
        super.onPause()
    }

    private fun initializePlayer() {
        if (!isResumed || player != null) return
        errorView?.visibility = View.GONE
        val newPlayer = ExoPlayer.Builder(requireContext())
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(SEEK_INCREMENT)
            .setSeekForwardIncrementMs(SEEK_INCREMENT)
            .build()
        player = newPlayer
        newPlayer.setPlaybackSpeed(playbackSpeed)
        newPlayer.addListener(this)
        playerView?.player = newPlayer
        val account = requireArguments().getParcelable<Account>(ARG_ACCOUNT)
        prepareTask = PrepareVideoPlayerAsyncTask(requireContext().applicationContext, this, file, account).apply { execute() }
    }

    override fun OnPrepareVideoPlayerTaskCallback(mediaSource: MediaSource?) {
        if (!isResumed) return
        if (mediaSource == null) {
            showPlaybackError()
            return
        }
        player?.apply {
            setMediaSource(mediaSource)
            seekTo(playbackPosition)
            playWhenReady = this@PreviewVideoFragment.playWhenReady
            prepare()
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        Timber.e(error, "Error in gallery video player")
        showPlaybackError()
    }

    private fun showPlaybackError() {
        errorView?.visibility = View.VISIBLE
        playerView?.showController()
    }

    private fun releasePlayer() {
        prepareTask?.cancel(true)
        prepareTask = null
        player?.let {
            playbackPosition = it.currentPosition
            playWhenReady = it.playWhenReady
            playbackSpeed = it.playbackParameters.speed
            it.removeListener(this)
            playerView?.player = null
            it.release()
        }
        player = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(ARG_POSITION, player?.currentPosition ?: playbackPosition)
        outState.putBoolean(ARG_PLAY, player?.playWhenReady ?: playWhenReady)
        outState.putFloat(ARG_SPEED, player?.playbackParameters?.speed ?: playbackSpeed)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        releasePlayer()
        playerView = null
        errorView = null
        super.onDestroyView()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.filterMenuOptions(previewViewModel.menuOptions.value, file.hasWritePermission)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share_file -> {
                mContainerActivity.fileOperationsHelper.showShareFile(file)
            }
            R.id.action_open_file_with -> {
                player?.pause()
                mContainerActivity.fileOperationsHelper.openFile(file)
            }
            R.id.action_remove_file -> {
                player?.pause()
                RemoveFilesDialogFragment.newInstance(file).show(parentFragmentManager, RemoveFilesDialogFragment.TAG_REMOVE_FILES_DIALOG_FRAGMENT)
            }
            R.id.action_see_details -> {
                player?.pause()
                mContainerActivity.showDetails(file)
            }
            R.id.action_send_file -> {
                player?.pause()
                requireActivity().sendDownloadedFilesByShareSheet(listOf(file))
            }
            R.id.action_sync_file, R.id.action_download_file -> {
                mContainerActivity.fileOperationsHelper.syncFile(file)
            }
            R.id.action_set_available_offline -> {
                operationsViewModel.performOperation(FileOperation.SetFilesAsAvailableOffline(listOf(file)))
                Snackbar.make(requireView(), R.string.confirmation_set_available_offline, Snackbar.LENGTH_LONG).show()
            }
            R.id.action_unset_available_offline -> {
                operationsViewModel.performOperation(FileOperation.UnsetFilesAsAvailableOffline(listOf(file)))
                Snackbar.make(requireView(), R.string.confirmation_unset_available_offline, Snackbar.LENGTH_LONG).show()
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    override fun onFileMetadataChanged(updatedFile: OCFile) { file = updatedFile }
    override fun onFileMetadataChanged() {
        mContainerActivity.storageManager.getFileById(file.id ?: -1L)?.let { file = it }
    }
    override fun onFileContentChanged() = Unit
    override fun updateViewForSyncInProgress() = Unit
    override fun updateViewForSyncOff() = Unit

    private fun gallery() = requireActivity() as PreviewImageActivity

    companion object {
        private const val ARG_FILE = "FILE"
        private const val ARG_ACCOUNT = "ACCOUNT"
        private const val ARG_POSITION = "POSITION"
        private const val ARG_PLAY = "PLAY"
        private const val ARG_SPEED = "SPEED"
        private const val SEEK_INCREMENT = 10_000L

        fun newInstance(file: OCFile, account: Account) = PreviewVideoFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_FILE, file)
                putParcelable(ARG_ACCOUNT, account)
            }
        }
    }
}
