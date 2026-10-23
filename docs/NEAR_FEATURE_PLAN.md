# Near camera and prompting feature plan

Prepared for Muhammad on 23 October 2026

Historical proposal, written before implementation. The camera, opacity controls, and incoming-text sharing are now covered by [the 1.1 implementation notes](UPDATE_1_1.md). Voice-follow remains proposed; the plan below is not a list of shipped capabilities.

## Recommendation and scope

I recommend adding a built-in camera, adjustable transparency, Share text into Near, and offline voice-follow. The camera and transparency controls should lead the update. Voice-follow should ship after its recording and speech-tracking prototype passes real Pixel tests.

This plan covers those four features. It describes proposed behavior and implementation; none of these additions is implemented or validated by this document. The existing floating mode remains an option for recording through Pixel Camera.

### Are these good additions

| Addition | My answer | Why it fits Near | Relative effort |
| --- | --- | --- | --- |
| Camera inside Near | Yes | Combines framing, recording, and prompting in one interface | Medium to high |
| Separate panel and text opacity | Yes | Lets you see your framing through the script while keeping the words readable | Low to medium |
| Share text into Near | Yes | Removes the copy and paste steps when bringing in a script | Low |
| Offline voice-follow | Yes with a prototype first | Lets delivery set the pace, but needs reliable audio capture and word matching | High |

### What the update would feel like

Open a script, choose Record in Near, adjust the reader over the live camera view, and press Record. A countdown prepares the take. Near records the camera image and microphone audio while the script advances. The reader and controls are visible on your screen and excluded from the saved video.

There would be two scrolling choices: Fixed pace, using the existing speed setting, and Voice follow, available once its local model and recording path are ready. Pausing your speech pauses the script in voice mode; the video continues recording.

### The proposed build order

First, prove that recording and local speech recognition can use a reliable shared audio path. Then build the camera screen with fixed scrolling and transparency controls. Add text sharing as a separate small change. Finally, connect voice-follow to the proven recording backend and test the complete flow.

Current source baseline: Near 1.0.0, commit c1e4100. Near currently uses Java 17, Android Views, a custom ReaderView, and local script storage. It has no camera or microphone permission. This plan keeps that codebase and adds components around it.

<!-- pagebreak -->

## Camera layout based on your references

The references place the camera image behind a rounded text panel near the top of the screen. A small toolbar provides reading and camera controls, and the bottom area contains the primary recording action. I would carry that arrangement into Near with Android controls and room for the Pixel camera cutout.

<!-- reference_images -->

### The proposed screen

- A live camera preview fills the screen. The front camera opens by default for self-recording; camera switching is available before a take.
- A movable, resizable reader sits near the front lens. A narrow reading width reduces side-to-side eye travel. Its position stays outside the camera cutout and system controls.
- A compact toolbar exposes supported video quality, camera switching, text size, and reader settings. Secondary settings open in a sheet so they do not permanently cover your face.
- The bottom area has a large Record or Stop button, a separate script Play or Pause control, and elapsed recording time. Restart text is available while paused.

### What opacity means here

There are two different controls. Background opacity changes how much of the camera you can see through the panel. Text opacity changes how solid the letters look. Keeping those settings separate is essential: making the entire panel view transparent at once would also fade its words and controls.

Suggested starting values are 50 percent background opacity and 100 percent text opacity. Those are design proposals to test on the phone, with a one-tap reset. The Record and Stop controls always remain clear.

### How the references guide the design

The second reference is the closer model for the live camera experience. The first also shows a colored backdrop, which I would treat as a possible reading-only screen rather than a mandatory camera background. We would use Near's own colors and icons. A screen-level text overlay can improve lens proximity, but it cannot guarantee perfect eye contact.

<!-- pagebreak -->

## Built in camera implementation

**Recommendation: Yes.** This is the most useful structural change for your proposed interface. It gives Near direct control of the preview, recording button, reader placement, and timing. The first release should prioritize a dependable talking-head recording over a large camera settings menu.

### How I would build it

1. Add a native CameraActivity and a RecordingController that own the capture lifecycle. Add Record in Near alongside the existing Open over camera action. Keep scripts and settings shared between the two modes.
2. Start the normal recording backend with CameraX Preview and VideoCapture. Pin matching stable CameraX dependencies after a compatibility build. The official release page currently lists 1.6.2 as stable. [1] [2]
3. Use PreviewView as the camera layer and place ReaderView and controls above it in an ordinary view hierarchy. Capture the camera stream directly so UI text, highlights, and buttons never enter the encoded video. Do not use screen recording.
4. Request CAMERA on entry and RECORD_AUDIO when recording with sound. Camera denial returns to the editor. If microphone permission is denied, offer an explicit silent-video option or permission retry; voice-follow stays unavailable.
5. Start with supported 1080p recording and a tested 30 fps target, with a lower-quality fallback when needed. Query capabilities for each lens. Add 4K and other combinations only after testing them; do not promise Pixel Camera's processing or stabilization behavior.
6. Save to MediaStore on Android 10 and later. On Android 8 and 9, initially save privately and offer file-picker export or FileProvider sharing. This avoids broad storage access. [3]

