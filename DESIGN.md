---
name: Near · Teleprompter
description: A native Android script workbench and floating reader close to the lens.
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
---

# Design System: Near · Teleprompter

## Overview

**Creative North Star: "Native Android workbench"**

Near is a quiet, practical place to prepare words and read them close to the lens. Warm surfaces, forest green actions, system typography, and familiar Material 3 controls give editing and setup a clear hierarchy. The script is the main content; the interface stays compact and direct.

The signature is a dark floating reader with large warm-white text, a steady first-line focus band, and a bottom fade. Its header is a handle, its central control starts or pauses reading, and its small adjustment panel can be tucked away. This is a code-led native interface built with Java 17, Android views, and Material Components 1.13.0. There is no image composition or generated artwork behind the UI.

**Key Characteristics:**

- Native Material 3 editing and settings.
- Warm light and forest dark editor themes.
- An always-dark, movable reader with a stable reading line.
- Persistent preparation action and plainly labeled reading controls.
- Native density, font scaling, insets, and frame-timed movement.

This record describes the built app in `app/src/main`, checked against the native captures in `docs/images/verification`. The finish disposition is **ship as a personal testing build**. The adjustment-spacing fix was resolved and recaptured in portrait and landscape at system font scale 1.3. The physical Pixel 7 Pro was not connected; Pixel Camera behavior and a real recorded take remain unverified.

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

**The Constant Reader Rule.** Keep the reading surface dark in both editor themes, with warm-white script text and a fixed first-line band.

## Typography

**System Font:** Android `sans`, inherited through `Theme.Material3.DayNight.NoActionBar`.
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
| Reader | Default 26 sp, adjustable 18–42 sp | Script preview and floating script text. |
| Reader chrome | Explicit 12 sp | Status, text-size label, and pace label. |

The YAML omits numeric values for inherited Material appearances because the app does not override them. Reader text is measured with Android scaled density. Its layout uses 1.1 line-spacing multiplier plus 5 dp extra spacing, with font padding excluded. The countdown numeral is a functional canvas graphic sized at 54 dp rather than an sp text role.

**The Native Scale Rule.** Preserve system font scaling and wrapping; keep long settings and footer hints able to grow vertically.

## Layout

The workbench is one vertical phone layout: native toolbar, a weighted scroll area, and an action area outside that scroll area. Content uses 24 dp horizontal gutters. The main script editor is 240 dp tall and scrolls internally; the surrounding page scrolls to the preview and settings. The preview is 156 dp tall. Section openings use 28 dp top spacing, with 8 dp between headings and their following content. Supporting gaps use the recorded spacing scale.

The persistent footer has 24 dp horizontal insets, 8 dp top padding, and 12 dp bottom padding. Its filled action has a minimum height of 56 dp. The secondary “Close reader” action appears while the reader is running, with a minimum height of 48 dp and an 8 dp gap. Hint copy wraps below the actions. This keeps launching available while the user edits or adjusts settings.

Controls retain native touch areas: sliders and width choices are at least 48 dp high, the countdown switch row is at least 56 dp high, and every floating icon control is 48 × 48 dp. Independent floating controls have 8 dp separation: either side of the flexible play control, between each adjustment minus/plus pair, and between adjustment rows. The joined Material width selector is one segmented group.

The floating window uses the safe screen width: narrow 62%, medium 80%, or wide 96%, clamped to at least 220 dp and at most screen width minus 8 dp. The editor preview expresses the same choices with text measures of 65%, 82%, and 100% of its available measure. The window opens centered near the top, 12 dp down by default, and its header drag position is saved. Dragging clamps it to the available screen.

The overlay stacks a 48 dp header, the reader viewport, a playback row, and an optional adjustment panel. Its viewport is normally 152 dp; available-height calculation reduces it as far as 84 dp on a short display. Rotation pauses reading, recenters the window near the top, and recomputes width and text layout. Portrait and landscape share the same controls; tablets have no separate composition.

The activity applies system-bar, display-cutout, and keyboard insets on API 30+, with system-window inset handling on older versions. The keyboard uses native resize behavior. The overlay measures system-bar and cutout-safe bounds on API 30+ and avoids display cutouts on API 28+. System status/navigation icon appearance follows the activity's light or dark configuration. Geometry remains in dp, type in sp, and text wraps under increased system font scale.

**The Clear Target Rule.** Preserve 48 dp floating targets and 8 dp gaps between independent controls when the window resizes or text scales.

## Elevation & Depth

The editor uses native Material control surfaces, outlines, and tonal selection rather than custom card shadows. Fields remain part of a continuous workbench. Menus, dialogs, and feedback retain their native Material treatment.

The floating reader is the structural lifted surface: 6 dp Android elevation with a 1 dp outline, clipped to its rounded boundary. The band is tonal emphasis inside that surface, not an elevated card. The lower 44 dp of script has a transparent-to-reader-surface gradient that signals continuation without moving the reading line.

