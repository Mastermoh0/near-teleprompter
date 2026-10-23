---
name: Near · Teleprompter
description: A native Android script workbench, camera, and floating reader close to the lens.
colors:
  light-primary: "#286341"
  light-on-primary: "#FFFFFF"
  light-container: "#D6ECDC"
  light-on-container: "#112D1C"
  light-surface: "#F7F8F2"
  light-surface-variant: "#E7EAE1"
  light-on-surface: "#1B211B"
  light-secondary: "#505D52"
  light-outline: "#707B70"
  dark-primary: "#A0D5B0"
  dark-on-primary: "#07381F"
  dark-container: "#244E34"
  dark-on-container: "#D6ECDC"
  dark-surface: "#111713"
  dark-surface-variant: "#252D26"
  dark-on-surface: "#E0E5DD"
  dark-secondary: "#B6C1B5"
  dark-outline: "#899589"
  reader-surface: "#111713"
  reader-text: "#F5F8F2"
  reader-accent: "#B4EDC4"
  reader-chrome: "#E3EFE5"
  reader-focus: "#1F3024"
  reader-outline: "#3F4F43"
  reader-play: "#31503A"
  camera-toolbar-scrim: "#E6111713"
  camera-controls-scrim: "#F2111713"
  camera-surface-low: "#19211B"
  camera-surface-lowest: "#0B110D"
  recording-stop: "#B3261E"
typography:
  headline:
    fontFamily: "sans"
  title:
    fontFamily: "sans"
  body:
    fontFamily: "sans"
  label:
    fontFamily: "sans"
  script-editor:
    fontFamily: "sans"
    fontSize: "17sp"
  reader:
    fontFamily: "sans-serif-medium"
    fontSize: "26sp"
    fontWeight: 500
  reader-chrome:
    fontFamily: "sans"
    fontSize: "12sp"
rounded:
  focus-band: "8dp"
  icon-control: "12dp"
  play-control: "14dp"
  floating-reader: "18dp"
spacing:
  tight: "4dp"
  control-inset: "6dp"
  compact: "8dp"
  small: "12dp"
  field: "16dp"
  switch-section: "20dp"
  page: "24dp"
  section: "28dp"
  scroll-end: "32dp"
components:
  button-primary-light:
    backgroundColor: "{colors.light-primary}"
    textColor: "{colors.light-on-primary}"
  button-primary-dark:
    backgroundColor: "{colors.dark-primary}"
    textColor: "{colors.dark-on-primary}"
  button-text-light:
    textColor: "{colors.light-primary}"
  button-outlined-light:
    textColor: "{colors.light-primary}"
  script-body-light:
    textColor: "{colors.light-on-surface}"
    typography: "{typography.script-editor}"
    padding: "{spacing.field}"
    height: "240dp"
  width-selected-light:
    backgroundColor: "{colors.light-container}"
    textColor: "{colors.light-on-container}"
  width-selected-dark:
    backgroundColor: "{colors.dark-container}"
    textColor: "{colors.dark-on-container}"
  reader-preview:
    backgroundColor: "{colors.reader-surface}"
    textColor: "{colors.reader-text}"
    typography: "{typography.reader}"
    height: "156dp"
  reader-window:
    backgroundColor: "{colors.reader-surface}"
    textColor: "{colors.reader-text}"
    rounded: "{rounded.floating-reader}"
  reader-play:
    backgroundColor: "{colors.reader-play}"
    textColor: "{colors.reader-chrome}"
    rounded: "{rounded.play-control}"
    height: "48dp"
  camera-reader:
    textColor: "{colors.reader-text}"
    typography: "{typography.reader}"
    rounded: "{rounded.floating-reader}"
    height: "220dp"
  camera-record:
    backgroundColor: "{colors.reader-accent}"
    textColor: "{colors.dark-on-primary}"
    height: "64dp"
  camera-stop:
    backgroundColor: "{colors.recording-stop}"
    textColor: "{colors.light-on-primary}"
    height: "64dp"
  shared-script-body-light:
    textColor: "{colors.light-on-surface}"
    typography: "{typography.script-editor}"
    height: "320dp"