### Recording behavior

Use explicit states: Ready, Countdown, Starting, Recording, Saving, Saved, and Error. Cancel cancels the countdown. A recording indicator and timer begin when the recorder reports Start, rather than when the button is first touched. Disable repeated start commands and show Saving until finalization succeeds. [2]

Keep recording and scrolling as separate states. Script Pause pauses the reader only. Stop ends the take and stops prompting. Reaching the script's end holds the last text while recording continues until Stop. Lock quality and camera switching during a take. Distinguish a mirrored selfie preview from the saved video's orientation.

For the first release, leaving the camera screen or locking the phone should stop and finalize the take safely. Continuing to film in the background would be a separate feature. Playback must work before Near reports a video as saved.

**Ready when:** front and rear takes save with audible, synchronized sound; UI never appears in exported frames; permission denials, interrupted takes, and low storage produce clear outcomes without losing existing scripts.

<!-- pagebreak -->

## Transparent reader implementation

**Recommendation: Yes.** This is central to the reference layout. It lets you keep checking your framing while reading, and it can later improve the existing floating mode as well.

### The controls I would add

| Control | Proposed behavior |
| --- | --- |
| Background opacity | 0 to 100 percent for the reader's backing panel |
| Text opacity | 0 to 100 percent for the letters independently |
| Reader size and placement | Resize and drag within the usable camera area |
| Text size and spacing | Reuse the size setting and add adjustable line spacing |
| Reset appearance | Restore a readable panel, solid letters, and default placement |

Very low text opacity can make a script difficult to read. Keep the settings entry and reset action visible even when letters are completely transparent. Show changes live before and during a take. Collapse settings during delivery without hiding the recording status or Stop control.

### How I would change the current reader

Add backgroundAlpha and textAlpha to PrompterSettings, with defaults that preserve readability for existing installations. Set the panel drawable's alpha separately from the TextPaint alpha. Do not call setAlpha on the entire container, which would fade every child.

The current ReaderView paints a solid background, a focus band, a bottom color gradient, and a countdown scrim. All four need review. For transparency, replace the solid bottom-color gradient with a fade applied to the text layer, or remove it in the first version. Otherwise the lower part of the panel would stay opaque even when the background slider is reduced. Make the focus band optional and give it a controlled alpha.

Keep the countdown readable on its own layer. Store width and position relative to the usable preview area, so rotation and different screen sizes can clamp the panel correctly. Reserve separate drag and resize handles so a text-scrub gesture does not accidentally move the whole reader.

Use a shared appearance model for CameraActivity and PrompterService. The in-app panel needs no Display over other apps permission; the existing external floating window still does. For that external window, respect Android's obscuring-opacity limits and test touch behavior outside its bounds. Visual transparency does not automatically make a window touch-through. [12]

**Ready when:** panel and letters adjust independently; reset always restores a usable reader; no opaque fade remains at zero panel opacity; landscape, dark mode, large text, and the Pixel cutout work; saved videos remain clean.

<!-- pagebreak -->

## Offline voice follow behavior

**Recommendation: Yes, after a prototype.** It addresses your original wish to speak naturally instead of chasing a fixed scroll speed. The first supported use should be English in Near's own camera or rehearsal screen. External-camera voice-follow would need a separate compatibility solution.

### What it should do

As you read, Near estimates your position in the script and moves the matching phrase toward the reading line. Silence holds the text. Repeating a recent sentence can move the position back after a confident match. Going off script holds the current place until Near finds a reliable match again. Fixed pace remains available at any time.

Show a small state label such as Listening, Following, or Find your place. If recognition becomes uncertain, stop advancing and offer tap-to-resume from a selected sentence. Avoid jumping far ahead because of a single common word. A recognition failure must not stop the camera or corrupt its audio.

### The speech engine

Prototype with Vosk's Android and Java support because it accepts streamed audio for local recognition. Its published small English model is approximately 40 MB; the model list identifies that model as Apache 2.0. Small models also add runtime memory use, which must be measured during recording. [4] [5]

Bundle the selected English model with an offline-capable installation, or provide an explicit local model import. Do not make a first-run network download a hidden requirement. Retain the dependency and model license notices, validate extracted files, and show whether the model is ready.

