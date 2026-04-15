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

### 2. Inspect the next release version automatically

```bash
git tag --sort=version:refname | tail -n 5
git log --oneline <last-tag>..HEAD
git diff --stat <last-tag>..HEAD
```

Codex can inspect those changes and recommend `major`, `minor`, or `patch`. The version choice now lives in the skill and conversation, not in the script.

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

The command requires:

- a clean worktree
- running on `main`
- a configured git remote
- `gh auth status` to succeed

### 4. Publish a snapshot manually

```bash
./gradlew :naver-map-compose:publishToMavenCentral
```

This uses `VERSION_NAME` from `gradle.properties`. If it ends with `-SNAPSHOT`, the library is uploaded to the Sonatype snapshot repository.

### 5. Publish a final release manually

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

1. Confirm `README.md` dependency examples still match the latest released artifact.
2. Run `./gradlew :naver-map-compose:publishToMavenLocal`.
3. Let Codex inspect the recent changes and recommend the next release version.
4. Run `./scripts/cut-release.sh <release-version> [next-snapshot-version]`.
5. If you need to bypass the helper, use the manual Gradle commands above.