---

# Design System: Near · Teleprompter

## Overview

**Creative North Star: "Native Android workbench"**

Near is a quiet, practical place to prepare words, read them close to the lens, and record a take. Warm surfaces, forest green actions, system typography, and familiar Material 3 controls give editing and setup a clear hierarchy. The script and camera framing are the main content; the interface stays compact and direct.

The signature is a movable dark reader with large warm-white text. In floating mode it has a steady first-line focus band and bottom fade; in Near's camera it becomes a translucent rounded panel so framing remains visible through the words. Script playback stays separate from recording. This is a code-led native interface built with Java 17, Android views, and Material Components 1.13.0. There is no image composition or generated artwork behind the UI.

**Key Characteristics:**

- Native Material 3 editing, camera controls, and settings.
- Warm light and forest dark editor themes.
- A movable dark reader, opaque over other apps and translucent over Near's live camera.
- Persistent recording choices and plainly labeled, independent script controls.
- Review of clean video and editable plain-text shares before local saving.
- Native density, font scaling, insets, and frame-timed movement.

This record describes the built app in `app/src/main`, checked against native emulator captures in `docs/images/verification` and `docs/images/verification/v1.1`. The original floating reader's finish disposition was **ship as a personal testing build**; its adjustment-spacing fix was resolved and recaptured in portrait and landscape at system font scale 1.3. The camera/share reviewer scored the navigation-contrast and documentation fixes resolved, returning **ship** for those two fixes. The physical Pixel 7 Pro was not connected; physical framing, microphone quality, Pixel Camera behavior, and a real-device recorded take remain unverified.

## Colors

The editor uses a restrained forest palette with warm neutral surfaces. The YAML frontmatter owns the exact values; the following names describe their application.

### Primary

| Role | Light token | Dark token | Application |
| --- | --- | --- | --- |
| Forest action | `light-primary` | `dark-primary` | Filled primary action, text actions, sliders, active switch, setting values. |
| Action foreground | `light-on-primary` | `dark-on-primary` | Text and symbols on the primary fill. |
| Green selection | `light-container` | `dark-container` | Selected width segment and native selection containers. |
| Selection foreground | `light-on-container` | `dark-on-container` | Text on selected width and related containers. |

The theme maps Android's secondary action and container roles to the same primary family. The resource named `near_secondary` is the muted text color, not a second brand accent.

### Neutral

| Role | Light token | Dark token | Application |
| --- | --- | --- | --- |
| Workbench surface | `light-surface` | `dark-surface` | Activity, persistent action area, window, and system-bar background. |
| Tonal surface | `light-surface-variant` | `dark-surface-variant` | Native surface-variant and surface-container roles. |
| Main ink | `light-on-surface` | `dark-on-surface` | Main editor and section text. |
| Muted ink | `light-secondary` | `dark-secondary` | Saved-local status, statistics, hints, and supporting text. |
| Boundary | `light-outline` | `dark-outline` | Outlined inputs and native outlined controls. |

The independent reader roles stay constant in both themes: `reader-surface` is the dark base; `reader-text` is the script; `reader-focus` is the first-line band; `reader-chrome` is status and control ink; `reader-outline` bounds the floating window; `reader-play` distinguishes the central playback surface; `reader-accent` supplies the countdown and ripple tint.

The camera and video review content use a consistently dark native theme, including when the editor is light. Their navigation edge independently follows the system day/night surface, using `light-surface` in day mode and `dark-surface` at night. Camera toolbar and control scrims use their named tokens over the live preview. `camera-surface-low` and `camera-surface-lowest` support native Material surface layering. The camera's primary Record fill uses `reader-accent`; active Stop recording uses the semantic `recording-stop` fill and white text. A textual recording state and elapsed timer accompany that color change.

