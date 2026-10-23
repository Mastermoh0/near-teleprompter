Near 1.1.0 adds recording inside the app and importing selected text through Android's Share menu.

## New

- **Record in Near** with a live front/rear camera preview, a translucent script reader, and capability-based 1080p/720p selection.
- Independent background and text opacity, movable reader, text-size and reading-pace controls.
- Record countdown, script pause that keeps the camera recording, and safe take finalization when leaving the camera.
- Local playback through **Review last take**, with explicit video sharing.
- **Share → Near** for plain text: review, edit, then save a new script. Existing scripts remain available. Oversized payloads are rejected before text layout; links are never fetched automatically.

The external-camera floating mode remains available. Voice-follow is not included in this release.

## Install or update

Download `near-teleprompter-1.1.0.apk` from Assets and open it on the phone. It uses the same signing identity as public 1.0.0, so it can update that installation. Keep the app installed when updating to retain scripts.

The earlier personal debug APK has a different signature. If you still use that build, export scripts before uninstalling it and installing the public APK. Uninstalling removes app-local data.

Built-in recording asks for camera and microphone access. If microphone access is declined, a silent take requires an explicit choice. External-camera mode still uses the camera app's own recording. There is no internet or broad storage permission.

On Android 10+, recordings save under Movies/Near. Android 8–9 uses app-specific storage; share those recordings before uninstalling Near.

## Verification and limits

The validation record for this release is in `VERIFICATION.md` in the source repository. Native screenshots and device-flow checks use an Android 15 emulator configured for Pixel 7 Pro dimensions. Camera images and audio inputs are simulated. They do not establish physical Pixel camera quality, real microphone sound, heat, or long-take reliability. Make a short recording on your actual phone first.

The APK is signed locally; its private signing key is not uploaded. `SHA256SUMS.txt` contains the public APK checksum. The original build tutorial, narration, and 1.1 implementation notes are included as release downloads.
