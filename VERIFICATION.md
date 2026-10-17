# Verification

Tested on 17 October 2026.

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

The earlier device-flow tests and screenshot review tested the same application behavior in the debug build. They do not establish physical Pixel compatibility or constitute an instrumentation run against the release variant. The release build does not change camera/overlay behavior; it changes signing and debuggability. APKs published by CI are built again from the tagged source, signed with the same private identity, and have their own published checksum.
