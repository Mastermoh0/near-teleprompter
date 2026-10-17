# Teleprompter

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

Delegated by the user: a native Android application, using Java 17 and Android platform views. No network-dependent runtime or web wrapper.

## Users

Muhammad, recording talking-head videos on a Google Pixel 7 Pro. Reading from a laptop beside the phone currently makes his gaze visibly drift away from the lens.

## Product Purpose

Read a prepared script near the front camera while recording video in the existing Pixel Camera application, with natural-looking eye contact.

## Operating Context

The user explicitly confirmed a floating teleprompter over the existing camera app. Scripts and settings must work offline. The camera app controls recording and owns the camera and microphone.

## Capabilities and Constraints

- Enter and save scripts locally.
- Adjustable text size and manual scrolling speed.
- Move a compact reader near the camera lens.
- Start, pause, resume, and restart script scrolling while using the camera.
- Voice-follow scrolling is deferred; it is optional in the user's brief.
- Build target: Pixel 7 Pro, with support for Android 8.0 and later.
- No account, server, analytics, microphone, camera, or internet permission in this application.

## Product Principles

- Keep the reader close to the lens and the camera controls reachable.
- Make recording preparation brief and predictable.
- Keep scripts private and available offline.
- Prefer reliable manual control over premature speech recognition.
