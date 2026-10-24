# Third-party notices

Near's original application code and tutorial are MIT licensed. Bundled dependencies and assets retain their own licenses.

## Offline speech

- **Vosk Android 0.3.75**, Alpha Cephei contributors: [source](https://github.com/alphacep/vosk-api), Apache License 2.0.
- **vosk-model-small-en-us-0.15**, Alpha Cephei: [official model and license listing](https://alphacephei.com/vosk/models.html), Apache License 2.0. Build archive SHA-256: `30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498`. The original model README is retained in the bundled assets.
- **JNA 5.18.1**, Java Native Access contributors: [source and licensing](https://github.com/java-native-access/jna), available under Apache License 2.0 or LGPL 2.1; this distribution uses the Apache License 2.0 option.
- **Speech test fixture**, `app/src/androidTest/assets/vosk-test.wav`: copied unchanged from [Vosk's example test.wav](https://github.com/alphacep/vosk-api/blob/master/python/example/test.wav), under the Vosk repository's Apache License 2.0. It is test audio, not a recording supplied by a Near user, and is packaged only in the instrumentation test APK.

The complete [Apache License 2.0 text](app/src/main/assets/licenses/Vosk-Apache-2.0.txt) is included in the source and APK assets. Recognition is performed locally; these projects do not receive Near users' scripts or speech.

AndroidX, CameraX, AppCompat, and Material Components use their upstream licenses. The Gradle/Maven dependencies declare their licenses and upstream notices; no affiliation with those projects is implied.
