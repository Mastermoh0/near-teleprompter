# Building and publishing an installable release

Public downloads live on [GitHub Releases](https://github.com/Mastermoh0/near-teleprompter/releases). The repository contains the source and build instructions; APK binaries and checksums are release assets.

## Signing identity

Android requires APKs to be signed. Updates to the same application normally need the same signing identity. Near's public APK uses a dedicated release key, not the Android debug key. See [Android's app-signing documentation](https://developer.android.com/studio/publish/app-signing).

The original signing key and passwords are private. They are not in Git, the tutorial, screenshots, or release downloads. A fork should generate its own key and change `applicationId` to a package name it controls.

**If you installed the earlier personal debug build:** export your scripts, uninstall that build, and install the public release. The two signing identities cannot update over one another. Uninstalling removes local app data. Later upstream releases will use the same release key.

## First local release on Windows

```powershell
.\scripts\setup-tools.ps1
.\scripts\release.ps1 -CreateKey
```

The first command installs project-local build tools. The second creates `.signing/near-release.p12` and `.signing/release.properties`, then builds, tests, lints, verifies the APK signature, and writes the versioned APK plus `SHA256SUMS.txt` into `releases/`.

`-CreateKey` does not replace an existing configured key. Back up **both** files in `.signing/` privately before publishing updates. The properties file contains the passwords. Do not put that directory in a public archive. For subsequent builds:

```powershell
.\scripts\release.ps1
```

## Signing outside the local Windows helper

`app/build.gradle` accepts either the ignored `.signing/release.properties` or these four environment variables:

| Variable | Value |
| --- | --- |
| `NEAR_SIGNING_STORE_FILE` | Absolute or project-relative path to the PKCS12 keystore |
| `NEAR_SIGNING_STORE_PASSWORD` | Keystore password |
| `NEAR_SIGNING_KEY_ALIAS` | Key alias, `near-release` in the helper |
| `NEAR_SIGNING_KEY_PASSWORD` | Key password |

Then run:

```sh
bash gradlew assembleRelease testReleaseUnitTest lintRelease
```

Release assembly fails with a clear message if the signing configuration is missing. Debug builds work without release credentials. Nothing in the installed app needs any of these values.

## GitHub Actions releases

The normal check workflow is enabled. Upstream's initial public APK is signed and published locally; its private key and passwords have not been uploaded to GitHub. An optional tag-release workflow is included but its signing job stays disabled unless the repository variable `RELEASE_SIGNING_ENABLED` is set to `true`.

To enable that optional workflow for a repository you maintain, deliberately configure your own signing credentials as repository Actions secrets in **Settings → Secrets and variables → Actions**:

| Secret | Content |
| --- | --- |
| `NEAR_SIGNING_KEYSTORE` | Base64 encoding of your PKCS12 keystore file |
| `NEAR_SIGNING_STORE_PASSWORD` | Keystore password |
| `NEAR_SIGNING_KEY_ALIAS` | Key alias |
| `NEAR_SIGNING_KEY_PASSWORD` | Key password |

Do not paste passwords into an issue, commit, workflow YAML, or a terminal command that records them in shell history. GitHub CLI accepts secret values through standard input, as described in [its secret-set manual](https://cli.github.com/manual/gh_secret_set).

After configuring those secrets, set the repository Actions variable `RELEASE_SIGNING_ENABLED` to `true`. Only then will tag pushes run the signing job. A public repository does not mean its signing credentials should be public or transferred automatically.

To release:

1. Update `versionCode` and `versionName` in `app/build.gradle`. Increment `versionCode` for each published update.
2. Rewrite `docs/RELEASE_NOTES.md` for the actual version and validation.
3. Push the code and wait for the Android checks.
4. Tag the exact checked commit with `v` followed by `versionName`, then push that tag.

For version `1.0.0`, the commands are:

```sh
git tag v1.0.0
git push origin v1.0.0
```

When explicitly enabled, the tag workflow checks that the tag matches the version, restores the key into the temporary runner directory, builds and checks a signed release, verifies its signature, generates a checksum, and attaches the APK, tutorial Markdown, and narration text to a GitHub Release. The temporary keystore is removed afterward. It never uses signing secrets for pull-request builds.

## Publish locally without transferring signing credentials

This is the upstream 1.0.0 publication path. Build with the local helper, push the checked source and version tag, then upload only the signed public artifacts:

```powershell
.\scripts\release.ps1
git tag v1.0.0
git push origin v1.0.0
gh release create v1.0.0 releases/near-teleprompter-1.0.0.apk releases/SHA256SUMS.txt TUTORIAL.md docs/tutorial-voiceover.txt --verify-tag --title 'Near 1.0.0' --notes-file docs/RELEASE_NOTES.md
```

Use a new version/tag for later releases. These commands assume GitHub CLI is authenticated through its normal sign-in. They do not upload the private `.signing/` directory.

This publishes a downloadable Android APK. It does not submit the app to Google Play or complete any Google Play or developer-verification requirements. Keep platform-specific installation and hardware-test limits in the release notes.
