# Near 1.2: offline voice-follow and update alerts

Separate reader-panel and letter opacity already shipped in 1.1 and remain independent. This release adds English voice-follow to the built-in camera and release checking to the editor/Updates screen. The README contains the complete feature history and implementation breakdown.

## Offline recognition

The Android platform speech-recognition preference for offline use is implementation-dependent, so Near bundles Vosk Android 0.3.75 and the small US-English 0.15 model instead. Neither scripts nor microphone samples are sent to a speech service. The pinned model is downloaded at build time and copied from APK assets to private no-backup storage at first use. Preparation and decoding run away from the UI thread.

The existing CameraX recorder and synchronized audio/video encoder are preserved. A foreground AudioRecord client feeds Vosk under the same application UID and CAMCORDER microphone policy. Native audio-policy silencing and CameraX audio errors are checked; unavailable input pauses recognition and any video-audio interruption is reported. Fixed pace is always available. Voice-follow is deliberately limited to Near's camera; the separate floating-reader mode does not compete with another camera application's microphone.

ScriptMatcher uses a local edit-distance search around the current position, with minimum matching-word requirements and bounded lookahead/backtracking. It normalizes punctuation, case, and apostrophes, processes short partial/final phrases, and holds position for silence or unrelated words. Repeating a nearby sentence can move back; manual seeking changes the search anchor. ReaderView converts the matched character position into a laid-out line, and frame callbacks animate that line into the reading position.

Record starts recognition, Pause script releases its microphone without stopping video, and leaving/backgrounding/rotation stops capture. Mode changes are selected before a take. Rehearsal through Follow voice uses the microphone without saving a video. English is the initial language; names, numbers, accents, noise, and large departures from the script can require manual recovery. Real microphone accuracy, memory pressure, heat, and long takes need physical-device testing.

## Update delivery

The editor makes a bounded HTTPS request to the official GitHub latest-release endpoint at most daily. A failed attempt adds a short cooldown rather than repeatedly retrying every time the editor resumes. Manual checks are available in Updates. Successful results are cached; failure retains known update information and does not block reading or recording.

Only stable numeric version tags, the expected repository release page, and the expected uploaded APK asset are accepted. Metadata cannot choose an arbitrary download host. The UI opens a constructed official GitHub release URL in the browser; there is no APK downloader, package installer permission, forced update, or silent installation inside Near.

Background notifications are opt-in. Enabling them schedules Android JobScheduler work requiring network and a non-low battery, persisted across reboot. Notifications require Android permission and an enabled channel, are issued once per version, and are deferred while the camera or floating reader is visible. Disabled notifications still allow manual checking and an editor notice. Timing is controlled by Android and is not instant push delivery.

The app therefore adds INTERNET and RECEIVE_BOOT_COMPLETED permissions. Only public release metadata is requested; GitHub receives normal connection metadata. Core prompting, recording, and voice-follow remain offline. Existing users need to install this release once manually; older installed APKs cannot gain notification code remotely.

## Build and attribution

The `prepareVoiceModel` Gradle task downloads `vosk-model-small-en-us-0.15.zip` and requires SHA-256 `30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498` before packaging its assets. An already-downloaded archive can be supplied with `-PnearVoiceModelZip=/absolute/path/to/archive.zip`. Recognition uses JNA 5.18.1. The model/native binaries increase APK size and private storage use. See [third-party notices](../THIRD_PARTY_NOTICES.md) and [verification](../VERIFICATION.md).

Primary implementation references: [Vosk models](https://alphacephei.com/vosk/models.html), [Vosk Android](https://github.com/alphacep/vosk-android-demo), [Android input sharing](https://developer.android.com/media/platform/sharing-audio-input), [JobScheduler](https://developer.android.com/reference/android/app/job/JobScheduler), and [GitHub release metadata](https://docs.github.com/en/rest/releases/releases#get-the-latest-release).
