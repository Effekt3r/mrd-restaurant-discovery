# Security

This repository is a take-home assessment, not a production service. It's public, so it's run with the same care as one.

## Reporting a vulnerability

Use GitHub's private vulnerability reporting: **Security → Report a vulnerability**. Please don't open a public issue.

## How the repository is protected

| Control | What it does |
|---|---|
| Rulesets on `master` and `dev` | Changes arrive only through pull requests. Force pushes and branch deletion are blocked, and review threads must be resolved before merging. Pull requests need no approval, even for commits from email addresses not linked to an account, because a solo maintainer can't approve their own pull request. |
| Ruleset on `v*` tags | Release tags can't be moved or deleted. |
| Required checks | **Build and test** and **Secret scan** must pass before anything merges into `dev` or `master`. |
| Secret scanning with push protection | GitHub rejects pushes that contain known secret formats. Whoever is pushing can still bypass a block by giving a reason, but the CI secret scan runs regardless. |
| Local secret scan | A gitleaks pre-commit hook in `.githooks/` stops secrets before they're committed. It's opt-in (`git config core.hooksPath .githooks`). It skips with a warning when gitleaks isn't installed, so CI is the hard gate. |
| CI secret scan | gitleaks scans the full history on every pull request into `dev` or `master` and on every push to them. The binary is checksum-verified. Workflows from outside contributors wait for approval before they run. |
| Dependabot | **Alerts:** vulnerability alerts cover Gradle dependencies, fed by a dependency-submission workflow on `master`. **Security fixes:** for direct dependencies declared in the version catalog, Dependabot opens fix pull requests against `master`, which are merged back into `dev` like hotfixes; transitive dependencies get an alert only. **Version updates:** weekly, against `dev`. Libraries are proposed after 30 days, and GitHub Actions after 7, because actions pinned to a commit SHA never get alerts. **When it started:** Dependabot reads its configuration from `master`, so it started with the first release to `master`, on 2026-11-19. |
| GitHub Actions | The workflow token is read-only by default, and workflows can't approve pull requests. Two jobs can write: dependency submission's submit job, which runs no build code, and the release job, which runs only for `v*` tags and needs `contents: write` to create the GitHub Release. Only GitHub-owned actions and `gradle/actions` may run, each pinned to a commit SHA. Gradle caching uses the open-source provider. |
| Gradle wrapper | The wrapper verifies the distribution's SHA-256, and CI validates the wrapper JAR. |
| Signing material | Keystores, signing properties, `.env` files and local Claude Code state are never committed (`.gitignore`). Release signing keys come from Gradle properties or environment variables. In CI they're secrets on the `release` environment, which only `v*` tags can deploy from, and the release job fails unless the APK is signed with the upload key's certificate. |

These settings are applied from code by [`tools/github/apply-repo-settings.sh`](tools/github/apply-repo-settings.sh) and [`.github/rulesets/`](.github/rulesets/).

## Open Dependabot alerts

Assessed on 2026-11-19. Dependabot reports 53 open alerts against `master`'s dependency graph: 3 critical, 22 high, 26 medium and 2 low. Each one is a transitive dependency of the build's own tooling, and none ships in the APK: `:app`'s release runtime classpath contains none of these packages.

| Package | Alerts | Comes in through | Why the risk is low here |
|---|---|---|---|
| Netty | 41 | AGP's Unified Test Platform, which runs instrumented tests and controls the emulator | It only runs for instrumented tests on a device or emulator, and CI runs none. |
| Bouncy Castle | 6 | AGP's SDK tools and Android Lint | Build time only. `dev`'s AGP 9.5.1 brings Bouncy Castle 1.80.2, which fixes three of the six. |
| Guava | 1 | AGP, Android Lint and Hilt's annotation processor | The flaw is in Java deserialization of some Guava collections, and these tools don't deserialize untrusted data. |
| Apache Commons Lang, Apache HttpClient | 2 | AGP's SDK tools | Build time only. |
| jose4j | 1 | bundletool, inside AGP | bundletool builds app bundles, and this project builds APKs. |
| JDOM | 1 | Jetifier, inside AGP | Jetifier is off: `android.enableJetifier` isn't set. |
| Kotlin Gradle plugin | 1 | Gradle's `kotlin-dsl`, which compiles `build-logic` with the Kotlin embedded in Gradle: 2.4.0 in Gradle 9.7.1 on `master`, 2.4.10 in Gradle 9.8.2 on `dev` | The flaw is unsafe deserialization of Kotlin's build-cache metadata, so it needs a tampered cache. Only pushes to `dev` and `master` write CI's Gradle cache; pull requests and the release job only read it. The project's own Kotlin plugin, 2.4.21, has the fix. The alert closes when a Gradle release embeds Kotlin 2.4.20 or later. |

**What a compromised build tool could reach.** Every job's token is read-only except two. Dependency submission's submit job runs no build code. The release job does: it builds with the signing secrets and holds `contents: write` to create the GitHub Release. It runs only for `v*` tags, which can't be moved or deleted, and only in the `release` environment, which only `v*` tags can deploy from. It reads CI's Gradle cache but never writes it, and it refuses to publish an APK whose signing certificate isn't the upload key's.

**How a team would handle alerts like these:**
1. **Triage every week, with an owner:** the platform squad that owns the build.
2. **Ask first whether it ships.** A vulnerable package in the APK's runtime classpath gets fixed within days. For a direct dependency, Dependabot opens a security-fix pull request against `master`; for a transitive one, a Gradle dependency constraint raises the version. Then release.
3. **In the build, ask what the tool reads.** If nothing untrusted reaches the vulnerable code, take the fix with the next AGP, Gradle or plugin update that carries it, which Dependabot proposes. Forcing a newer transitive version under AGP risks a build it was never tested with.
4. **Dismiss with a reason.** An alert assessed as unreachable is dismissed in GitHub with its reason and the date, so a new alert stands out instead of hiding in a standing count. These aren't dismissed yet; this section is the assessment.
5. **Re-check when the toolchain moves.** Every AGP or Gradle update changes the graph. Releasing `dev`'s AGP 9.5.1 and Gradle 9.8.2 to `master` will close some of these alerts and could open others.

## App security

- HTTPS only: the network security configuration refuses cleartext traffic.
- Release builds are R8-minified and not debuggable.
- The app holds no secrets, analytics or personal data. Favourites stay on the device, and backup and device transfer are off.
