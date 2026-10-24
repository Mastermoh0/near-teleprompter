# Teleprompter

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

Delegated by the user: a native Android application, using Java 17 and Android platform views. No network-dependent runtime or web wrapper.

## Users

Muhammad, recording talking-head videos on a Google Pixel 7 Pro. Reading from a laptop beside the phone currently makes his gaze visibly drift away from the lens.

## Product Purpose

Read a prepared script near the front camera while recording a video in Near or in an existing camera app, with natural-looking eye contact.

## Operating Context

The user first requested a floating teleprompter over an existing camera app, then approved a built-in camera and Share text into Near. Both recording choices remain available. Scripts and settings work offline. In the built-in mode, Near owns camera and microphone capture; in floating mode, the external camera app controls recording.

## Capabilities and Constraints

- Enter and save scripts locally.
- Adjustable text size and manual scrolling speed.
- Move a compact reader near the camera lens.
- Start, pause, resume, and restart script scrolling while using the camera.
- Offline English voice-follow in the built-in camera, with fixed pace and manual seeking available for recovery.
- Build target: Pixel 7 Pro, with support for Android 8.0 and later.
- Built-in recording with a translucent reader, separate script playback, and review or sharing of the saved video.
- Incoming shared plain text is reviewed and saved as a new script without replacing existing work.
- No account, custom server, or analytics. Camera and microphone access is requested for built-in recording and voice-follow.
- Optional update alerts and manual checks use public GitHub metadata over HTTPS. Core reading, recording, and recognition remain offline; scripts, speech, and videos are not uploaded by the checker.

## Product Principles

- Keep the reader close to the lens and the camera controls reachable.
- Make recording preparation brief and predictable.
- Keep scripts private and available offline.
- Keep fixed pace and manual seeking dependable when speech tracking is uncertain.