**The Constant Reader Rule.** Keep the reader's dark color family and warm-white script in both editor themes. Preserve the floating mode's fixed first-line band; omit that band and bottom fade in the translucent camera reader.

## Typography

**System Font:** Android `sans` (native Roboto on the tested Android image), inherited through `Theme.Material3.DayNight.NoActionBar`.
**Reader Font:** Android `sans-serif-medium`, weight 500.

System type keeps the utility familiar and responds to Android font scaling. Material text appearances own the workbench hierarchy; the app does not impose a decorative display family or custom typeface.

| Role | Native source | Use |
| --- | --- | --- |
| Headline | Material 3 `HeadlineSmall` | “Your words, closer to the lens.” |
| Title | Material 3 `TitleMedium` | Script, preview, text size, pace, and width sections. |
| Body | Native outlined text field and Material body roles | Script title and ordinary UI copy. |
| Script editor | Explicit 17 sp with 4 dp extra line spacing | Multiline script input. |
| Label | Material 3 `LabelLarge` | Setting values. |
| Supporting copy | Material 3 `BodySmall` | Saved-local statement, word count, and hints. |
| Reader | Default 26 sp, adjustable 18–42 sp | Script preview, floating script text, and camera reader. |
| Reader chrome | Explicit 12 sp | Status, text-size label, and pace label. |
| Camera title and Record | Material 3 `TitleMedium` | Camera toolbar title and primary recording action. |
| Camera handle and status | Material 3 `LabelMedium` / `LabelLarge` | Drag instruction and recording feedback. |
| Reader sheet title | Material 3 `TitleLarge` | Camera reader-settings bottom sheet. |

The YAML omits numeric values for inherited Material appearances because the app does not override them. Reader text is measured with Android scaled density. Its layout uses 1.1 line-spacing multiplier plus 5 dp extra spacing, with font padding excluded. The countdown numeral is a functional canvas graphic sized at 54 dp rather than an sp text role.

**The Native Scale Rule.** Preserve system font scaling and wrapping; keep long settings and footer hints able to grow vertically.

## Layout

The editor workbench is one vertical phone layout: native toolbar, a weighted scroll area, and an action area outside that scroll area. Content uses 24 dp horizontal gutters. The main script editor is 240 dp tall and scrolls internally; the surrounding page scrolls to the preview and settings. The preview is 156 dp tall. Section openings use 28 dp top spacing, with 8 dp between headings and their following content. Supporting gaps use the recorded spacing scale.

The persistent footer has 24 dp horizontal insets, 8 dp top padding, and 12 dp bottom padding. The filled “Record in Near” action has a minimum height of 56 dp; the outlined “Open over camera” action beneath it has a minimum height of 48 dp and an 8 dp gap. A further outlined “Close reader” appears while the external reader is running. Hint copy wraps below the actions. Both recording choices stay available while the user edits or adjusts settings.

Controls retain native touch areas: sliders and width choices are at least 48 dp high, the countdown switch row is at least 56 dp high, and every floating icon control is 48 × 48 dp. Independent floating controls have 8 dp separation: either side of the flexible play control, between each adjustment minus/plus pair, and between adjustment rows. The joined Material width selector is one segmented group.

The floating window uses the safe screen width: narrow 62%, medium 80%, or wide 96%, clamped to at least 220 dp and at most screen width minus 8 dp. The editor preview expresses the same choices with text measures of 65%, 82%, and 100% of its available measure. The window opens centered near the top, 12 dp down by default, and its header drag position is saved. Dragging clamps it to the available screen.

The overlay stacks a 48 dp header, the reader viewport, a playback row, and an optional adjustment panel. Its viewport is normally 152 dp; available-height calculation reduces it as far as 84 dp on a short display. Rotation pauses reading, recenters the window near the top, and recomputes width and text layout. Portrait and landscape share the same controls; tablets have no separate composition.