## Shapes

Material 3 supplies the workbench's outlined fields, pill-shaped buttons, joined width segments, sliders, switch, dialogs, and menus. Retain those library shapes instead of substituting custom web-style controls.

Custom reader geometry is explicit: the window has 18 dp corners, the central play surface 14 dp corners, transparent icon ripple surfaces 12 dp corners, and the first-line band 8 dp corners. These values belong to their specific components, not a universal card radius.

## Components

### Workbench and local script fields

The toolbar contains the Near title and an overflow menu for import, export, delete, and help. “Scripts” opens a native popup list with the current entry checked and a “New script” option. Scripts are library entries. The named, outlined title field precedes the generous outlined body field; word count and approximate duration sit below. The title is a single line capped at 80 characters; the body supports multiline text up to 100,000 characters. Local saving is debounced by 350 ms and flushed when leaving the activity.

Native Material inputs supply focused outlines, floating title labels, keyboard selection, and accessibility behavior. Empty-reader launch gives a snackbar and focuses the body. Delete requires a Material confirmation dialog. Import/export use Android document pickers and report failure through a snackbar.

### Reader preparation

Text-size and pace sliders show their current values beside the section title. Editor size changes are 1 sp steps; pace is 60–220 approximate words per minute in steps of 5. Default values are 26 sp, 120 wpm, medium width, and countdown enabled. The width selector requires exactly one selection. The countdown switch has a separate explanatory hint. Preview changes reset its offset to the beginning.

### Persistent action

“Open over camera” is the filled primary action. It explains overlay access through a Material dialog when required, then uses Android permission settings. While the reader runs, the label becomes “Update reader & open camera” and a separate outlined “Close reader” appears. Android 13+ notification permission is requested once; the foreground notification exposes play/pause and close controls. Camera recording remains owned by the camera app.

### Floating reader

The header shows `state · pace wpm`, acts as the drag handle, and contains a separate close button. The script viewport accepts tap for play/pause and vertical swipes for manual text seeking. Swiping pauses playback; dragging the header moves the whole window. Screen-reader scroll actions seek by one line.

The playback row has restart, a flexible central play/pause control, and a show/hide settings control. Restart pauses and rewinds to the beginning. The expanded settings panel has text-size and pace rows with labeled values and separate minus/plus buttons. Text size changes by 2 sp and pauses while retaining proportional script progress; pace changes by 5 wpm. Controls have spoken descriptions and native ripple feedback. The custom script content is available as an accessibility description; header drag uses touch.

### Reader timing and states

Movement follows `Choreographer` frame callbacks using monotonic timestamps. Distance is elapsed time times pixels per second, with a 100 ms maximum frame step to prevent large skips after stalls. Speed derives from laid-out text height, word count, and selected approximate wpm; it is not speech tracking. The text scrolls upward beneath a stationary first-line focus band. There are no decorative entrance or page-transition animations.

| Visible status | Trigger and behavior |
| --- | --- |
| Ready | Reader opened or restarted at the beginning, paused. |
| Starting | Optional countdown shows 3, 2, 1 at one-second intervals. Pause cancels it. |
| Scrolling | Play advances text frame by frame; central icon becomes Pause. |
| Paused | Playback or a manual seek stops at a position after the beginning. Resume starts from that position, using the configured countdown. |
| End of script | Last line reaches the reading line and playback stops. Play restarts before running again. |

Screen off and rotation pause reading. Closing removes the overlay and foreground service; it does not control camera recording. Launch or permission failures return native feedback in the editor.

Evidence for the implemented surfaces is `phone-light.png`, `phone-settings.png`, `phone-dark-large-text.png`, `phone-dark-settings-large-text.png`, `phone-overlay-large-text.png`, `phone-overlay-controls-large-text.png`, and `phone-landscape-overlay.png` in `docs/images/verification/`. These are emulator Android renders; the camera scene is AOSP Camera's synthetic scene. The sidecar's HTML/CSS is an illustrative translation for its design panel, not app source or browser verification.

## Do's and Don'ts

### Do:

- Do use native Material 3 controls and inherited text appearances for the workbench.
- Do keep the filled launch action available outside the scrolling preparation content.
- Do keep the reader dark, its first-line band stationary, and its large text responsive to system font scale.
- Do preserve 48 dp floating targets and 8 dp gaps between independent controls.
- Do respect native insets, reflow text, and pause before recomputing rotated reader geometry.
- Do use elapsed frame time for scrolling and keep countdown timing functional.

### Don't:

- Don't replace native controls with a web wrapper or an image composition.
- Don't introduce a separate decorative font, illustration, or card system into this workbench.
- Don't use editor theme changes to invert the floating reader.
- Don't turn reader play, pause, or close into camera recording controls.
- Don't describe emulator screenshots as verification on the physical Pixel 7 Pro or Pixel Camera.
