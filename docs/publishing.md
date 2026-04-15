# Publishing

## Version Source Of Truth

- Shared project version: `gradle.properties` -> `VERSION_NAME`
- Shared Maven coordinates: `gradle.properties` -> `GROUP`
- Release override for CI or one-off publish: `-PVERSION_NAME=0.1.1`

`VERSION_NAME` ends with `-SNAPSHOT` while you are preparing a release. Maven Central final releases must use a non-snapshot semantic version such as `0.1.1`.

## Required Sonatype Setup

Before the first upload, prepare these once:

1. Verify the `io.github.jude.navermap` namespace in the Sonatype Central Portal.
2. Generate a Portal token in Sonatype Central.
3. Prepare a GPG key pair for signing.

## Required Environment Variables

Gradle reads release secrets from Gradle properties or these environment variables:

```bash
export MAVEN_CENTRAL_USERNAME=...
export MAVEN_CENTRAL_PASSWORD=...
export MAVEN_CENTRAL_GPG_PRIVATE_KEY="$(cat /path/to/private.asc)"
export MAVEN_CENTRAL_GPG_PASSPHRASE=...
# optional
export MAVEN_CENTRAL_GPG_KEY_ID=...
```

## Local Release Commands

### 1. Validate artifacts locally

```bash
./gradlew :naver-map-compose:publishToMavenLocal
```

### 2. Publish a snapshot

```bash
./gradlew :naver-map-compose:publishToMavenCentral
```

This uses `VERSION_NAME` from `gradle.properties`. If it ends with `-SNAPSHOT`, the library is uploaded to the Sonatype snapshot repository.

### 3. Publish a final release

```bash
./gradlew :naver-map-compose:publishToMavenCentral -PVERSION_NAME=0.1.1
```

This uploads every `naver-map-compose` publication, signs them, and then calls the Sonatype Central staging API to finalize the deployment automatically.

## GitHub Actions Automation

`.github/workflows/publish-maven-central.yml` publishes on:

- tag push: `v*`
- manual dispatch with a version input

Recommended repository secrets:

- `MAVEN_CENTRAL_USERNAME`
- `MAVEN_CENTRAL_PASSWORD`
- `MAVEN_CENTRAL_GPG_PRIVATE_KEY`
- `MAVEN_CENTRAL_GPG_PASSPHRASE`
- `MAVEN_CENTRAL_GPG_KEY_ID` (optional)

The workflow extracts the release version from the tag, for example `v0.1.1` -> `0.1.1`, then runs:

```bash
./gradlew :naver-map-compose:publishToMavenCentral -PVERSION_NAME=<tag-version>
```

## Release Checklist

1. Update `VERSION_NAME` to the next planned version if you want the repository default to change.
2. Confirm `README.md` dependency examples still match the latest released artifact.
3. Run `./gradlew :naver-map-compose:publishToMavenLocal`.
4. Publish with `./gradlew :naver-map-compose:publishToMavenCentral -PVERSION_NAME=<release>`.
5. Tag the release or trigger the GitHub Actions workflow.