The camera's first viewport contains a 56 dp toolbar with back, actual video quality, and camera-switch controls; the live preview; a near-top reader; and separate script and recording controls. CameraX preview uses `fitCenter`, preserving the framing with letterboxing when needed. The stage has 12 dp margins. Its reader is at most 220 dp tall, limited by available stage height, with a 48 dp drag handle. The controls have 12 dp padding, a status line, a 48 dp restart/play/settings row, and a 64 dp Record action beside a 48 dp last-take review button. Independent controls retain 8 dp gaps. Landscape places a 280 dp control column beside the weighted stage instead of below it. Rotation returns the reader to the top and recomputes its height.

Share text review uses the editor's toolbar and 24 dp content gutters, a scrolling form with a 320 dp multiline field, and persistent Save as new script / Cancel actions with 56 dp / 48 dp minimum heights. The native text counter and error can grow below the field. Video review uses a toolbar, weighted native video player, and a 56 dp Share video action with 16 dp outer margins.

Activities apply system-bar, display-cutout, and keyboard insets on API 30+, with system-window inset handling on older versions. The keyboard uses native resize behavior. The overlay measures system-bar and cutout-safe bounds on API 30+ and avoids display cutouts on API 28+. Status icon appearance comes from the activity's Material surface, so dark camera/review status bars request light icons. The navigation edge is separately painted from the system day/night `near_surface` resource and requests matching icons: a warm light edge in day mode, a dark edge at night. This protection covers bottom or side navigation insets behind the translucent system navigation area. The camera reader-settings bottom sheet applies the same protection to its own window. Camera content remains dark in both modes. Geometry remains in dp, type in sp, and text wraps under increased system font scale.

**The Clear Target Rule.** Preserve 48 dp floating targets and 8 dp gaps between independent controls when the window resizes or text scales.

## Elevation & Depth

The editor uses native Material control surfaces, outlines, and tonal selection rather than custom card shadows. Fields remain part of a continuous workbench. Menus, dialogs, and feedback retain their native Material treatment.

The floating reader is the structural lifted surface: 6 dp Android elevation with a 1 dp outline, clipped to its rounded boundary. The band is tonal emphasis inside that surface, not an elevated card. The lower 44 dp of script has a transparent-to-reader-surface gradient that signals continuation without moving the reading line.

The in-camera reader uses a translucent dark fill clipped to the same rounded silhouette. Panel alpha and script alpha are independent, so the user can trade contrast against visible framing. It has no first-line band or lower fade. Toolbar and control scrims keep native actions readable over changing footage; the reader-settings bottom sheet uses native Material depth and scrim behavior.

## Shapes

Material 3 supplies the workbench's outlined fields, pill-shaped buttons, joined width segments, sliders, switch, dialogs, and menus. Retain those library shapes instead of substituting custom web-style controls.

Custom reader geometry is explicit: the window has 18 dp corners, the central play surface 14 dp corners, transparent icon ripple surfaces 12 dp corners, and the first-line band 8 dp corners. These values belong to their specific components, not a universal card radius.

## Components

### Workbench and local script fields

The toolbar contains the Near title and an overflow menu for import, export, delete, and help. “Scripts” opens a native popup list with the current entry checked and a “New script” option. Scripts are library entries. The named, outlined title field precedes the generous outlined body field; word count and approximate duration sit below. The title is a single line capped at 80 characters; the body supports multiline text up to 100,000 characters. Local saving is debounced by 350 ms and flushed when leaving the activity.

Native Material inputs supply focused outlines, floating title labels, keyboard selection, and accessibility behavior. Empty-reader launch gives a snackbar and focuses the body. Delete requires a Material confirmation dialog. Import/export use Android document pickers and report failure through a snackbar.

### Reader preparation

Text-size and pace sliders show their current values beside the section title. Editor size changes are 1 sp steps; pace is 60–220 approximate words per minute in steps of 5. Default values are 26 sp, 120 wpm, medium width, and countdown enabled. The width selector requires exactly one selection. The countdown switch has a separate explanatory hint. Preview changes reset its offset to the beginning.