Test with your own accent, speaking style, names, and recording room. A published model benchmark does not establish accuracy for your voice. Select the engine only after comparing tracking behavior, heat, memory, and battery impact on the Pixel. Avoid using a generic SpeechRecognizer call as an assumption of guaranteed offline, continuous recognition. Android's reference warns about network-backed implementations and continuous use. [6]

### How I would track the words

Normalize a copy of the script into tokens while retaining each token's character offsets in the original text. Match recent recognized phrases against a bounded region around the current script position, allowing small omissions, repeats, and recognition errors. Require several matching words and a confidence margin before committing a position change.

Map the chosen character offset back to a StaticLayout line in ReaderView. Smooth movement toward that line, with limits on sudden jumps. Keep token matching separate from drawing, so font size and panel width changes do not invalidate the spoken position. Add targeted tests for repeated phrases, numbers, punctuation, pauses, and manual repositioning.

<!-- pagebreak -->

## Recording and voice follow must share audio reliably

Putting the camera inside Near removes dependence on another camera app, but it does not automatically expose the recorder's microphone samples to a speech engine. Two independent microphone consumers are not a reliable architecture to assume. Android documents additional restrictions when separate apps capture audio. [7]

### The intended audio design

One microphone capture supplies timestamped PCM, meaning raw audio samples. A recording path encodes those samples for the video, while a separate bounded worker receives a copy for speech recognition. Recognition may fall behind without blocking video or audio writing. If it becomes overloaded, drop stale recognition work and show a tracking warning.

<!-- audio_diagram -->

CameraX Recorder is a suitable starting point for normal video. Its public builder does not provide a documented general-purpose raw-audio callback for this split. I would verify the exact pinned API and source before committing to an integration route. Starting a second recorder is not the fallback plan. [8]

If a supported shared-audio integration is unavailable, the voice-enabled recording backend would use Camera2, AudioRecord, MediaCodec, and MediaMuxer behind the same RecordingController. One AudioRecord supplies the encoder and recognizer. Video uses an encoder surface, and encoded video and audio tracks are combined into an MP4. Timestamp normalization and audio sample counts must preserve synchronization. This is substantially more engineering than the ordinary CameraX path. [9] [10] [11]

### The prototype that decides the approach

Before building the final camera UI, record a short and a ten-minute Pixel take while transcribing that same audio locally. Measure sound continuity, tracking delay, memory, heat, dropped frames, and end-to-end synchronization. Use claps near the beginning and end to detect drift. A provisional acceptance target is audio/video offset within 80 ms at both points, verified from the output file.

The prototype must recover from recognition failure while continuing to save playable video with sound. If this backend is not ready, release the camera with fixed scrolling and ship voice-follow later. Do not publish a feature switch that records silent or unsynchronized takes.

Speech recognition runs only during the user-started session. It produces temporary matching data; no separate voice file or permanent transcript is needed. The video intentionally retains its recorded audio. A Stop or interrupted-session path releases capture and recognition resources.

<!-- pagebreak -->

## Share text into Near implementation

**Recommendation: Yes.** This is the simplest useful addition in this group and can be delivered independently of the camera. It should accept a script without replacing the script you are already editing.

### The user flow

In an app that supports sharing selected text, select the text and choose Share, then Near. Near opens a review screen containing the received text and an editable title. Tap Save as new script to add it to the local library, or Cancel to leave the library untouched. From the new script, choose Record in Near or Open over camera.

Some apps share only a page link. In that case Near should display and identify the received link; it should not silently fetch an article or claim to have imported its contents. Offline text sharing requires the sending app to provide the actual text.

### How I would build it

1. Add an exported ShareTextActivity with an ACTION_SEND intent filter for text/plain and the DEFAULT category. Android uses these declarations to offer the app as a share target. No internet or storage permission is required for inline shared text. [13]
2. Parse Intent.EXTRA_TEXT as a CharSequence, turn it into plain text, and optionally use EXTRA_SUBJECT as a proposed title. Preserve paragraph breaks, normalize line endings, and reject empty or malformed input. Treat shared content as data and do not render active HTML or run links.
3. Enforce ScriptStore's current 100000-character limit before saving. If the received text is longer, show the limit and let the user edit or cancel; do not silently truncate it.
4. Keep the incoming draft separate from ScriptStore.current. On confirmation, call ScriptStore.create so it gets a new ID, becomes the selected script, and saves through the existing persistence path.
5. Handle cold launches, later share intents, rotation, and returning from the background without creating duplicate scripts. An incoming share during recording finalization should wait for that operation to complete and must not replace the active recording's script or start another take.

### What is included first

The first version accepts plain text only. PDF, Word, OCR, webpage extraction, and multiple attachments are separate import features. An optional PROCESS_TEXT action could later make Near appear directly in Android's selected-text menu where the sending app supports it.

