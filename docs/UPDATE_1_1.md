# Near 1.1 camera and text sharing

This update keeps the Java/Android Views app and its existing reader and storage. It adds two entry points: Record in Near and the Android plain-text share target.

## Recording directly in Near

CameraActivity places CameraX PreviewView behind the custom ReaderView. Preview and VideoCapture share the selected camera lifecycle; the recorder encodes the camera stream, so reader text and controls never become video content. Front-facing recording is preferred, with a back-camera fallback when no front camera exists. Supported 1080p and 720p choices come from the active camera's capabilities.

The Record button requests microphone permission before a sound take. Denial presents a deliberate silent-recording choice. Camera access is required for the preview; the floating reader can still be used without it.

The existing ScrollSession owns script movement. A recorder Start event begins scrolling, while Pause script leaves the camera recording. Stop, backgrounding, or rotation finalizes the take. Metadata checks require a video track and nonzero duration before the app calls a file saved; missing audio is reported. An interrupted but playable output is identified as partial. ReviewActivity uses Android VideoView to play the last take and grants only that content URI when the user shares it.

MediaStore owns public Movies/Near recordings on Android 10+. Older supported versions use app-specific storage and FileProvider sharing. Near does not request broad media or filesystem access.

The reader's camera appearance disables the opaque focus band and bottom-color fade used by the external floating reader. Panel background and letter alpha are separate values. The settings sheet keeps a reset action available even if the letters are made transparent. The old editor and external-overlay appearance are preserved.

## Sharing a script into Near

ShareTextActivity is exported with an ACTION_SEND filter for text/plain. It accepts only that action and type, reads EXTRA_TEXT and an optional subject, normalizes line endings, and presents editable plain text. It does not interpret markup, open a URL, read an attachment, or start recording.

The incoming payload is checked against the existing 100,000-character limit before editable text layout. Oversized shares are explicitly rejected with instructions to share a shorter selection; nothing is silently truncated or saved. The Save action checks the edited text again and creates a new ScriptStore entry only after confirmation. Cancel creates nothing. Android state restoration preserves edits across recreation, and the saved flag prevents repeated saving by the same review screen.

MainActivity refreshes its library when returning, because another activity can now create a script while it is stopped. The previous draft is flushed on pause using the existing save path.

## Verification approach

SharedTextTest exercises paragraph normalization, empty/Unicode-whitespace rejection, exact size limits, bounded titles, and literal URL handling. NewFeaturesTest exercises actual share review, cancellation, recreation, unsupported data, video/audio metadata, independent script pause, and background finalization. DeviceFlowTest retains the original editor and external-reader checks. See [VERIFICATION.md](../VERIFICATION.md) for the evidence from this release.

The emulator's camera scene and audio are simulated. These checks do not establish physical Pixel lens quality, real microphone sound, heat, or long-take performance. Make a short real take before relying on a recording.

Voice-follow, cloud accounts, background filming, and a video-editing suite are outside this update.
