# Verification

The original 1.0 checks below were completed on 17 October 2026. The 1.1 update record follows them.

## Build and package

- Java 17 / Gradle 8.13 / Android Gradle Plugin 8.13.0, compile and target SDK 36.
- `scripts/build.ps1 -Verify`: successful; APK generated.
- Android Lint: **0 errors**. Remaining warnings cover dependency updates and native widget/accessibility conventions; they are retained in `app/build/reports/lint-results-debug.html`.
- Initial personal APK Signature Scheme v2 verification: successful, one Android debug signer.
- Packaged permissions inspected with Android `aapt`: overlay, foreground service, special-use foreground service, notifications, and the AndroidX internal receiver permission. **No internet, camera, microphone, or storage permission.**
- Public release checksums are attached as `SHA256SUMS.txt` on GitHub Releases; the local release helper writes the same file into `releases/`.

## Automated checks

Six JVM tests passed:

- Equal scroll distance over one second at 60 Hz and 120 Hz.
- No position jump after pause/resume.
- Long frame stalls are capped to avoid skipping text.
- Stop at end and rewind to beginning.
- Clamp manual seek and resized content bounds.
- Word counting for empty text, line breaks, and Unicode spacing.

Two Android instrumentation tests passed on an Android 15 / API 35 x86_64 emulator configured as a Pixel 7 Pro:

- Script edits survive activity recreation and remain saved locally.
- The overlay remains active when the editor backgrounds, accepts play/pause, and closes.

The tests ran directly with AndroidJUnitRunner (`OK (2 tests)`). Gradle's device-discovery runner was slow in this Windows environment, so `scripts/test-device.ps1` provides the direct runner as a repeatable alternative.

## Native checks

- Emulator networking disabled during app use and device-flow tests.
- Light editor and settings, dark editor and settings with system font scale 1.3.
- Actual floating reader over the AOSP emulator video camera, including its settings controls.
- Countdown, scrolling, pause, and reader remaining visible over the launcher.
- Landscape reader and expanded controls captured from a rotated Android display.
- Final camera launch confirmed in a separate camera task; returning to Near brings the editor forward.

Native screenshots are in `docs/images/verification/`.

## Independent finish review

Disposition: **ship as a personal testing build**. The reviewer requested one spacing fix: 8 dp between the adjustment buttons and their rows, retaining 48 dp targets. That fix was rebuilt, recaptured in portrait and landscape at font scale 1.3, and scored resolved. The verdict covers that fix; it does not establish physical Pixel Camera compatibility.

## Hardware limits

The actual Pixel 7 Pro was not connected. Pixel Camera-specific overlay handling, lens placement, live audio, refresh-rate performance, and a real recorded take must still be checked on that phone. The emulator camera is AOSP Camera, using a synthetic scene; it is not Pixel Camera. Voice-follow scrolling is not implemented.

## Public APK preparation

The public 1.0.0 APK is a non-debuggable release build signed with a dedicated private 4096-bit RSA key. `assembleRelease`, all six `testReleaseUnitTest` cases, `lintRelease`, and APK signature verification passed locally. The release certificate SHA-256 fingerprint is `a629ab31e4f5431c19bfb87447841245fbc634f717fc01e2bd343388cbd324ab`.

The earlier device-flow tests and screenshot review tested the same application behavior in the debug build. They do not establish physical Pixel compatibility or constitute an instrumentation run against the release variant. Published APKs are signed locally with the private release identity; the optional CI release workflow remains disabled. Each published APK has its own checksum.

## Version 1.1 — 23 October 2026

### Build and package

- Java 17, Gradle 8.13, AGP 8.13.0, target/compile SDK 36; CameraX 1.6.2.
- Debug application and instrumentation APKs built successfully. Release assembly, release JVM tests, and release Lint completed successfully.
- Eleven release JVM tests passed: six scrolling/word-count checks and five incoming-text normalization, validation, size, and title checks.
- Release Lint: **0 errors, 16 warnings**. Warnings include dependency updates and native layout/widget conventions; they are not a claim of complete accessibility verification.
- The merged package requests camera and microphone access for built-in recording, alongside the existing overlay/service/notification permissions. CameraX adds `ACCESS_NETWORK_STATE`. The package has **no `INTERNET` or broad storage permission**.
- The non-debuggable 1.1.0 APK uses the same dedicated release certificate as public 1.0.0. APK Signature Scheme v2 verification passed; SHA-256 certificate fingerprint: `a629ab31e4f5431c19bfb87447841245fbc634f717fc01e2bd343388cbd324ab`.
- This OneDrive checkout encountered locked/generated-cache files. The local build used an ignored Gradle init script to put build output and caches in Windows temporary storage. No machine-specific paths or keys are required by the source project.

### Device flows and input boundary

The Android 15 / API 35 x86_64 emulator uses a Pixel 7 Pro display profile. Airplane mode was enabled and Wi-Fi/data disabled during testing. The original two editor/overlay tests and six new camera/share tests passed together (`OK (8 tests)`). After adding the early oversized-share guard, the six new tests passed again against the updated APK (`OK (6 tests)`, 87.952 seconds).

The new device tests cover:

- Shared paragraphs and edited titles surviving activity recreation, saving once as a new script while retaining the prior draft.
- Cancel leaving the library and selection unchanged.
- Empty or oversized shares remaining unsavable; oversized input is rejected before editable text layout.
- Unsupported attachment MIME types not being imported as text.
- A recorded file containing video and audio tracks with positive duration; pausing the script leaves recording active.
- Backgrounding the camera finalizing its output.

The exported share receiver accepts only `ACTION_SEND` with `text/plain`, treats URLs literally, and requires Save before creating a script. Source checks also confirmed the playback activity is private and FileProvider exposes only recording directories with per-URI grants. These are focused implementation checks, not a formal security audit.

### Native interaction evidence and limits

Manual emulator interaction exercised the actual Android share chooser, camera launch, recording/stop, script controls, settings, local playback, and rotation. Actual swipes independently changed panel opacity to 20 and text opacity to 50; Reset restored 55/100. Dragging the reader header moved the panel within the stage. Captures cover the light editor, camera ready/recording/settings/review, landscape camera at font scale 1.3, and light/dark shared-text review with large system text. These captures are in [docs/images/verification/v1.1](docs/images/verification/v1.1/). The camera's green block scene is Android's simulated input; it is labeled synthetic in published documentation.

The instrumentation runs used the debug variant. Separately, the signed public 1.0.0 APK was installed in a clean emulator test installation, a synthetic title/body were saved through the editor, and the signed 1.1.0 APK was installed over it with `adb install -r`. Both title and body survived, versionCode advanced from 1 to 2, and recording/local playback worked in the updated release build. The real phone was not altered.

The camera and audio input are simulated; an audio track does not verify real microphone sound. A physical Pixel 7 Pro was not connected. Real lens framing, microphone quality, heat, long takes, other Android versions, TalkBack navigation, and Pixel Camera-specific external-overlay behavior remain unverified. Voice-follow scrolling is deferred.

### Independent UI verdict

The fresh Impeccable reviewer requested two changes: readable system navigation against the camera screen and an updated design record. After the correction batch, the same reviewer scored both **resolved** and returned **ship** for those scored fixes. Idle, settings, recording, playback, and landscape captures show dark navigation symbols on a protected light edge, measured by the reviewer at approximately 8.80:1. The camera content stays dark; its navigation edge follows the system day/night theme. This verdict covers the two fixes, not a new full-surface or physical-device certification.
