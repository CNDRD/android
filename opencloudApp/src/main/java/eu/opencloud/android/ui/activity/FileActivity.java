/**
 * openCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * @author Abel García de Prada
 * @author Aitor Ballesteros Pavón
 * <p>
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2024 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.opencloud.android.ui.activity;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.snackbar.Snackbar;
import eu.opencloud.android.R;
import eu.opencloud.android.domain.files.model.FileListOption;
import eu.opencloud.android.domain.files.model.OCFile;
import eu.opencloud.android.lib.common.network.CertificateCombinedException;
import eu.opencloud.android.lib.common.operations.OnRemoteOperationListener;
import eu.opencloud.android.lib.common.operations.RemoteOperation;
import eu.opencloud.android.lib.common.operations.RemoteOperationResult;
import eu.opencloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import eu.opencloud.android.presentation.authentication.AccountUtils;
import eu.opencloud.android.presentation.authentication.AuthenticatorConstants;
import eu.opencloud.android.presentation.authentication.LoginActivity;
import eu.opencloud.android.services.OperationsService;
import eu.opencloud.android.services.OperationsService.OperationsServiceBinder;
import eu.opencloud.android.ui.dialog.ConfirmationDialogFragment;
import eu.opencloud.android.ui.dialog.SslUntrustedCertDialog;
import eu.opencloud.android.ui.errorhandling.ErrorMessageAdapter;
import eu.opencloud.android.ui.helpers.FileOperationsHelper;
import timber.log.Timber;

/**
 * Activity with common behaviour for activities handling {@link OCFile}s in openCloud {@link Account}s .
 */
