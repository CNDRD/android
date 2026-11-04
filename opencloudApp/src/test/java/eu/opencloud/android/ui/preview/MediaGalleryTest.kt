package eu.opencloud.android.ui.preview

import android.accounts.Account
import android.app.Application
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.FragmentManager
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ApplicationProvider
import eu.opencloud.android.R
import eu.opencloud.android.testutil.OC_FILE
import eu.opencloud.android.testutil.OC_FILE_DOWNLOADED
import eu.opencloud.android.usecases.transfers.DOWNLOAD_ADDED_MESSAGE
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class MediaGalleryTest {
    private val photo = OC_FILE_DOWNLOADED.copy(id = 1, remotePath = "/photo.jpg")
    private val video = OC_FILE.copy(id = 2, remotePath = "/clip.mp4", mimeType = "video/mp4")
    private val nextPhoto = photo.copy(id = 3, remotePath = "/next.jpg")

    @Test
    fun `photos and videos stay interleaved in folder order excluding other files`() {
        val pdf = photo.copy(mimeType = "application/pdf", remotePath = "/document.pdf")
        assertEquals(listOf(photo, video, nextPhoto), galleryMediaFiles(listOf(photo, pdf, video, nextPhoto), photo))
    }

    @Test
    fun `missing parent cache still previews the opened video`() {
        assertEquals(listOf(video), galleryMediaFiles(emptyList(), video))
    }

    @Test
    fun `opening a video selects its page and uses streaming even without local content`() {
        val adapter = adapter()
        assertEquals(1, adapter.getFilePosition(video))
        assertTrue(adapter.getItem(0) is PreviewImageFragment)
        assertTrue(adapter.getItem(1) is PreviewVideoFragment)
        assertTrue(adapter.getItem(2) is PreviewImageFragment)
    }

    @Test
    fun `progress events do not replace the streaming page with a download or error page`() {
        val adapter = adapter()
        adapter.onDownloadEvent(video, DOWNLOAD_ADDED_MESSAGE, false)
        assertTrue(adapter.getItem(1) is PreviewVideoFragment)
        assertTrue(!adapter.pendingErrorAt(1))
    }

    @Test
    fun `leaving a video releases decoder detaches surface and remembers position and pause state`() {
        val fragment = PreviewVideoFragment()
        val player = mockk<ExoPlayer>(relaxed = true)
        val playerView = mockk<PlayerView>(relaxed = true)
        every { player.currentPosition } returns 12345L
        every { player.playWhenReady } returns false
        setField(fragment, "player", player)
        setField(fragment, "playerView", playerView)

        fragment.onPause()

        verify(exactly = 1) { playerView.player = null }
        verify(exactly = 1) { player.release() }
        assertEquals(12345L, field(fragment, "playbackPosition"))
        assertEquals(false, field(fragment, "playWhenReady"))
        assertEquals(null, field(fragment, "player"))
    }

    @Test
    fun `late video preparation cannot start an offscreen player`() {
        val fragment = PreviewVideoFragment()
        val player = mockk<ExoPlayer>(relaxed = true)
        setField(fragment, "player", player)
        fragment.OnPrepareVideoPlayerTaskCallback(mockk())
        verify(exactly = 0) { player.prepare() }
    }

    @Test
    fun `modern controls inflate and invoke play rewind and forward on the player`() {
        val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext<Application>(), R.style.Theme_openCloud_Toolbar)
        val root = LayoutInflater.from(context).inflate(R.layout.preview_video_fragment, null)
        val view = root.findViewById<PlayerView>(R.id.gallery_video_player)
        val player = mockk<Player>(relaxed = true)
        every { player.applicationLooper } returns Looper.getMainLooper()
        every { player.availableCommands } returns Player.Commands.Builder().addAllCommands().build()
        every { player.isCommandAvailable(any()) } returns true
        every { player.currentTimeline } returns Timeline.EMPTY
        every { player.playbackState } returns Player.STATE_READY
        every { player.playWhenReady } returns false
        view.player = player

        assertNotNull(root.findViewById<View>(androidx.media3.ui.R.id.exo_progress))
        assertNotNull(root.findViewById<View>(androidx.media3.ui.R.id.exo_settings))
        root.findViewById<View>(androidx.media3.ui.R.id.exo_play_pause).performClick()
        root.findViewById<View>(androidx.media3.ui.R.id.exo_rew_with_amount).performClick()
        root.findViewById<View>(androidx.media3.ui.R.id.exo_ffwd_with_amount).performClick()
        verify { player.play() }
        verify { player.seekBack() }
        verify { player.seekForward() }
        view.player = null
    }

    private fun adapter() = PreviewImagePagerAdapter(
        mockk<FragmentManager>(), Account("test", "test"), mutableListOf(photo, video, nextPhoto)
    )

    private fun setField(fragment: PreviewVideoFragment, name: String, value: Any) {
        fragment.javaClass.getDeclaredField(name).apply { isAccessible = true }.set(fragment, value)
    }

    private fun field(fragment: PreviewVideoFragment, name: String): Any? =
        fragment.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(fragment)
}
