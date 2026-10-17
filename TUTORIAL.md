# Build an Offline Android Teleprompter from Start to Finish

**A YouTube-style teaching and recording script for Near**

This is the full build story: product choices, tools, project structure, native UI, local storage, text rendering, scroll timing, overlays, permissions, camera handoff, testing, troubleshooting, signing, and public GitHub distribution.

**Audience:** beginners who can install tools and read basic Java. Explain unfamiliar Android terms before showing code. Plan roughly 45–60 minutes including demonstrations; add chapter timestamps after editing the actual recording.

**Recording instructions:** read the `Narration` sections aloud. Use `On screen` as the shot list. Code fences and checkpoints are teaching material, not words to read. [docs/tutorial-voiceover.txt](docs/tutorial-voiceover.txt) contains narration only and can be imported into Near.

**Evidence:** this project was built with Codex assistance using Java and native Android views. The emulator checks described here were performed; a physical Pixel 7 Pro/Pixel Camera recording was not. If you film that test later, label the actual device and Android version. Do not describe an emulator result as a physical-phone result.

**Source:** [Mastermoh0/near-teleprompter](https://github.com/Mastermoh0/near-teleprompter). The complete linked files are authoritative. Short snippets explain mechanisms; they are not an alternative incomplete app to paste over the full source.

## 1. Show the problem and the finished idea

### Narration

When I record a video, I want to look toward the camera. But if my script is on a laptop beside my phone, you can see my eyes move away from the lens. So I wanted a simple teleprompter directly on the phone.

The app we are building is called Near. You write a script, choose the text size and reading pace, and open a small reader over your existing camera app. You can move that window close to the front lens, start recording in the camera, and then press Play to scroll your words.

The installed app works offline. It does not upload the script, record the microphone, or take over the camera. Your camera app still does the recording. Near supplies the words and scrolling controls.

I will explain the complete implementation and how to produce an APK somebody else can install. We will also examine what was tested, because a successful build and a successful recording on a real phone are different things.

### On screen

Show editor, settings, and reader. Use native screenshots in `docs/images` if an emulator is unavailable. Label its camera scene synthetic. For a later real-phone demo, record and identify the actual device/version.

### Checkpoint

The viewer can describe the product: a locally scrolling window over an existing camera app.

## 2. Scope the product and choose the tools

### Narration

The first decision was to keep the existing camera. Building a recorder would add camera preview, video encoding, microphone access, file saving, and device-specific behavior. My immediate problem was reading near the lens, so a floating reader was the smaller product that solved it.

The second decision was manual auto-scroll. You set the pace and the text moves automatically. Voice following would need speech recognition, a way to match spoken words to the script, and testing while the camera uses the microphone. We are leaving it for later instead of pretending it is solved.

I used Java seventeen, Android views, XML layouts, and Material Components. Kotlin would also be a valid Android choice. Java suits this small app because the platform APIs are directly available and the scrolling logic can be tested without Android.

Codex helped create and refine the project. But the deliverable is ordinary source code, not a dependency on an AI service. The installed app does not call Codex or a language model. We still inspect the implementation, run tests, build the APK, and check the actual interface.

### On screen

Open [PRODUCT.md](PRODUCT.md) and [app/build.gradle](app/build.gradle). Point to Java 17, min SDK 26, target/compile SDK 36, Material Components, AppCompat, and JUnit. Explain that min SDK is the intended oldest API while target SDK opts into modern platform behavior.

### Checkpoint

Keep four boundaries visible: existing camera; offline storage; manual pace; no voice recognition yet.

## 3. Install tools and get a repeatable build

### Narration

Before changing code, I want a build I can repeat. The development computer used Windows and PowerShell. Java and the Android SDK were not already installed, so I added a setup script that keeps these tools inside the project.

Java compiles and runs the build tooling. The Android SDK supplies platform APIs, packaging tools, signing tools, and ADB. ADB installs the app on an emulator or connected testing phone. Gradle coordinates the build, and the Android Gradle Plugin teaches it how to build Android applications.

The setup script downloads tools from their official distribution locations and checks archive hashes before extraction. The Gradle wrapper pins a version and a distribution checksum. That gives another developer a documented starting point with the same tool versions.

The first setup and build need internet to download tools and libraries. That is different from runtime. Once installed, this app can run without internet.

### On screen

Start from a fresh clone, not a folder containing private signing files:

```powershell
git clone https://github.com/Mastermoh0/near-teleprompter.git
cd near-teleprompter
.\scripts\setup-tools.ps1
.\scripts\build.ps1 -Verify
```

Read [setup-tools.ps1](scripts/setup-tools.ps1) and [build.ps1](scripts/build.ps1). Use PowerShell 7 for the helpers. Android Studio users can open the root, install SDK 36/build tools 35.0.0, and use `gradlew.bat`. Do not change machine-wide execution policy just for the video; use the normal approved way to run reviewed local scripts.

### Checkpoint

The build succeeds and produces `releases/near-teleprompter-1.0.apk`. Reports are under `app/build/reports`.

## 4. Map the project before implementing features

### Narration

An Android project has several kinds of files. Gradle files explain how to build. The manifest declares components and permissions. Resources describe layouts, text, colors, and themes. Java files implement behavior.

The main activity is the editor screen. The prompter service owns the floating window. The reader view draws the script. A scroll-session class decides how far the text moves. Separate storage classes remember scripts and preferences.

This separation matters. Drawing, file import, notifications, storage, and timing should not become one giant method. Here I can test timing on the computer while the Android classes handle Android work.

If rebuilding from an empty folder, follow the chapters in order and create the corresponding files using the linked complete source. If you cloned the project, use this as a guided implementation walkthrough and make one small change at each checkpoint. The source is available so you do not have to reconstruct missing files from video frames.

### On screen

```text
settings.gradle / build.gradle / gradle.properties
gradle/wrapper/ + gradlew + gradlew.bat
app/build.gradle
app/src/main/AndroidManifest.xml
app/src/main/java/com/mastermoh/near/
  MainActivity.java
  ScriptStore.java
  PrompterSettings.java
  ScrollSession.java
  ReaderView.java
  IconButton.java
  PrompterService.java
app/src/main/res/
  layout/activity_main.xml
  values/strings.xml, colors.xml, themes.xml
  values-night/colors.xml
  drawable/ and xml/
app/src/test/ and app/src/androidTest/
scripts/
```

### Checkpoint

Ask which file changes for incorrect scroll timing and which changes for a color. The answers should differ.

## 5. Configure Gradle and the manifest

### Narration

Project-level Gradle settings declare where plugins and libraries come from. The app build declares its identity, Android API versions, Java version, and dependencies. The application ID identifies the installed app. The namespace is used for generated code and resources. They have the same value here, but different jobs.

The manifest declares the main activity as the launcher screen. It is exported so the Android launcher can open it. The service is not exported because other apps do not need to start our internal service directly.

Every permission should match a feature. We need display-over-other-apps permission, foreground-service permissions for the visible reader, and notification permission on Android versions that request it. We do not need camera or microphone permission because Near does not record either one. We do not request internet permission.

Modern Android requires an appropriate foreground-service type. Our visible reader does not fit camera, microphone, or data transfer. This implementation uses special use and describes the teleprompter use case in the manifest. That is part of the platform contract, not a way to bypass Android rules.

### On screen

Open [settings.gradle](settings.gradle), [build.gradle](build.gradle), [app/build.gradle](app/build.gradle), and [AndroidManifest.xml](app/src/main/AndroidManifest.xml). Highlight:

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Show the non-exported service and `android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE` explanation. Refer to [Android's service-type documentation](https://developer.android.com/develop/background-work/services/fgs/service-types#special-use).

### Checkpoint

Explain every requested permission and why recording permissions are absent.

## 6. Build the editor with native controls

### Narration

The editor uses a native layout: a title field, multiline script field, word count, reader preview, and settings for size, pace, width, and countdown. Material Components provides familiar Android buttons, sliders, switches, fields, and dialogs.

The action that opens the reader stays at the bottom while the main content scrolls. I can reach the next step without scrolling through the entire editor again. A close-reader action appears when the reader is open.

Shared color roles define light and dark themes instead of coloring each widget independently. The floating reader stays dark because it needs contrast over unpredictable camera imagery. Text follows system scaling, while small icons retain full touch targets.

We must also account for status and navigation bars, display cutouts, and the keyboard. Android can draw edge to edge. Assuming the entire rectangle is safe is a common source of clipped controls, so the activity applies the relevant window insets.

### On screen

Read [activity_main.xml](app/src/main/res/layout/activity_main.xml), [themes.xml](app/src/main/res/values/themes.xml), and light/dark color resources. Show the weighted `ScrollView` and separate bottom action container. In `MainActivity.onCreate`, explain `WindowInsets`.

Use a temporary theme-color edit as a small exercise, then restore it. Demonstrate light/dark and larger system text.

### Checkpoint

The script and controls remain usable. Interact with them; do not rely solely on a screenshot.

## 7. Save scripts and settings locally

### Narration

We remember two kinds of information. Scripts have an ID, title, and body. Reader preferences include text size, pace, width, countdown, and window position. Keeping these separate prevents settings from becoming entangled with the library.

For this small first version, I use app-private shared preferences. The library is serialized as JSON and scripts receive generated IDs. This is a simple local store, not a database or a synchronization system. A larger library with complex queries could justify a database later.

The editor updates its in-memory script as text changes. It waits briefly before saving and refreshing the preview, so each keystroke does not immediately trigger all that work. It also saves when the activity pauses, including when switching to the camera or recreating the screen.

Local storage is not an automatic backup. This version disables backup and excludes device transfer in its extraction rules. Uninstalling or clearing app data can remove scripts, so export exists for anything important.

### On screen

Read [ScriptStore.java](app/src/main/java/com/mastermoh/near/ScriptStore.java) and [PrompterSettings.java](app/src/main/java/com/mastermoh/near/PrompterSettings.java). Show the `TextWatcher`, 350-millisecond delayed refresh, and `onPause` save in `MainActivity`. Inspect [data_extraction_rules.xml](app/src/main/res/xml/data_extraction_rules.xml).

### Checkpoint

Edit a title/body, leave, and reopen. Do not uninstall as a persistence test without exporting first.

## 8. Implement a testable scroll state machine

### Narration

Before drawing moving text, define the position rules. ScrollSession keeps the current position, maximum distance, pixels-per-second speed, and playback state. It does not know about activities, cameras, or buttons.

Play enables movement. Pause disables movement and resets the frame clock. Restart pauses and returns to zero. Seek clamps a requested position into the valid range. Reaching the end stops playback.

The unit is distance per second, not distance per frame. Moving a fixed number of pixels every frame would make a one-hundred-twenty-hertz screen scroll about twice as fast as a sixty-hertz screen. Instead, we multiply speed by elapsed time between frames.

We also cap a very long interval. If Android stalls, I would rather briefly slow down than jump past a large section of the script. That is an explicit product choice protected by a test.

### On screen

Read [ScrollSession.java](app/src/main/java/com/mastermoh/near/ScrollSession.java), explaining:

```java
double elapsed = Math.min(0.1,
        (frameNanos - lastFrameNanos) / 1_000_000_000.0);
position = Math.min(limit, position + elapsed * pixelsPerSecond);
```

At 60 pixels/second, a tenth of a second moves six pixels. Explain why pause/play resets `lastFrameNanos`. Show the Unicode-whitespace-aware `wordCount` implementation.

### Checkpoint

The viewer can test positions with fake timestamps without launching Android.

## 9. Lay out the script as native text

### Narration

A string alone does not tell us where the lines are. Font size and window width change wrapping, total height, and the distance we can scroll.

TextPaint describes typeface, color, and size. Android's StaticLayout breaks the script into lines that fit the available width. We rebuild the layout when the script, size, or view width changes.

The reading limit is the top of the last line. That lets the final line reach the reading area and stay there instead of continuing into an empty window.

The pace is approximate words per minute. We estimate pixel speed using laid-out text height and word count. Blank lines, wrapping, and speaking style still affect how that feels, so the label says approximate and the pace remains adjustable.

### On screen

Read `rebuild`, `maxOffset`, and `pixelsPerSecond` in [ReaderView.java](app/src/main/java/com/mastermoh/near/ReaderView.java):

```java
layout = StaticLayout.Builder.obtain(
        text, 0, text.length(), textPaint, width)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(dp(5), 1.1f)
        .setIncludePad(false)
        .build();
```

```text
approximate pixel speed
  = text height × words per minute ÷ (word count × 60)
```

### Checkpoint

Change size and width. Lines reflow inside the reader. Empty text must not divide by zero.

## 10. Draw the reader and handle touch

### Narration

Drawing is mostly a coordinate problem. Clip the canvas to the reading area, move its origin upward by the scroll offset, and ask StaticLayout to draw the text. We do not edit or rebuild the string on every frame.

There is a fixed focus band near the top and a fade at the bottom. These establish a reading area and make upcoming text less visually competitive. They do not mean the app knows which word I am speaking.

A tap toggles playback. A vertical drag changes position. Android's touch-slop value distinguishes a deliberate drag from tiny finger movement during a tap. Scrubbing pauses the session so scrolling does not fight the finger.

The drawn buttons have forty-eight-by-forty-eight-dp targets and spoken labels, with space between adjustment buttons and rows. The reader also exposes accessibility scroll actions. A tiny icon should not require a tiny tap.

### On screen

Show `onDraw`, `onTouchEvent`, `performClick`, and accessibility actions in `ReaderView`. Explain:

```java
canvas.save();
canvas.clipRect(0, dp(12), getWidth(), getHeight());
canvas.translate(left, dp(12) - offset);
layout.draw(canvas);
canvas.restore();
```

Read [IconButton.java](app/src/main/java/com/mastermoh/near/IconButton.java). The play path and fade are reused rather than allocated on every draw.

### Checkpoint

Tap to pause, drag back, then tap again. Confirm that text scrubbing and moving the entire window are different interactions.

## 11. Add the floating window

### Narration

An activity is a screen inside our app. It does not stay visibly over another app when we switch to the camera. The reader is a separate window added with Android's WindowManager.

Its type is application overlay, which requires a special user-granted permission. The editor checks permission, explains its purpose, and opens Android settings. The user returns and starts the reader after enabling it.

The window is small and touchable. It does not take keyboard focus, and touches outside its area can reach the camera. It keeps the screen on while present. System screens and apps that deliberately hide overlays retain their own rules; our reader cannot force itself onto every possible screen.

The top bar is the drag handle. On touch down, remember the pointer and window positions. On movement, apply their difference, clamp it to the available screen, and update the layout. On release, save the position.

### On screen

Read `createWindow`, `readScreenSize`, `clampPosition`, and `updateWindow` in [PrompterService.java](app/src/main/java/com/mastermoh/near/PrompterService.java). Show `TYPE_APPLICATION_OVERLAY` and:

```java
WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
```

Use a launcher and configured test camera for the demo, not password/payment screens.

### Checkpoint

Drag the window, then scrub text separately. Camera controls outside it should remain reachable in the camera app being tested.

## 12. Own the overlay with a foreground service

### Narration

The window needs an owner that remains active when the editor backgrounds. That owner is a foreground service. Foreground does not mean the editor stays on screen. It means user-visible ongoing work with the required service declaration and notification.

We create a low-importance notification channel and a notification that can return to the editor, toggle playback, or close the reader. Its pending intents are explicit and immutable, so our app defines their destinations.

The visible editor starts the service after a user action and permission checks. We do not launch it automatically at boot. The service checks overlay permission again before adding the window because settings can change.

Cleanup is part of the feature. Destruction cancels countdown/frame callbacks, removes the window, unregisters the screen receiver, and removes the foreground notification. A close button that leaves invisible work running would be unfinished.

### On screen

Read service `onCreate`, `onStartCommand`, `notification`, and `onDestroy`. Explain `START_NOT_STICKY`: the app does not silently restore a stopped reader; the user starts it again. See [Android service-type documentation](https://developer.android.com/develop/background-work/services/fgs/service-types#special-use).

### Checkpoint

Close from the window and notification. Both the overlay and its ongoing notification should disappear.

## 13. Open the camera as a separate app

### Narration

The launch sequence is straightforward: save the script, reject empty text, check overlay permission, optionally request notifications, and start the service. Once its window is attached, open the camera.

We first try Google's camera package with the video-camera action. If unavailable, remove the package restriction and let Android resolve a compatible app. If no camera supports the action, keep the reader available and tell the user to open a camera manually.

We launch the camera in its own Android task. The camera should remain a separate app, and returning to Near from the launcher should bring back the editor. This was one of the details tightened during actual testing.

The handoff does not press Record. Pausing Near does not stop camera recording. Keeping that boundary clear avoids surprising behavior.

### On screen

Read `launchReader`, `openReaderAndCamera`, `waitForReader`, and `openCamera` in [MainActivity.java](app/src/main/java/com/mastermoh/near/MainActivity.java):

```java
Intent camera = new Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA)
        .setPackage("com.google.android.GoogleCamera")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
```

Show the bounded ready wait and `ActivityNotFoundException` fallback. Finish the camera's own first-run setup before an overlay demo; setup/permission screens may handle overlays differently.

### Checkpoint

Open the camera, then return via Near's launcher icon. Camera recording is controlled by the camera, script playback by Near.

## 14. Schedule frames, countdowns, and interruptions

### Narration

Choreographer drives animation. Android supplies a frame timestamp, ScrollSession updates its position, and ReaderView redraws at that position. We request another frame only while playing.

A handler drives the countdown once per second. When the count reaches zero, playback begins. Pausing cancels the pending countdown callback and clears its display. An old callback must not unexpectedly start scrolling later.

The app pauses when the screen turns off. On configuration changes such as rotation, it pauses and recalculates window geometry. Clamping keeps the window from disappearing outside the available display.

Changing font size can change the layout dramatically. This implementation pauses, rebuilds, and preserves an approximate fraction of progress. It is not word-perfect anchoring. Preserving the exact word during reflow would be a separate improvement.

### On screen

Show `frame`, `scheduleFrame`, `countdownTick`, `toggle`, `pause`, `changeSize`, and `onConfigurationChanged`. Explain Ready → Countdown → Scrolling → Paused/End. These describe the script, not camera recording.

### Checkpoint

Pause during countdown and wait: playback must stay paused. Lock/unlock and rotate the test device; controls must remain reachable.

## 15. Import and export selected text files

### Narration

Scripts often start elsewhere. Import and export use Android's document picker. The user selects a file or destination, and the app works through its content URI.

We do not scan every folder on the phone. The system picker grants access to the chosen document, so we avoid broad storage permission.

Files use UTF-eight. Import handles common line endings and a leading byte-order mark, enforces a hundred-thousand-character limit, and reports a useful error for unreadable input. Reading and writing run on an executor rather than deliberately blocking the UI thread.

The picker may show a cloud provider installed on the phone. That does not make Near a cloud-sync app. Choose local files for an offline workflow, and export important scripts before uninstalling or switching signing identities.

### On screen

Show `ACTION_OPEN_DOCUMENT`, `ACTION_CREATE_DOCUMENT`, `ContentResolver`, UTF-8 reader/writer, limits, and executor in `MainActivity`. Demonstrate with a small neutral local `.txt` file.

### Checkpoint

Import, edit, export, and inspect the text. Try invalid/oversized input and demonstrate recovery.

## 16. Test timing without a phone

### Narration

ScrollSession is plain Java, so JUnit can feed it exact timestamps and inspect exact positions. That makes the important timing rules easy to test without an emulator.

One test simulates a second at sixty frames per second and another at one hundred twenty. Both must travel the same distance. Another pauses for a long time and resumes; that pause must not become scrolling time.

We test frame stalls, reaching the end, restart, seek bounds, and word counting across whitespace. These protect behavior the user notices instead of merely repeating the implementation in test syntax.

All six original JVM tests passed. That proves these specific rules. It does not prove compatibility with every camera app or natural eye contact in a physical recording. Those need different checks.

### On screen

Read [ScrollSessionTest.java](app/src/test/java/com/mastermoh/near/ScrollSessionTest.java):

```powershell
.\scripts\gradle.ps1 @('testDebugUnitTest')
```

Or with configured Java/SDK:

```sh
bash gradlew testDebugUnitTest
```

### Checkpoint

Show the passing report. Optionally make a temporary fixed-pixels-per-frame edit to demonstrate the refresh-rate test failing, then restore the implementation.

## 17. Test the actual Android lifecycle and UI

### Narration

The development setup used a Pixel 7 Pro emulator profile running Android fifteen. Its image was AOSP Android. Its camera was the emulator camera, not Pixel Camera on physical hardware.

Two instrumentation tests checked lifecycle behavior. One edits a script, recreates the activity, and confirms the text remains. The other opens the overlay, backgrounds the editor, toggles playback, and closes it. Tests preserve and restore script and reader preferences.

Native screenshots cover light/dark themes, larger system text, the floating reader, expanded controls, and landscape. These were captured from Android, not recreated in a browser.

Emulator networking was disabled during app use and device checks. The manifest confirms Near does not request internet. These are useful results, but hardware-specific questions still require the actual phone.

### On screen

Read [DeviceFlowTest.java](app/src/androidTest/java/com/mastermoh/near/DeviceFlowTest.java), using a dedicated test installation:

```powershell
.\scripts\test-device.ps1 -Serial emulator-5554
```

Alternatively use `gradlew.bat connectedDebugAndroidTest`. Show [VERIFICATION.md](VERIFICATION.md). Explain that the direct runner checks the instrumentation result, not only ADB's exit code.

### Checkpoint

Record the actual device/API, font scale, and camera app used. Do not describe emulator evidence as a physical Pixel test.

## 18. Explain the problems and fixes from the real build

### Narration

The first problem was environmental: Java could not establish a loopback connection for Gradle inside the packaged Windows environment. The helpers resolved it by placing Java's temporary and Unix-domain socket files inside the project. That was a host-tool issue, not a reason to add internet access to the Android app.

Android Lint found concrete implementation issues too. The SDK path in local properties needed its Windows drive colon escaped. The custom button needed AppCompatButton. The internal status receiver needed compatibility registration with an explicit non-exported flag. A redundant text-break setting was removed after a constant annotation mismatch.

We tightened Material theme roles to keep selected controls in the green palette, kept the launch action visible, put the camera into its own task, and added gaps between adjustment targets without shrinking their touch areas.

The Gradle device runner was slow in this environment, so we added a direct ADB instrumentation runner and checked its actual result. Distinguish a tool failure, an app failure, and an untested assumption. Fix the real cause, then repeat the relevant check.

### On screen

Show temp settings in [gradle.ps1](scripts/gradle.ps1), SDK path escaping, `AppCompatButton`, `ContextCompat.registerReceiver`, theme roles, and 8-dp adjustment spacing. Show verified outcomes instead of recreating every historical failure.

### Checkpoint

The viewer understands the reason for each fix, not a suggestion to disable checks to get a successful build.

## 19. Build a signed public release

### Narration

The first APK was a personal debug build. For public downloads, we now produce a non-debuggable release with a dedicated signing identity.

Signing affects updates. A new APK needs an accepted signing identity to update the installed app. Publishing every version with a different key would break ordinary updates, so generate the release key once, keep it private, and back it up.

The helper creates a PKCS-twelve key and an ignored properties file. Passwords are generated locally and passed to the key tool through environment variables. Neither the key nor its passwords belong in Git, the tutorial video, or release downloads.

Gradle accepts that private file or four signing environment variables. Missing signing causes a clear release-build failure. The helper builds, tests, lints, verifies the APK signature, and generates a checksum.

The original debug APK and public APK have different keys. Export scripts before uninstalling the old build and installing the public one. Uninstalling can remove local scripts, and that consequence must be explained first.

### On screen

Read [release.ps1](scripts/release.ps1), signing in `app/build.gradle`, [.gitignore](.gitignore), and [RELEASING.md](docs/RELEASING.md).

```powershell
# Create your own release identity once:
.\scripts\release.ps1 -CreateKey

# Later builds use the same configured key:
.\scripts\release.ps1
```

Show the APK filename and verification result, never `.signing/release.properties` contents. Version 1.0.0 produces `releases/near-teleprompter-1.0.0.apk`.

### Checkpoint

Verify signature/checksum, confirm `.signing/` is ignored, and back up its key and properties privately.

## 20. Publish source people can reuse

### Narration

Publishing is more than uploading a folder. People need code, build instructions, a license explaining reuse, and a clear app download.

The original code and tutorial use the MIT license. The repository includes the wrapper, Java files, resources, tests, scripts, design notes, and this tutorial. It excludes SDK downloads, caches, local SDK paths, signing files, and generated APK binaries from Git history.

The APK goes on a GitHub Release. That keeps source history manageable and gives a phone user an obvious install path. Download the APK to use the app, not the source ZIP.

This public repository is Mastermoh0 slash near-teleprompter. For a fork, create your own repository, change the application ID, and use your own signing key. That also lets your fork coexist with upstream Near on one phone.

### On screen

Show README download links, [LICENSE](LICENSE), [CONTRIBUTING.md](CONTRIBUTING.md), and ignore rules. Explain these typical commands for a project without Git history. Authenticate `gh` normally first; never put tokens in remote URLs:

```sh
git init -b main
git add .
git commit -m "Build offline Android teleprompter and tutorial"
gh repo create YOUR_ACCOUNT/near-teleprompter --public --source=. --remote=origin
git push -u origin main
```

A clone already has Git history. Do not blindly initialize it or replace its remote. Use GitHub's fork flow or deliberately configure a destination you own.

### Checkpoint

Review staged files. `.signing`, `.android`, `.tools`, `.gradle`, and `local.properties` must be absent. Public visibility is a deliberate publication choice.

## 21. Automate checks and tagged APK releases

### Narration

There are two GitHub Actions workflows. Normal pushes and pull requests build debug code, run JVM tests, and run Android Lint without release secrets.

The first public release is signed locally and uploaded directly, so the private signing key stays on the development computer. The repository also includes an optional tag-release workflow, disabled by default. A maintainer can explicitly enable it after deciding to configure their own encrypted signing secrets.

When enabled, that workflow verifies the tag against the app version, restores the key into temporary runner storage, builds and checks the release, verifies its signature, and attaches files to a GitHub Release.

Assets include the APK, checksum, Markdown tutorial, and narration-only text. Temporary key material is removed afterward. Pull-request checks never receive these signing secrets.

This is direct APK distribution, not automatic Google Play submission or completion of platform developer-verification requirements. For every update, increment the version code, set the version name, and update release notes and test evidence for that actual release.

### On screen

Read [android.yml](.github/workflows/android.yml) and [release.yml](.github/workflows/release.yml). Show the disabled-by-default job condition and the `RELEASE_SIGNING_ENABLED` variable described in the release guide. Explain both publication paths. Do not transfer signing credentials as a routine part of pushing a public project. Tag the checked commit:

```sh
git tag v1.0.0
git push origin v1.0.0
```

Use a new version for an update, not an already published tag. Show completed Actions and public download assets after they exist.

For the local-signing path, create the release after pushing its tag:

```powershell
gh release create v1.0.0 releases/near-teleprompter-1.0.0.apk releases/SHA256SUMS.txt TUTORIAL.md docs/tutorial-voiceover.txt --verify-tag --title 'Near 1.0.0' --notes-file docs/RELEASE_NOTES.md
```

### Checkpoint

The release opens publicly and the APK downloads. Debug contributors do not need upstream signing credentials.

## 22. Install, check a real phone, and choose the next feature

### Narration

On Android, open the GitHub Release and download its versioned APK. Open it in Files, follow installation prompts, then open Near. Enter a script, choose a comfortable size and pace, and grant display-over-other-apps permission when asked.

Choose video and the front camera in your camera app. Move the reader close to the lens without covering necessary controls. Start recording in the camera, then start the reader. Pause Near, stop recording in the camera, and inspect the result.

The original build did not have a physical Pixel 7 Pro connected. The next hardware check is a short real recording: does Pixel Camera allow the window, is audio present, is the text readable, and does the eye line look natural? Also check offline use, rotation, a saved script after reopening, and closing the reader. A build log cannot answer these for us.

Possible next improvements include word anchoring during text reflow, richer script organization, and optional offline voice following. Voice following should earn its place through recognition and microphone-sharing tests. The manual product already has a clear job: keep the words on the phone near the lens while the camera does its own work.

You now have the source, a buildable project, a signed-installation path, tests, and public distribution. Extend one behavior at a time, explain the tradeoff, and test what the user will actually do.

### On screen

Show [the latest release](https://github.com/Mastermoh0/near-teleprompter/releases/latest), installation, and a short neutral script. Add actual physical test results/device versions to `VERIFICATION.md` only after performing them.

### Final filming checklist

- Narration, commands, and screen actions match the current source.
- No passwords, tokens, personal scripts, or private files appear.
- The synthetic emulator scene is labeled honestly.
- Viewers see a successful build and understand test limits.
- Download, source, tutorial, and license links work.
- Export/uninstall data loss is explained before debug-to-release migration.

## Primary references

- [Android foreground-service types](https://developer.android.com/develop/background-work/services/fgs/service-types#special-use)
- [Application-overlay window type](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY)
- [Android document picker](https://developer.android.com/training/data-storage/shared/documents-files)
- [Android Choreographer](https://developer.android.com/reference/android/view/Choreographer)
- [Android app signing](https://developer.android.com/studio/publish/app-signing)
- [Gradle wrapper](https://docs.gradle.org/8.13/userguide/gradle_wrapper.html)
- [GitHub repository creation](https://cli.github.com/manual/gh_repo_create)
- [GitHub releases](https://cli.github.com/manual/gh_release_create)
- [GitHub encrypted repository secrets](https://cli.github.com/manual/gh_secret_set)

See [VERIFICATION.md](VERIFICATION.md) for measured results and limits, and [RELEASING.md](docs/RELEASING.md) for signing and future updates. The exact pinned versions and complete implementation are in this repository.