### Persistent recording choices

“Record in Near” is the filled primary action. It flushes the current script, rejects an empty script with native feedback, closes any external floating reader, and opens Near's camera. The camera activity owns camera and microphone capture in this mode.

“Open over camera” is the outlined secondary action and preserves the original external-camera workflow. It explains overlay access through a Material dialog when required, then uses Android permission settings. While the external reader runs, its label becomes “Update reader & open camera” and a separate outlined “Close reader” appears. Android 13+ notification permission is requested once; the foreground notification exposes play/pause and close controls. In this mode, the external camera app owns recording and Near's overlay service does not access camera or microphone.

### Camera reader and recording

The live camera is the stage, with the script drawn in a separate UI layer. The rounded reader opens near the top and its labeled handle moves it vertically within the stage. Tapping the script toggles reading; swiping seeks and pauses it. Restart rewinds and pauses only the script. Play script / Pause script remains a separate action from Record / Stop recording. Pausing, seeking, or reaching the end of the script does not stop video capture. The reader and camera controls are excluded from the encoded video.

The native reader-settings bottom sheet has text size (18–42 sp, steps of 1), reading pace (60–220 wpm, steps of 5), Background opacity, and Text opacity (each 0–100%, steps of 5). Defaults are 26 sp, 120 wpm, 55% panel opacity, and 100% text opacity. Reset appearance restores only the two opacity defaults. Opacity changes affect the camera reader; the external floating reader remains opaque. Size and pace are shared reading preferences.

The camera opens on the front lens when available. Camera switch and quality changes are unavailable during countdown, recording, or saving. The quality label shows the supported quality actually bound to the device, with fallback when the preferred 1080p or 720p option is unavailable. Last-take review is enabled only when a saved take exists and the camera is idle.

| Camera state | Visible behavior |
| --- | --- |
| Opening / permission needed | Record is unavailable until the camera is ready. Allow camera, system app settings, or Retry camera provides recovery. |
| Ready | “Ready for your take”; mint Record action. Microphone permission is requested on recording. If denied, a native dialog offers a silent take, settings, or cancel. |
| Countdown | Optional configured 3, 2, 1; the primary action becomes Cancel countdown. Script playback pauses during this countdown. |
| Recording | Red Stop recording action; status identifies sound / no sound and elapsed time. Script starts on the actual recording-start event and can then be paused or resumed independently. |
| Saving | “Saving your take…”; recording action and conflicting camera controls are disabled while capture finalizes and the saved file is checked. |
| Saved / interrupted / failed | Text distinguishes a saved take, a playable interrupted take, absent expected audio, or failure to save. Unplayable output is discarded; a validated playable take becomes the last-take review target. |

Stop recording pauses the script and finalizes video. Leaving an active take asks to Stop and leave or Cancel; navigation completes after saving. Backgrounding or rotating cancels countdown, pauses reading, and stops/finalizes any active take. Rotation reflows the stage and controls; it does not continue the same recording across the orientation change.

### Video review and incoming shared text

Review last take opens the latest validated video in Android's native player with playback controls and a Share video action. Playback starts when prepared, pauses off-screen, and restores its position on recreation. Missing or unplayable video gives native feedback and disables sharing. Share video opens Android's chooser with temporary read access to the video.

Share into Near accepts a plain-text Android share into an editable title/body review. The subject seeds a title capped at 80 characters. Save as new script creates a new local library entry and returns to the editor; Cancel exits the review. Neither action replaces an existing script. Save remains disabled for blank or over-limit text. Incoming text over 100,000 characters is explicitly rejected before it reaches the editor, with a field error instead of silently truncating the share. A URL-only share remains editable plain text with a link-specific hint; Near does not fetch the page.

### Floating reader