public class FileActivity extends DrawerActivity
        implements OnRemoteOperationListener, ComponentsGetter, SslUntrustedCertDialog.OnSslUntrustedCertListener {

    public static final String EXTRA_FILE = "eu.opencloud.android.ui.activity.FILE";
    public static final String EXTRA_ACCOUNT = "eu.opencloud.android.ui.activity.ACCOUNT";
    public static final String EXTRA_FROM_NOTIFICATION =
            "eu.opencloud.android.ui.activity.FROM_NOTIFICATION";
    public static final String EXTRA_FILE_LIST_OPTION = "EXTRA_FILE_LIST_OPTION";

    private static final String KEY_WAITING_FOR_OP_ID = "WAITING_FOR_OP_ID";
    private static final String KEY_ACTION_BAR_TITLE = "ACTION_BAR_TITLE";

    // go to a high number, since the low numbers are used by android
    public static final int REQUEST_CODE__UPDATE_CREDENTIALS = 0;
    public static final int REQUEST_CODE__LAST_SHARED = REQUEST_CODE__UPDATE_CREDENTIALS;

    protected static final long DELAY_TO_REQUEST_OPERATIONS_LATER = 200;

    /* Dialog tags */
    private static final String DIALOG_UNTRUSTED_CERT = "DIALOG_UNTRUSTED_CERT";
    private static final String DIALOG_CERT_NOT_SAVED = "DIALOG_CERT_NOT_SAVED";

    /**
     * Main {@link OCFile} handled by the activity.
     */
    private OCFile mFile;

    /**
     * Flag to signal if the activity is launched by a notification
     */
    private boolean mFromNotification;

    /**
     * Messages handler associated to the main thread and the life cycle of the activity
     */
    private Handler mHandler;

    private FileOperationsHelper mFileOperationsHelper;

    private ServiceConnection mOperationsServiceConnection = null;

    private OperationsServiceBinder mOperationsServiceBinder = null;

    private boolean mResumed = false;

    /**
     * Loads the openCloud {@link Account} and main {@link OCFile} to be handled by the instance of
     * the {@link FileActivity}.
     * <p>
     * Grants that a valid openCloud {@link Account} is associated to the instance, or that the user
     * is requested to create a new one.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        mFileOperationsHelper = new FileOperationsHelper(this);
        Account account = null;
        if (savedInstanceState != null) {
            mFile = savedInstanceState.getParcelable(FileActivity.EXTRA_FILE);
            mFromNotification = savedInstanceState.getBoolean(FileActivity.EXTRA_FROM_NOTIFICATION);
            mFileOperationsHelper.setOpIdWaitingFor(
                    savedInstanceState.getLong(KEY_WAITING_FOR_OP_ID, Long.MAX_VALUE)
            );
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(savedInstanceState.getString(KEY_ACTION_BAR_TITLE));
            }
        } else {
            account = getIntent().getParcelableExtra(FileActivity.EXTRA_ACCOUNT);
            mFile = getIntent().getParcelableExtra(FileActivity.EXTRA_FILE);
            mFromNotification = getIntent().getBooleanExtra(FileActivity.EXTRA_FROM_NOTIFICATION,
                    false);
        }

        AccountUtils.updateAccountVersion(this); // best place, before any access to AccountManager
        // or database

        setAccount(account, savedInstanceState != null);

        mOperationsServiceConnection = new OperationsServiceConnection();
        bindService(new Intent(this, OperationsService.class), mOperationsServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mResumed = true;
        if (mOperationsServiceBinder != null) {
            doOnResumeAndBound();
        }
    }

    @Override
    protected void onPause() {
        if (mOperationsServiceBinder != null) {
            mOperationsServiceBinder.removeOperationListener(this);
        }
        mResumed = false;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mOperationsServiceConnection != null) {
            unbindService(mOperationsServiceConnection);
            mOperationsServiceBinder = null;
        }
        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInsta…16510 tokens truncated…ction = launchAction,
                            uiResult = uiResult,
                            action = false
                        )
                    }

                    override fun onSkipButtonClick() {
                        applyAction(
                            posDialog = posDialog,
                            data = data,
                            replace = replace,
                            pos = posArray,
                            launchAction = launchAction,
                            uiResult = uiResult,
                            action = null
                        )
                    }

                    override fun onReplaceButtonClick() {
                        applyAction(
                            posDialog = posDialog,
                            data = data,
                            replace = replace,
                            pos = posArray,
                            launchAction = launchAction,
                            uiResult = uiResult,
                            action = true
                        )
                    }
                }
                )
            }
        }
    }

    private fun applyAction(
        posDialog: IntArray,
        data: List<OCFile>,
        replace: MutableList<Boolean?>,
        pos: IntArray,
        launchAction: (List<OCFile>, List<Boolean?>) -> Unit,
        uiResult: UIResult.Success<List<OCFile>>,
        action: Boolean?
    ) {
        var posDialog1 = posDialog
        var pos1 = pos
        if (fileOperationsViewModel.openDialogs[posDialog1[0]].isCheckBoxChecked) {
            repeat(data.asReversed().size) {
                replace.add(action)
                pos1[0]++
                if (pos1[0] == data.size) {
                    launchAction(
                        uiResult.data!!,
                        replace,
                    )
                }
            }
            dismissAllOpenDialogs()
        } else {
            replace.add(action)
            pos1[0]++
            if (pos1[0] == data.size) {
                launchAction(
                    uiResult.data!!,
                    replace,
                )
            }
            fileOperationsViewModel.openDialogs[posDialog1[0]].dismiss()
            fileOperationsViewModel.openDialogs.removeAt(posDialog1[0])
            if (posDialog1[0] == 0) {
                fileOperationsViewModel.openDialogs.clear()
            } else {
                posDialog1[0]--
            }
        }
    }

    private fun dismissAllOpenDialogs() {
        fileOperationsViewModel.openDialogs.forEach { dialog ->
            dialog.dismiss()
        }
        fileOperationsViewModel.openDialogs.clear()
    }

    private fun showDialogs() {
        fileOperationsViewModel.openDialogs.forEach { dialog ->
            dialog.show(this.supportFragmentManager, CUSTOM_DIALOG_TAG)
        }
    }

    private fun dismissDialogs() {
        fileOperationsViewModel.openDialogs.forEach { dialog ->
            dialog.dismiss()
        }
    }

    private fun launchCopyFile(files: List<OCFile>, replace: List<Boolean?>) {
        fileOperationsViewModel.performOperation(
            FileOperation.CopyOperation(
                listOfFilesToCopy = files,
                targetFolder = null,
                replace = replace,
                isUserLogged = eu.opencloud.android.presentation.authentication.AccountUtils.getCurrentOpenCloudAccount(this) != null,
            )
        )
    }

    private fun launchMoveFile(files: List<OCFile>, replace: List<Boolean?>) {
        fileOperationsViewModel.performOperation(
            FileOperation.MoveOperation(
                listOfFilesToMove = files,
                targetFolder = null,
                replace = replace,
                isUserLogged = eu.opencloud.android.presentation.authentication.AccountUtils.getCurrentOpenCloudAccount(this) != null,
            )
        )
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to rename
     * a file.
     *
     * @param uiResult - UIResult wrapping the file that was renamed
     */
    private fun onRenameFileOperationFinish(
        uiResult: UIResult<OCFile>
    ) {
        when (uiResult) {
            is UIResult.Loading -> {
                showLoadingDialog(R.string.wait_a_moment)
            }

            is UIResult.Success -> {
                dismissLoadingDialog()

                val details = secondFragment
                uiResult.data?.id?.let { fileId ->
                    if (details != null && uiResult.data == details.file) {
                        val updatedRenamedFile = storageManager.getFileById(fileId)
                        file = updatedRenamedFile
                        details.onFileMetadataChanged(updatedRenamedFile)
                        updateToolbar(updatedRenamedFile)
                    }
                }
            }

            is UIResult.Error -> {
                dismissLoadingDialog()

                showErrorInSnackbar(R.string.rename_server_fail_msg, uiResult.error)

                if (uiResult.getThrowableOrNull() is SSLRecoverablePeerUnverifiedException) {
                    showUntrustedCertDialogForThrowable(uiResult.getThrowableOrNull())
                }
            }
        }
    }

    private fun onSynchronizeFileOperationFinish(
        uiResult: UIResult<SynchronizeFileUseCase.SyncType>
    ) {
        when (uiResult) {
            is UIResult.Success -> {
                when (uiResult.data) {
                    SynchronizeFileUseCase.SyncType.AlreadySynchronized -> {
                        if (fileWaitingToPreview != null) {
                            startPreview(fileWaitingToPreview)
                            fileWaitingToPreview = null
                        } else {
                            showMessageInSnackbar(R.id.list_layout, getString(R.string.sync_file_nothing_to_do_msg))
                        }
                    }

                    is SynchronizeFileUseCase.SyncType.ConflictDetected -> {
                        val showConflictActivityIntent = Intent(this, ConflictsResolveActivity::class.java)
                        showConflictActivityIntent.putExtra(ConflictsResolveActivity.EXTRA_FILE, file)
                        startActivity(showConflictActivityIntent)
                    }

                    is SynchronizeFileUseCase.SyncType.DownloadEnqueued -> {
                        fileWaitingToPreview?.let {
                            showMessageInSnackbar(R.id.list_layout, getString(R.string.new_remote_version_found_msg))
                            startSyncThenOpen(it)
                            fileWaitingToPreview = null
                        } ?: showMessageInSnackbar(R.id.list_layout, getString(R.string.download_enqueued_msg))
                    }

                    SynchronizeFileUseCase.SyncType.FileNotFound -> {
                        /** Nothing to do atm. If we are in details view, go back to file list */
                    }

                    is SynchronizeFileUseCase.SyncType.UploadEnqueued -> {
                        showMessageInSnackbar(R.id.list_layout, getString(R.string.upload_enqueued_msg))
                    }
                    null -> { /* Nothing to do */ }
                }
            }

            is UIResult.Error -> {
                if (fileWaitingToPreview != null) {
                    startPreview(fileWaitingToPreview)
                    fileWaitingToPreview = null
                }
                when (uiResult.error) {
                    is AccountNotFoundException -> {
                        showRequestAccountChangeNotice(getString(R.string.sync_fail_ticker_unauthorized), false)
                    }

                    is UnauthorizedException -> {
                        launch(Dispatchers.IO) {
                            val credentials = AccountUtils.getCredentialsForAccount(MainApp.appContext, account)

                            launch(Dispatchers.Main) {
                                if (credentials is OpenCloudBearerCredentials) { // OAuth
                                    showRequestRegainAccess()
                                } else {
                                    showRequestAccountChangeNotice(getString(R.string.auth_failure_snackbar), false)
                                }
                            }
                        }
                    }

                    is CertificateCombinedException -> {
                        showUntrustedCertDialogForThrowable(uiResult.error)
                    }

                    else -> {
                        showMessageInSnackbar(R.id.list_layout, getString(R.string.sync_fail_ticker), SNACKBAR_DURATION_ERROR_MS)
                    }
                }
            }

            is UIResult.Loading -> {
                /** Not needed at the moment, we may need it later */
            }
        }
    }

    private fun openShortcutFileInBrowser(file: OCFile) {
        val url = extractUrlFromFile(file.storagePath.toString())
        val urlFormat = formatUrl(url!!)
        val message = getString(R.string.open_shortcut_description)
        val messageTextView = TextView(this).apply {
            text = message
            setPadding(0, 70, 0, 30)
            setTextColor(ContextCompat.getColor(this@FileDisplayActivity, R.color.theme_color_on_surface))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }

        val urlTextView = TextView(this).apply {
            text = urlFormat
            setTextColor(ContextCompat.getColor(this@FileDisplayActivity, R.color.theme_color_on_surface))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            addView(urlTextView)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(62, 0, 62, 70)
            addView(messageTextView)
            addView(scrollView)
        }

        val dialog = AlertDialog.Builder(this@FileDisplayActivity)
            .setTitle(getString(R.string.open_shortcut_title))
            .setView(layout)
            .setPositiveButton(R.string.drawer_open) { view, _ ->
                urlFormat?.let {
                    goToUrl(urlFormat)
                }
                view.dismiss()
            }
            .setNegativeButton(R.string.share_cancel_public_link_button) { view, _ ->
                view.dismiss()
            }
            .setCancelable(true)
            .create()
        dialog.show()
    }

    private fun extractUrlFromFile(filePath: String): String? {
        val file = File(filePath)
        if (file.exists()) {
            val lines = file.readLines()
            for (line in lines) {
                if (line.startsWith("URL=")) {
                    return line.substringAfter("URL=").trim('"')
                }
            }
        }
        return null
    }

    private fun formatUrl(url: String): String {
        var formattedUrl = url
        if (!url.startsWith(PROTOCOL_HTTP) && !url.startsWith(PROTOCOL_HTTPS)) {
            formattedUrl = PROTOCOL_HTTPS + url
        }
        return formattedUrl
    }

    private fun onSynchronizeFolderOperationFinish(
        uiResult: UIResult<Unit>
    ) {
        when (uiResult) {
            is UIResult.Success -> {
                // Nothing to handle when synchronizing a folder succeeds
            }

            is UIResult.Error -> {
                when (uiResult.error) {
                    is UnauthorizedException -> {
                        launch(Dispatchers.IO) {
                            val credentials = AccountUtils.getCredentialsForAccount(MainApp.appContext, account)
                            launch(Dispatchers.Main) {
                                if (credentials is OpenCloudBearerCredentials) { // OAuth
                                    showRequestRegainAccess()
                                } else {
                                    showRequestAccountChangeNotice(getString(R.string.auth_failure_snackbar), false)
                                }
                            }
                        }
                    }

                    is CertificateCombinedException -> {
                        showUntrustedCertDialogForThrowable(uiResult.error)
                    }

                    else -> {
                        showMessageInSnackbar(R.id.list_layout, getString(R.string.sync_fail_ticker), SNACKBAR_DURATION_ERROR_MS)
                    }
                }
            }

            is UIResult.Loading -> {
                /** Not needed at the moment, we may need it later */
            }
        }
    }

    private fun onCapabilitiesOperationFinish(uiResult: UIResult<OCCapability>) {
        if (uiResult is UIResult.Success) {
            val capabilities = uiResult.data
            capabilities?.versionString?.let { capabilitiesVersionString ->
                // FIXME: We should check productversion not versionString for second parameter:
                val openCloudVersion = OpenCloudVersion(capabilitiesVersionString, capabilitiesVersionString)
                if (!openCloudVersion.isServerVersionSupported) {
                    Timber.d("Server version not supported")
                    showRequestAccountChangeNotice(getString(R.string.server_not_supported), true)
                }
            }
        }
    }

    private fun requestForDownload(file: OCFile) {
        val downloadFileUseCase: DownloadFileUseCase by inject()

        val uuid = downloadFileUseCase(DownloadFileUseCase.Params(account.name, file)) ?: return

        WorkManager.getInstance(applicationContext).getWorkInfoByIdLiveData(uuid).observeWorkerTillItFinishes(
            owner = this,
            onWorkEnqueued = {
                showMessageInSnackbar(R.id.list_layout,
                    message = String.format(getString(R.string.downloader_download_enqueued_ticker), file.fileName)
                )
            },
            onWorkRunning = { progress -> Timber.d("Downloading - Progress $progress") },
            onWorkSucceeded = {
                CoroutineScope(Dispatchers.IO).launch {
                    if (file.mimeType == MIMETYPE_TEXT_URI_LIST) {
                        waitingToOpen = storageManager.getFileByPath(file.remotePath, file.spaceId)
                        launch(Dispatchers.Main) {
                            openShortcutFileInBrowser(waitingToOpen!!)
                        }
                    } else if (file.id == waitingToSend?.id) {
                        waitingToSend = storageManager.getFileByPath(file.remotePath, file.spaceId)
                        sendDownloadedFile()
                    } else if (file.id == waitingToOpen?.id) {
                        waitingToOpen = storageManager.getFileByPath(file.remotePath, file.spaceId)
                        openDownloadedFile()
                    }
                }
            },
            onWorkFailed = {
                showMessageInSnackbar(R.id.list_layout,
                    message = String.format(getString(R.string.downloader_download_failed_ticker), file.fileName)
                )
                if (file.id == waitingToSend?.id) {
                    waitingToSend = null
                } else if (file.id == waitingToOpen?.id) {
                    waitingToOpen = null
                }
            },
            onWorkCancelled = {
                if (file.id == waitingToSend?.id) {
                    waitingToSend = null
                } else if (file.id == waitingToOpen?.id) {
                    waitingToOpen = null
                }
            },
        )
    }

    private fun sendDownloadedFile() {
        waitingToSend?.let {
            sendDownloadedFilesByShareSheet(listOf(it))
        }

        waitingToSend = null
    }

    private fun openDownloadedFile() {
        waitingToOpen?.let {
            openOCFile(it)
        }

        waitingToOpen = null
    }

    /**
     * Requests the download of the received [OCFile] , updates the UI
     * to monitor the download progress and prepares the activity to send the file
     * when the download finishes.
     *
     * @param file [OCFile] to download and preview.
     */
    private fun startDownloadForSending(file: OCFile) {
        waitingToSend = file
        requestForDownload(file)
        val hasSecondFragment = secondFragment != null
        updateFragmentsVisibility(hasSecondFragment)
    }

    /**
     * Requests the download of the received [OCFile] , updates the UI
     * to monitor the download progress and prepares the activity to open the file
     * when the download finishes.
     *
     * @param file [OCFile] to download and preview.
     */
    private fun startDownloadForOpening(file: OCFile) {
        waitingToOpen = file
        requestForDownload(file)
        val hasSecondFragment = secondFragment != null
        updateFragmentsVisibility(hasSecondFragment)
    }

    /**
     * Opens the image gallery showing the image [OCFile] received as parameter.
     *
     * @param file Image [OCFile] to show.
     */
    fun startImagePreview(file: OCFile) {
        val showDetailsIntent = Intent(this, PreviewImageActivity::class.java)
        showDetailsIntent.putExtra(EXTRA_FILE, file)
        showDetailsIntent.putExtra(EXTRA_ACCOUNT, account)
        startActivity(showDetailsIntent)

    }

    /**
     * Stars the preview of an already down audio [OCFile].
     *
     * @param file                  Media [OCFile] to preview.
     * @param startPlaybackPosition Media position where the playback will be started,
     * in milliseconds.
     */
    fun startAudioPreview(file: OCFile, startPlaybackPosition: Int) {
        val mediaFragment = PreviewAudioFragment.newInstance(
            file,
            account,
            startPlaybackPosition,
            true
        )
        setSecondFragment(mediaFragment)
        updateToolbar(file)
        setFile(file)
    }

    /**
     * Stars the preview of an already down video [OCFile].
     *
     * @param file                  Media [OCFile] to preview.
     * @param startPlaybackPosition Media position where the playback will be started,
     * in milliseconds.
     */
    fun startVideoPreview(file: OCFile, startPlaybackPosition: Int) {
        val videoActivityIntent = Intent(this, PreviewVideoActivity::class.java)
        videoActivityIntent.putExtra(PreviewVideoActivity.EXTRA_FILE, file)
        videoActivityIntent.putExtra(PreviewVideoActivity.EXTRA_ACCOUNT, account)
        videoActivityIntent.putExtra(PreviewVideoActivity.EXTRA_PLAY_POSITION, startPlaybackPosition)
        startActivity(videoActivityIntent)
    }

    /**
     * Stars the preview of a text file [OCFile].
     *
     * @param file Text [OCFile] to preview.
     */
    fun startTextPreview(file: OCFile) {
        val textPreviewFragment = PreviewTextFragment.newInstance(
            file,
            account
        )
        setSecondFragment(textPreviewFragment)
        updateToolbar(file)
        setFile(file)
    }

    /**
     * Chooses the suitable method to preview a file [OCFile].
     *
     * @param file File [OCFile] to preview.
     */
    private fun startPreview(file: OCFile?) {
        file?.let {
            when {
                PreviewTextFragment.canBePreviewed(file) -> {
                    startTextPreview(file)
                }

                PreviewAudioFragment.canBePreviewed(file) -> {
                    startAudioPreview(file, 0)
                }
            }
        }
    }

    /**
     * Requests the synchronization of the received [OCFile],
     * updates the UI to monitor the progress and prepares the activity
     * to preview or open the file when the download finishes.
     *
     * @param file [OCFile] to sync and open.
     */
    private fun startSyncThenOpen(file: OCFile) {
        if (file.mimeType == MIMETYPE_TEXT_URI_LIST) {
            openOrDownloadShortcutFile(file)
        } else {
            navigateToDetails(account = account, ocFile = file, syncFileAtOpen = true)
//        fileWaitingToPreview = file
//        fileOperationsViewModel.performOperation(FileOperation.SynchronizeFileOperation(file, account.name))
            updateToolbar(file)
            setFile(file)
        }
    }

    private fun openOrDownloadShortcutFile(file: OCFile) {
        if (file.isAvailableLocally) {
            waitingToOpen = storageManager.getFileByPath(file.remotePath, file.spaceId)
            openShortcutFileInBrowser(waitingToOpen!!)
        } else {
            requestForDownload(file)
        }
    }

    private fun navigateToDetails(account: Account, ocFile: OCFile, syncFileAtOpen: Boolean) {
        val detailsFragment = FileDetailsFragment.newInstance(
            fileToDetail = ocFile,
            account = account,
            syncFileAtOpen = syncFileAtOpen
        )
        setSecondFragment(detailsFragment)
    }

    private fun navigateTo(newFileListOption: FileListOption, initialState: Boolean = false) {
        // account can be null if it was removed while the activity was still visible.
        // swapToDefaultAccount (called from onRestart/onNewIntent) launches the login
        // wizard asynchronously, but we can't navigate without an account.
        if (account == null) return
        val previousFileListOption = fileListOption
        when (newFileListOption) {
            FileListOption.ALL_FILES -> {
                if (isLightUser) {
                    file = null
                    fileListOption = newFileListOption
                    initAndShowEmptyPersonalSpace()
                    updateToolbar(null)
                } else {
                    if (previousFileListOption != newFileListOption || initialState) {
                        file = storageManager.getRootPersonalFolder()
                        fileListOption = newFileListOption
                        mainFileListFragment?.updateFileListOption(newFileListOption, file) ?: initAndShowListOfFiles(newFileListOption)
                        updateToolbar(file)
                    } else {
                        browseToRoot()
                    }
                }
            }

            FileListOption.SPACES_LIST -> {
                if (previousFileListOption != newFileListOption || initialState) {
                    file = null
                    initAndShowListOfSpaces()
                    updateToolbar(null)
                }
            }

            FileListOption.SHARED_BY_LINK -> {
                if (previousFileListOption != newFileListOption || initialState) {
                    val rootFolderForShares = storageManager.getRootSharesFolder()
                    val personalFolder = storageManager.getRootPersonalFolder()
                    if (rootFolderForShares == null && personalFolder?.spaceId != null) {
                        fileListOption = newFileListOption
                        initAndShowListOfShares()
                        updateToolbar(null)
                    } else {
                        file = rootFolderForShares ?: personalFolder
                        fileListOption = newFileListOption
                        mainFileListFragment?.updateFileListOption(newFileListOption, file) ?: initAndShowListOfFiles(newFileListOption)
                        updateToolbar(null)
                    }
                }
            }

            FileListOption.AV_OFFLINE -> {
                if (previousFileListOption != newFileListOption || initialState) {
                    file = storageManager.getRootPersonalFolder()
                    fileListOption = newFileListOption
                    mainFileListFragment?.updateFileListOption(newFileListOption, file) ?: initAndShowListOfFiles(newFileListOption)
                    updateToolbar(file)
                }
            }
        }
    }

    override fun navigateToOption(fileListOption: FileListOption) {
        navigateTo(fileListOption)
    }

    private fun getMenuItemForFileListOption(fileListOption: FileListOption?): Int = when (fileListOption) {
        FileListOption.SPACES_LIST -> R.id.nav_spaces
        FileListOption.SHARED_BY_LINK -> R.id.nav_shared_by_link_files
        FileListOption.AV_OFFLINE -> R.id.nav_available_offline_files
        else -> R.id.nav_all_files
    }

    override fun optionLockSelected(type: LockType) {
        manageOptionLockSelected(type)
    }

    private fun startListeningToOperations() {
        onDeepLinkManaged()

        fileOperationsViewModel.copyFileLiveData.observe(this, Event.EventObserver {
            onCopyFileOperationFinish(it)
        })
        fileOperationsViewModel.moveFileLiveData.observe(this, Event.EventObserver {
            onMoveFileOperationFinish(it)
        })
        fileOperationsViewModel.removeFileLiveData.observe(this, Event.EventObserver {
            onRemoveFileOperationResult(it)
        })
        fileOperationsViewModel.renameFileLiveData.observe(this, Event.EventObserver {
            onRenameFileOperationFinish(it)
        })
        fileOperationsViewModel.syncFileLiveData.observe(this, Event.EventObserver {
            onSynchronizeFileOperationFinish(it)
        })
        fileOperationsViewModel.refreshFolderLiveData.observe(this, Event.EventObserver {
            onSynchronizeFolderOperationFinish(it)
        })
        fileOperationsViewModel.syncFolderLiveData.observe(this, Event.EventObserver {
            onSynchronizeFolderOperationFinish(it)
        })
    }

    override fun onCurrentFolderUpdated(newCurrentFolder: OCFile, currentSpace: OCSpace?) {
        updateToolbar(newCurrentFolder, currentSpace)
        file = newCurrentFolder
    }

    override fun onFileClicked(file: OCFile) {
        when {
            PreviewImageFragment.canBePreviewed(file) -> {
                // preview image - it handles the sync, if needed
                startImagePreview(file)
            }

            PreviewTextFragment.canBePreviewed(file) -> {
                setFile(file)
                fileWaitingToPreview = file
                fileOperationsViewModel.performOperation(FileOperation.SynchronizeFileOperation(file, account.name))
            }

            PreviewAudioFragment.canBePreviewed(file) -> {
                setFile(file)
                fileWaitingToPreview = file
                fileOperationsViewModel.performOperation(FileOperation.SynchronizeFileOperation(file, account.name))
            }

            PreviewVideoActivity.canBePreviewed(file) && !WorkManager.getInstance(this).isDownloadPending(account, file) -> {
                // Available offline but not downloaded yet, don't initialize streaming
                if (!file.isAvailableLocally && file.isAvailableOffline) {
                    // sync file content, then open with external apps
                    startSyncThenOpen(file)
                } else {
                    // media preview
                    startVideoPreview(file, 0)
                }

                // If the file is already downloaded sync it, just to update it if there is a
                // new available file version
                if (file.isAvailableLocally) {
                    fileOperationsViewModel.performOperation(FileOperation.SynchronizeFileOperation(file, account.name))
                }
            }

            else -> {
                startSyncThenOpen(file)
            }
        }
        fileOperationsViewModel.setLastUsageFile(file)
    }

    override fun onShareFileClicked(file: OCFile) {
        fileOperationsHelper.showShareFile(file)
    }

    override fun initDownloadForSending(file: OCFile) {
        startDownloadForSending(file)
    }

    override fun cancelFileTransference(files: ArrayList<OCFile>) {
        transfersViewModel.cancelTransfersRecursively(files, account.name)
    }

    override fun setBottomBarVisibility(isVisible: Boolean) {
        showBottomNavBar(isVisible)
    }

    override fun uploadFromCamera() {
        filesUploadHelper?.uploadFromCamera(REQUEST_CODE__UPLOAD_FROM_CAMERA)
    }

    override fun uploadShortcutFileFromApp(shortcutFilePath: Array<String>) {
        requestUploadOfFilesFromFileSystem(shortcutFilePath)
    }

    override fun uploadFromFileSystem() {
        val action = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            setType(ALL_FILES_SAF_REGEX).addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(
            Intent.createChooser(action, getString(R.string.upload_chooser_title)),
            REQUEST_CODE__SELECT_CONTENT_FROM_APPS
        )
    }

    private fun handleDeepLink() {
        intent.data?.let { uri ->
            fileOperationsViewModel.handleDeepLink(uri, getCurrentOpenCloudAccount(baseContext).name)
        }
    }

    private fun handleShortcutIntent() {
        val sc = eu.opencloud.android.presentation.files.addtohomescreen.FolderShortcutHelper
        if (intent?.action != sc.ACTION_OPEN_SHORTCUT) return

        val shortcutRemotePath = intent?.getStringExtra(sc.EXTRA_SHORTCUT_FOLDER_REMOTE_PATH)
        val shortcutSpaceId = intent?.getStringExtra(sc.EXTRA_SHORTCUT_FOLDER_SPACE_ID)
        val shortcutAccountName = intent?.getStringExtra(sc.EXTRA_SHORTCUT_FOLDER_ACCOUNT)

        shortcutFolderToNavigate = null
        intent?.removeExtra(sc.EXTRA_SHORTCUT_FOLDER_REMOTE_ID)
        intent?.removeExtra(sc.EXTRA_SHORTCUT_FOLDER_REMOTE_PATH)
        intent?.removeExtra(sc.EXTRA_SHORTCUT_FOLDER_SPACE_ID)
        intent?.removeExtra(sc.EXTRA_SHORTCUT_FOLDER_ACCOUNT)

        if (shortcutRemotePath != null &&
            !shortcutAccountName.isNullOrBlank() &&
            shortcutAccountName != account?.name
        ) {
            val targetAccount = eu.opencloud.android.presentation.authentication.AccountUtils
                .getOpenCloudAccountByName(this, shortcutAccountName)
            if (targetAccount != null) {
                MainApp.initDependencyInjection()
                // Recreate account-scoped state instead of delivering another intent to this instance.
                finish()
                startActivity(Intent(this, FileDisplayActivity::class.java).apply {
                    action = sc.ACTION_OPEN_SHORTCUT
                    putExtra(EXTRA_ACCOUNT, targetAccount)
                    putExtra(sc.EXTRA_SHORTCUT_FOLDER_REMOTE_PATH, shortcutRemotePath)
                    putExtra(sc.EXTRA_SHORTCUT_FOLDER_SPACE_ID, shortcutSpaceId)
                    putExtra(sc.EXTRA_SHORTCUT_FOLDER_ACCOUNT, shortcutAccountName)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } else {
                showMessageInSnackbar(
                    R.id.list_layout,
                    getString(R.string.add_to_home_screen_shortcut_folder_missing)
                )
            }
            return
        }

        if (shortcutRemotePath != null && account != null) {
            val file = storageManager.getFileByPath(shortcutRemotePath, shortcutSpaceId)
            if (file != null) {
                shortcutFolderToNavigate = file
            } else {
                showMessageInSnackbar(
                    R.id.list_layout,
                    getString(R.string.add_to_home_screen_shortcut_folder_missing)
                )
            }
        }
    }

    private fun navigateToShortcutFolder() {
        val folder = shortcutFolderToNavigate ?: return
        shortcutFolderToNavigate = null
        fileListOption = FileListOption.ALL_FILES
        setFile(folder)
        cleanSecondFragment()
        initAndShowListOfFiles(fileListOption)
        refreshListOfFilesFragment()
        updateToolbar(folder)
        setCheckedItemAtBottomBar(getMenuItemForFileListOption(fileListOption))
    }

    private fun onDeepLinkManaged() {
        collectLatestLifecycleFlow(fileOperationsViewModel.deepLinkFlow) {
            it?.getContentIfNotHandled()?.let { uiResult ->
                when (uiResult) {
                    is UIResult.Loading -> {
                        showLoadingDialog(R.string.deep_link_loading)
                    }

                    is UIResult.Success -> {
                        intent?.data = null
                        dismissLoadingDialog()
                        uiResult.data?.let { it1 -> manageItem(it1) }
                    }

                    is UIResult.Error -> {
                        dismissLoadingDialog()
                        if (uiResult.error is FileNotFoundException) {
                            showMessageInSnackbar(
                                R.id.list_layout,
                                message = getString(R.string.deep_link_user_no_access),
                                duration = SNACKBAR_DURATION_ERROR_MS
                            )
                            changeUser()
                        } else {
                            showMessageInSnackbar(
                                R.id.list_layout,
                                message = getString(
                                    if (uiResult.error is DeepLinkException) {
                                        R.string.invalid_deep_link_format
                                    } else {
                                        R.string.default_error_msg
                                    }
                                ),
                                duration = SNACKBAR_DURATION_ERROR_MS
                            )
                        }
                    }
                }
            }
        }
    }

    private fun changeUser() {
        val currentUser = getCurrentOpenCloudAccount(this)
        val usersChecked = intent?.getStringArrayListExtra(KEY_DEEP_LINK_ACCOUNTS_CHECKED) ?: arrayListOf()
        usersChecked.add(currentUser.name)
        eu.opencloud.android.presentation.authentication.AccountUtils.getAccounts(this).forEach {
            if (!usersChecked.contains(it.name)) {
                MainApp.initDependencyInjection()
                val i = Intent(
                    this,
                    FileDisplayActivity::class.java
                )
                i.data = intent?.data
                i.putExtra(EXTRA_ACCOUNT, it)
                i.putExtra(KEY_DEEP_LINK_ACCOUNTS_CHECKED, usersChecked)
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(i)
            }
        }
    }

    private fun manageItem(file: OCFile) {
        setFile(file)
        account = eu.opencloud.android.presentation.authentication.AccountUtils.getOpenCloudAccountByName(this, file.owner)

        if (file.isFolder) {
            refreshListOfFilesFragment()
        } else {
            initFragmentsWithFile()
            onFileClicked(file)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean =
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (findViewById<View>(R.id.opencloud_app_bar).hasFocus()) {
                findViewById<View>(R.id.left_fragment_container).requestFocus()
            }
            true
        } else {
            super.onKeyUp(keyCode, event)
        }

    companion object {
        private const val TAG_LIST_OF_FILES = "LIST_OF_FILES"
        private const val TAG_LIST_OF_SPACES = "LIST_OF_SPACES"
        private const val TAG_SECOND_FRAGMENT = "SECOND_FRAGMENT"

        private const val KEY_WAITING_TO_PREVIEW = "WAITING_TO_PREVIEW"
        private const val KEY_SYNC_IN_PROGRESS = "SYNC_IN_PROGRESS"
        private const val KEY_WAITING_TO_SEND = "WAITING_TO_SEND"
        private const val KEY_UPLOAD_HELPER = "FILE_UPLOAD_HELPER"
        private const val KEY_FILE_LIST_OPTION = "FILE_LIST_OPTION"
        const val MIMETYPE_TEXT_URI_LIST = "text/uri-list"
        const val KEY_DEEP_LINK_ACCOUNTS_CHECKED = "DEEP_LINK_ACCOUNTS_CHECKED"

        private const val CUSTOM_DIALOG_TAG = "CUSTOM_DIALOG"

        private const val PREFERENCE_NOTIFICATION_PERMISSION_REQUESTED = "PREFERENCE_NOTIFICATION_PERMISSION_REQUESTED"
        const val PREFERENCE_CLEAR_DATA_ALREADY_TRIGGERED = "PREFERENCE_CLEAR_DATA_ALREADY_TRIGGERED"
        const val ALL_FILES_SAF_REGEX = "*/*"

        const val ACTION_DETAILS = "eu.opencloud.android.ui.activity.action.DETAILS"

        const val REQUEST_CODE__SELECT_CONTENT_FROM_APPS = REQUEST_CODE__LAST_SHARED + 1
        const val REQUEST_CODE__MOVE_FILES = REQUEST_CODE__LAST_SHARED + 2
        const val REQUEST_CODE__COPY_FILES = REQUEST_CODE__LAST_SHARED + 3
        const val REQUEST_CODE__UPLOAD_FROM_CAMERA = REQUEST_CODE__LAST_SHARED + 4
        const val RESULT_OK_AND_MOVE = RESULT_FIRST_USER
        const val PROTOCOL_HTTPS = "https://"
        const val PROTOCOL_HTTP = "http://"
    }
}
