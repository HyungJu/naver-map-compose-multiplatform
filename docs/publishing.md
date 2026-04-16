# Publishing

## Current Publishing Model

`naver-map-compose` now follows the same Maven Central pattern used by recent Kotlin Multiplatform library templates:

- publish with `com.vanniktech.maven.publish`
- enable Central publishing via `mavenCentralPublishing=true`
- use `./gradlew :naver-map-compose:publishToMavenCentral`
- let the plugin handle publication wiring, signing, sources/javadoc jars, and automatic release

The shared version source of truth is still `gradle.properties` -> `VERSION_NAME`.

## Required Sonatype Setup

Before the first upload, prepare these once:

1. Verify the `io.github.hyungju.navermap` namespace in the Sonatype Central Portal.
2. Generate a Central Portal publishing token.
3. Prepare a GPG key pair for signing and publish the public key.

Important: Maven Central publishing uses a Portal token, not your regular Sonatype login password.

## Preferred Local Credential Setup

The plugin reads publishing secrets from Gradle properties. The most reliable local setup is `~/.gradle/gradle.properties`:

```properties
mavenCentralUsername=...
mavenCentralPassword=...
signingInMemoryKey=-----BEGIN PGP PRIVATE KEY BLOCK-----
...
-----END PGP PRIVATE KEY BLOCK-----
signingInMemoryKeyPassword=...
```

`signingInMemoryKeyId` is optional.

If you prefer environment variables, use Gradle's `ORG_GRADLE_PROJECT_` convention:

```bash
export ORG_GRADLE_PROJECT_mavenCentralUsername=...
export ORG_GRADLE_PROJECT_mavenCentralPassword=...
export ORG_GRADLE_PROJECT_signingInMemoryKey="$(cat /path/to/private.asc)"
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword=...
```

If your CI stores the private key as base64, decode it before passing it to Gradle. The plugin expects the final value to be the full ASCII-armored private key block.

## Local Release Commands

### 1. Validate artifacts locally

```bash
./gradlew :naver-map-compose:publishToMavenLocal
```

### 2. Inspect the next release version automatically

```bash
git tag --sort=version:refname | tail -n 5
git log --oneline <last-tag>..HEAD
git diff --stat <last-tag>..HEAD
```

Codex can inspect those changes and recommend `major`, `minor`, or `patch`. In the app, invoke it with `/release`.

### 3. Cut a release from your machine

```bash
./scripts/cut-release.sh 0.1.0
```

This command:

- updates `VERSION_NAME` from snapshot to the release version
- creates a `Release x.y.z` commit
- creates and pushes `vx.y.z`
- creates or updates the GitHub Release with `gh`
- bumps `VERSION_NAME` to the next snapshot
- pushes `main`

If you want to control the next snapshot explicitly:

```bash
./scripts/cut-release.sh 0.2.0 0.3.0-SNAPSHOT
```

### 4. Publish a snapshot manually

```bash
./gradlew :naver-map-compose:publishToMavenCentral
```

If `VERSION_NAME` ends with `-SNAPSHOT`, the plugin uploads to the Central Portal snapshot repository.

### 5. Publish a final release manually

```bash
./gradlew :naver-map-compose:publishToMavenCentral -PVERSION_NAME=0.1.1
```

For a non-snapshot version, the plugin uploads the deployment, waits for Central validation, and automatically releases it.

## GitHub Actions Automation

`.github/workflows/publish-maven-central.yml` publishes on:

- tag push: `v*`
- manual dispatch with a version input

Recommended repository secrets:

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `MAVEN_CENTRAL_GPG_PRIVATE_KEY`
- `MAVEN_CENTRAL_GPG_PASSPHRASE`

The workflow maps those secrets to the Gradle properties that `com.vanniktech.maven.publish` expects, normalizes the private key if needed, resolves the release version from the tag, and runs:

```bash
./gradlew :naver-map-compose:publishToMavenCentral -PVERSION_NAME=<tag-version> --no-configuration-cache
```

## Release Checklist

1. Confirm `README.md` dependency examples still match the latest released artifact.
2. Run `./gradlew :naver-map-compose:publishToMavenLocal`.
3. Use `/release` so Codex inspects the recent changes and recommends the next release version.
4. Run `./scripts/cut-release.sh <release-version> [next-snapshot-version]`.
5. If you need to bypass the helper, use the manual Gradle commands above.
