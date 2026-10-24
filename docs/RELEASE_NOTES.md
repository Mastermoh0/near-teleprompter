Near 1.2.0 adds offline English voice-follow and optional update notifications.

## New

- **Voice follow · English** in the built-in camera's Reader settings. The bundled Vosk model recognizes speech on the phone; matching words move the reader and silence holds it still. Rehearse with Follow voice or start it with Record. Pause script leaves video recording active.
- Swipe to reposition if recognition loses your place. Turn voice-follow off to return to fixed pace. Voice-follow currently works in Near's built-in camera; the external floating reader retains fixed pace.
- **Updates** shows the installed version, checks the official GitHub release, displays release notes, and opens the official download page.
- **Optional update notifications** check roughly daily while online. Android controls timing; allow notifications and turn on Update notifications to enable background alerts. The editor also shows available updates. Alerts are deferred during camera/floating-reader sessions.

Separate panel and text opacity, built-in recording, and Share → Near were already included in 1.1 and remain available.

## Install or update

Download `near-teleprompter-1.2.0.apk` from Assets and install it over public Near 1.0 or 1.1. Keep Near installed to retain scripts. This release uses the same private signing identity. Older versions need this one manual installation before they can check for future updates.

The earlier personal debug APK uses another signature. Export scripts before uninstalling that build; uninstalling removes app-local data.

The APK is larger because the English speech model is included. No model download or account is required on the phone. Reading, recording, and voice-follow work offline. Update checks require internet, request only public release information, and do not upload scripts, speech, or videos. Near never silently downloads or installs an APK: you choose the download and approve Android's installation.

## Verification and limits

Release assembly, 26 JVM tests, and release Lint passed locally. Fifteen Android device-flow cases passed on an Android 15 emulator: the existing eight editor/overlay/camera/share cases and seven new release/voice cases. Tests decode an actual English speech fixture with the bundled model offline and exercise simultaneous voice/camera audio clients. Emulator camera and microphone inputs are simulated; this does not establish recognition accuracy or real recorded sound on a physical Pixel. Try a short take on your phone first. Fixed pace remains available.

See the repository README for the complete feature history, implementation breakdown, privacy changes, and usage instructions. `VERIFICATION.md` records evidence and its limits. The APK is signed locally; signing keys remain private. `SHA256SUMS.txt` contains its checksum. The original build tutorial, narration, and 1.2 implementation notes are also attached.