**Ready when:** sharing from a notes app, browser selection, and chat selection works where each source offers real text; Cancel creates nothing; Save creates exactly one new script; an existing unsaved draft survives; empty, oversized, and unsupported content have clear outcomes; all of this works in airplane mode with local source content.

<!-- pagebreak -->

## Delivery stages and checks

These stages are proposed work, not delivery dates. Camera and audio hardware findings determine how large the voice-follow stage becomes.

| Stage | Work | Evidence needed to proceed |
| --- | --- | --- |
| 1 | Camera and shared-audio prototypes | Pixel recordings remain audible and synchronized while local recognition runs |
| 2 | Camera screen with fixed pace and independent opacity | Reference layout works on-device and exports clean video |
| 3 | Share text into Near | New scripts import once without overwriting current work |
| 4 | Voice matching and recording integration | Silence, repeats, off-script speech, and recognizer failure behave predictably |
| 5 | Public update | Device checks, signed upgrade, documentation, and download verification pass |

### The main release checks

- Compare front and rear camera output in portrait and landscape. Verify the saved video orientation and test mirror behavior explicitly.
- Read with a 0, 50, and 100 percent panel background and several text opacity levels. Check that the UI is absent from the actual video frames.
- Test a normal short take and at least a ten-minute take in airplane mode. Review both ends for audio drift, and measure heat and tracking delay.
- Speak with pauses, repeated phrases, paraphrases, unfamiliar names, and ordinary room noise. Tracking should hold or request repositioning when uncertain.
- Deny and revoke permissions, interrupt with a call, lock the screen, switch apps, and simulate low storage. Stop or fail visibly and preserve already-saved files.
- Import shared text with a cold app and a warm app, rotate the review screen, cancel, and retry. Verify no duplicates or accidental replacement.
- Install the signed update over 1.0.0 with the same private signing identity. Verify existing scripts and settings survive, and repeat the old external-overlay flow.

### Offline behavior and permission changes

Text sharing and fixed scrolling remain entirely local. Built-in video adds camera and microphone permissions, requested when needed. Camera-only operation should not require the external-window permission. Voice-follow needs a ready local model and no cloud service. Keep network access out of the core app if the model is bundled or imported locally.

Update the current privacy text and service declarations accurately: the app can no longer generally say that it never uses the camera or microphone. The external floating service can still keep its present role when it does not capture either. Recordings remain local unless the user explicitly exports or shares them.

**My preferred first public update:** built-in recording, the transparent reader, and Share text into Near. Voice-follow joins that release only if its audio and matching checks pass; otherwise it follows as a separate update.

<!-- pagebreak -->

## Components and source references

### Proposed source changes

| Component | Responsibility |
| --- | --- |
| CameraActivity and camera layout | Live preview, layered reader, permissions, and camera controls |
| RecordingController and recording backends | Recording states, lifecycle, output files, and errors |
| ReaderView and PrompterSettings | Independent alpha values, layout, focus styling, and saved appearance |
| VoiceFollowEngine and ScriptMatcher | Local speech results, token alignment, and stable reader targets |
| AudioCaptureCoordinator | Shared timestamped audio and bounded recognition work |
| ShareTextActivity | Incoming text review and one-time script creation |
| MainActivity and manifest | Mode entry points, share declarations, and feature permissions |

The names above are proposed components. Implementation should reuse the existing ScrollSession, ScriptStore, and reader behavior where they fit.

### Primary technical references

Checked on 23 October 2026. API documentation explains platform behavior; the architecture and release choices in this plan are recommendations that still require implementation and device testing.

1. [CameraX releases](https://developer.android.com/jetpack/androidx/releases/camera)
2. [CameraX video capture architecture](https://developer.android.com/media/camera/camerax/video-capture)
3. [Android shared media storage](https://developer.android.com/training/data-storage/shared/media)
4. [Vosk Android integration](https://alphacephei.com/vosk/android)
5. [Vosk models and licenses](https://alphacephei.com/vosk/models)
6. [Android SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer)
7. [Android audio input sharing](https://developer.android.com/media/platform/sharing-audio-input)
8. [CameraX Recorder builder](https://developer.android.com/reference/androidx/camera/video/Recorder.Builder)
9. [Android AudioRecord](https://developer.android.com/reference/android/media/AudioRecord)
10. [Android MediaCodec](https://developer.android.com/reference/android/media/MediaCodec)
11. [Android MediaMuxer](https://developer.android.com/reference/android/media/MediaMuxer)
12. [Android WindowManager opacity and touch rules](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_NOT_TOUCHABLE)
13. [Receiving shared data on Android](https://developer.android.com/training/sharing/receive)

The current Near source provides the baseline for the component review. Your two supplied images provide the layout references. This document does not claim that the new camera or voice-follow path has already passed Pixel hardware testing.
