Near 1.0.0 is the first public, signed APK preview of the offline Android teleprompter.

## Install

Download `near-teleprompter-1.0.0.apk` from the Assets below on your Android phone. Open it in Files and allow that app to install APKs if Android asks. Open Near and allow **Display over other apps** when prompted. Then use the reader over your normal video camera.

Android 8.0+ is the intended baseline. The main device target is Pixel 7 Pro. Use the front camera, drag the reader close to the lens, start recording in the camera, and then press Play on the reader.

## Included

- Local script library and UTF-8 text import/export.
- Text-size and pace controls, three window widths, and a draggable reader.
- Countdown, pause/resume, swipe to reposition text, and restart.
- Light/dark editor, larger-system-text layouts, and notification controls.
- A complete YouTube-style build tutorial and a plain-text narration script.
- MIT-licensed source and GitHub Actions build/release workflows.

Near requests no internet, camera, microphone, or storage permission. The camera app owns video and audio recording. Voice-follow scrolling is deferred.

## Validation and limits

Build, JVM tests, Android Lint, and release-signature checks pass. Two device-flow tests passed on an Android 15 Pixel 7 Pro emulator. Native editor/overlay layouts were inspected in light/dark mode, portrait/landscape, and system font scale 1.3.

The actual Pixel 7 Pro and Pixel Camera were not connected. Do a short real recording to check overlay handling, audio, placement, and scrolling before a longer take. Emulator camera evidence uses AOSP Camera with a synthetic scene.

This release uses a dedicated release key. If you installed the earlier debug build, export your scripts before uninstalling it and installing this APK. Uninstalling removes local scripts; a different signing key cannot update over that build.

`SHA256SUMS.txt` contains the APK's SHA-256 checksum. `TUTORIAL.md` and `tutorial-voiceover.txt` are included below as downloads and also live in the source repository.
