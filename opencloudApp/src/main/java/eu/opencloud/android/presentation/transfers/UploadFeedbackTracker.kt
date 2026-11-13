package eu.opencloud.android.presentation.transfers

import eu.opencloud.android.domain.transfers.model.OCTransfer
import eu.opencloud.android.domain.transfers.model.TransferStatus
import eu.opencloud.android.domain.transfers.model.UploadEnqueuedBy

/** Tracks foreground changes without replaying old upload results on launch or rotation. */
class UploadFeedbackTracker {
    private var accountName: String? = null
    private var previousStatuses: Map<Long, TransferStatus>? = null

    data class Feedback(val uploading: Int, val queued: Int, val succeeded: Int, val failed: Int)

    fun update(account: String, transfers: List<OCTransfer>): Feedback {
        if (accountName != account) {
            accountName = account
            previousStatuses = null
        }
        val uploads = transfers.filter {
            it.accountName == account && it.createdBy == UploadEnqueuedBy.ENQUEUED_BY_USER
        }
        val previous = previousStatuses
        val changed = if (previous == null) emptyList() else uploads.filter {
            it.id != null && previous[it.id] != it.status
        }
        previousStatuses = uploads.mapNotNull { upload -> upload.id?.let { it to upload.status } }.toMap()
        return Feedback(
            uploading = uploads.count { it.status == TransferStatus.TRANSFER_IN_PROGRESS },
            queued = uploads.count { it.status == TransferStatus.TRANSFER_QUEUED },
            succeeded = changed.count { it.status == TransferStatus.TRANSFER_SUCCEEDED },
            failed = changed.count { it.status == TransferStatus.TRANSFER_FAILED },
        )
    }
}
