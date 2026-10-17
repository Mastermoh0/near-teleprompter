# Contributing

Near is a native Java Android app. Small, focused changes are welcome under the MIT license.

## Start locally

Use Android Studio with Java 17 and SDK 36, or follow the Windows toolchain steps in [README.md](README.md). The Gradle wrapper pins Gradle 8.13. Run:

```sh
bash gradlew assembleDebug testDebugUnitTest lintDebug
```

On Windows use `gradlew.bat`, or `scripts/build.ps1 -Verify` with the project-local tools.

## Before opening a pull request

- Explain the actual problem, the resulting behavior, and how you checked it.
- Run the JVM tests and Android Lint. Add tests when behavior changes justify them.
- For UI changes, check a phone in light/dark mode, portrait/landscape, and larger system text.
- For overlay changes, test with a camera app, keep its recording controls accessible, and check the close button and notification actions.
- Keep scripts local. Adding network, camera, microphone, storage, or accessibility-service permissions changes the product's privacy model and needs a clear design discussion.
- Do not include signing keys, passwords, SDKs, Gradle caches, personal scripts, or machine-specific paths in a commit.

The regular CI workflow builds the debug app and runs JVM tests and Lint. It does not prove that Pixel Camera works on real hardware. Instrumentation tests are in `app/src/androidTest`; use an emulator or dedicated testing phone.

To publish your own fork, change `applicationId` and create your own signing key. This lets your app coexist with upstream Near. See [the release guide](docs/RELEASING.md). Local signing needs no GitHub signing secrets. The optional automated signing workflow stays disabled until explicitly configured; it never uses signing secrets for pull-request checks.
