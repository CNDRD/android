package eu.opencloud.android.presentation.transfers

import eu.opencloud.android.domain.transfers.model.UploadEnqueuedBy
import eu.opencloud.android.domain.transfers.model.TransferStatus
import eu.opencloud.android.testutil.OC_TRANSFER
import org.junit.Assert.assertEquals
import org.junit.Test

class UploadFeedbackTrackerTest {
    private val tracker = UploadFeedbackTracker()
    private val account = OC_TRANSFER.accountName

    @Test
    fun `initial history does not announce old results but shows active uploads`() {
        val result = tracker.update(account, listOf(
            OC_TRANSFER,
            OC_TRANSFER.copy(id = 1, status = TransferStatus.TRANSFER_SUCCEEDED),
            OC_TRANSFER.copy(id = 2, status = TransferStatus.TRANSFER_FAILED),
            OC_TRANSFER.copy(id = 3, status = TransferStatus.TRANSFER_QUEUED),
        ))
        assertEquals(UploadFeedbackTracker.Feedback(1, 1, 0, 0), result)
    }

    @Test
    fun `completion is announced once and is not replayed after rotation`() {
        tracker.update(account, listOf(OC_TRANSFER))
        val finished = listOf(OC_TRANSFER.copy(status = TransferStatus.TRANSFER_SUCCEEDED))
        assertEquals(1, tracker.update(account, finished).succeeded)
        assertEquals(0, tracker.update(account, finished).succeeded)
    }

    @Test
    fun `fast upload finishing between snapshots is still announced`() {
        tracker.update(account, emptyList())
        assertEquals(1, tracker.update(account, listOf(
            OC_TRANSFER.copy(status = TransferStatus.TRANSFER_SUCCEEDED)
        )).succeeded)
    }

    @Test
    fun `failure then retry can report a subsequent success`() {
        tracker.update(account, listOf(OC_TRANSFER))
        assertEquals(1, tracker.update(account, listOf(OC_TRANSFER.copy(status = TransferStatus.TRANSFER_FAILED))).failed)
        assertEquals(0, tracker.update(account, listOf(OC_TRANSFER.copy(status = TransferStatus.TRANSFER_QUEUED))).failed)
        assertEquals(1, tracker.update(account, listOf(OC_TRANSFER.copy(status = TransferStatus.TRANSFER_SUCCEEDED))).succeeded)
    }

    @Test
    fun `other accounts are excluded and switching accounts establishes a new baseline`() {
        tracker.update(account, emptyList())
        val other = OC_TRANSFER.copy(accountName = "other", status = TransferStatus.TRANSFER_SUCCEEDED)
        assertEquals(UploadFeedbackTracker.Feedback(0, 0, 0, 0), tracker.update(account, listOf(other)))
        assertEquals(0, tracker.update("other", listOf(other)).succeeded)
    }

    @Test
    fun `automatic camera backups do not flood manual upload feedback`() {
        tracker.update(account, emptyList())
        val automatic = OC_TRANSFER.copy(createdBy = UploadEnqueuedBy.ENQUEUED_AS_AUTOMATIC_UPLOAD_PICTURE)
        assertEquals(UploadFeedbackTracker.Feedback(0, 0, 0, 0), tracker.update(account, listOf(automatic)))
        assertEquals(0, tracker.update(account, listOf(
            automatic.copy(status = TransferStatus.TRANSFER_SUCCEEDED)
        )).succeeded)
    }

    @Test
    fun `clearing history does not produce a completion`() {
        tracker.update(account, listOf(OC_TRANSFER))
        assertEquals(UploadFeedbackTracker.Feedback(0, 0, 0, 0), tracker.update(account, emptyList()))
    }
}
