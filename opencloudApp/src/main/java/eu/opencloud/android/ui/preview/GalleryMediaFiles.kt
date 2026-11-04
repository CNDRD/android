package eu.opencloud.android.ui.preview

import eu.opencloud.android.domain.files.model.OCFile

/** Keep folder order and retain the opened item even when its parent is not cached. */
internal fun galleryMediaFiles(folderFiles: List<OCFile>, openedFile: OCFile): List<OCFile> {
    val media = folderFiles.filter { it.isImage || it.isVideo }
    val containsOpened = media.any {
        it.remotePath == openedFile.remotePath && it.spaceId == openedFile.spaceId
    }
    return if (containsOpened) media else media + openedFile
}