The header shows `state · pace wpm`, acts as the drag handle, and contains a separate close button. The script viewport accepts tap for play/pause and vertical swipes for manual text seeking. Swiping pauses playback; dragging the header moves the whole window. Screen-reader scroll actions seek by one line.

The playback row has restart, a flexible central play/pause control, and a show/hide settings control. Restart pauses and rewinds to the beginning. The expanded settings panel has text-size and pace rows with labeled values and separate minus/plus buttons. Text size changes by 2 sp and pauses while retaining proportional script progress; pace changes by 5 wpm. Controls have spoken descriptions and native ripple feedback. The custom script content is available as an accessibility description; header drag uses touch.

### Reader timing and states

Movement follows `Choreographer` frame callbacks using monotonic timestamps. Distance is elapsed time times pixels per second, with a 100 ms maximum frame step to prevent large skips after stalls. Speed derives from laid-out text height, word count, and selected approximate wpm; it is not speech tracking. The text scrolls upward beneath a stationary first-line focus band in the external reader and editor preview; the camera reader omits the band. There are no decorative entrance or page-transition animations.

| Visible status | Trigger and behavior |
| --- | --- |
| Ready | Reader opened or restarted at the beginning, paused. |
| Starting | Optional countdown shows 3, 2, 1 at one-second intervals. Pause cancels it. |
| Scrolling | Play advances text frame by frame; central icon becomes Pause. |
| Paused | Playback or a manual seek stops at a position after the beginning. Resume starts from that position, using the configured countdown. |
| End of script | Last line reaches the reading line and playback stops. Play restarts before running again. |

In external floating mode, screen off and rotation pause reading. Closing removes the overlay and foreground service; it does not control the external camera app's recording. Launch or permission failures return native feedback in the editor. The camera's recording countdown and lifecycle follow the separate camera states above; the reader-state table describes the existing floating mode.

Evidence for the original floating surfaces is `phone-light.png`, `phone-settings.png`, `phone-dark-large-text.png`, `phone-dark-settings-large-text.png`, `phone-overlay-large-text.png`, `phone-overlay-controls-large-text.png`, and `phone-landscape-overlay.png` in `docs/images/verification/`. Camera/share evidence in `docs/images/verification/v1.1/` includes `camera-phone.png`, `camera-settings-phone.png`, `camera-recording-phone.png`, `camera-landscape-large.png`, `review-phone.png`, `share-phone.png`, `share-dark-large-phone.png`, and `editor-phone.png`. These are emulator Android renders; green blocks in the camera captures are the Android synthetic camera input. They establish native layout and states, not physical-camera image quality. The finish reviewer measured the corrected navigation contrast at approximately 8.80:1 and scored that fix resolved. The sidecar's HTML/CSS is an illustrative translation for its design panel, not app source or browser verification.

## Do's and Don'ts

### Do:

- Do use native Material 3 controls and inherited text appearances for the workbench.
- Do keep Record in Near primary and Open over camera secondary, outside the scrolling preparation content.
- Do keep the floating reader dark with its first-line band stationary, and keep camera reader opacity independently adjustable.
- Do keep script playback controls separate from Record / Stop recording and omit the reader from encoded video.
- Do review incoming plain text before creating a new script; show explicit blank, oversize, and URL-only states.
- Do preserve 48 dp floating targets and 8 dp gaps between independent controls.
- Do respect native insets, reflow text, and pause before recomputing rotated reader geometry.
- Do use elapsed frame time for scrolling and keep countdown timing functional.

### Don't:

- Don't replace native controls with a web wrapper or an image composition.
- Don't introduce a separate decorative font, illustration, or card system into this workbench.
- Don't use editor theme changes to invert the floating reader.
- Don't make script play, pause, or restart stop camera capture; keep recording lifecycle explicit.
- Don't silently truncate shared text or fetch a shared URL as page content.
- Don't describe emulator screenshots as verification on the physical Pixel 7 Pro or Pixel Camera.
