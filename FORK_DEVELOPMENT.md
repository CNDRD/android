# Personal Android client fork

First milestone: make manual uploads visible and let the user control file grid density.

## Build and install

The `Fork UX APK and tests` workflow builds the unchanged starting commit and the feature branch as QA debug APKs. The feature job also runs `UploadFeedbackTrackerTest`. Download the `opencloud-feature-qa-debug` artifact from a successful workflow run and extract the APK. The existing QA flavor uses a separate package ID from the store app, so it can be installed alongside it.

Local commands (JDK 17 and Android SDK required):

```sh
./gradlew :opencloudApp:assembleQaDebug
./gradlew :opencloudApp:testQaDebugUnitTest --tests 'eu.opencloud.android.presentation.transfers.UploadFeedbackTrackerTest'
```

## Permanent signing for updates

Installable feature APKs require the repository Actions secret `OC_QA_SIGNING_JSON`. Its private JSON bundle contains `keystore_base64` (a PKCS12 keystore), `store_password`, `key_password`, and `key_alias`. Keep a private backup outside this repository. Never commit the bundle or upload it as a workflow artifact.

The workflow restores this key only on the feature job. The public certificate fingerprint is pinned in `.github/qa-signing-certificate.sha256`, and both the restored key and the finished APK must match it before publication. If the secret is absent, tests still run but no installable feature APK is published; the job summary explains setup. After adding the secret, rerun the feature job.

The package ID and account/provider authorities stay fixed. Each build uses version code `1000 + GITHUB_RUN_NUMBER`. Rerunning a workflow keeps its version code; subsequent workflow runs increase it. Use this workflow consistently for installable builds.

Moving from an earlier disposable debug key requires one final uninstall/reinstall. Subsequent APKs signed with this permanent key can update in place, retaining app data. Android still prompts to approve installation. The fixed package also avoids creating a new app entry for VPN inclusion; VPN behavior should be verified on the phone.

Other unit-test workflows may compile with disposable debug keys internally, but those APKs are not published as installable feature artifacts.

## Behavior

- Upload selection immediately shows “Preparing uploads…”.
- A button below the toolbar displays queued and uploading manual file counts. Tap it to open uploads, where the existing transfer details and retry controls remain available.
- New upload results show success/failure counts and a “View uploads” action. Existing history is not announced at launch. Automatic photo/video backups are excluded from these foreground messages.
- The sorting row beside the list/grid toggle offers “Grid density”: Automatic or 2–6 columns. Selecting a density switches to grid view and saves the setting. List view remains available.

## Device checks before merging

1. Upload one file, then several files; verify preparing, active counts, completion and destination contents.
2. Disconnect during an upload, inspect the uploads screen and retry; verify that failure and subsequent success are visible when the corresponding status changes.
3. Navigate to another folder while uploading. Rotate the phone. Reopen the app with old successful transfers; verify old results are not replayed.
4. Switch accounts and verify only the selected account's manual uploads are shown.
5. Select each grid density in portrait and landscape, navigate folders, restart the app, and switch between list/grid. Check filenames, thumbnails, menus and selection at dense settings.
6. Check the status banner in light/dark themes and with large text.

No device checks have been performed in the execution environment. Photo/video thumbnail improvements are intentionally a separate milestone; this change does not alter thumbnail requests or server behavior.
