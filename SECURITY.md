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
| Dependabot | **Alerts:** vulnerability alerts cover Gradle dependencies, fed by a dependency-submission workflow on `master`. **Security fixes:** Dependabot opens fix pull requests against `master`, which are merged back into `dev` like hotfixes. **Version updates:** weekly, against `dev`. Libraries are proposed after 30 days, and GitHub Actions after 7, because actions pinned to a commit SHA never get alerts. **When it starts:** Dependabot reads its configuration from `master`, so none of this runs until the first release. |
| GitHub Actions | The workflow token is read-only by default, and workflows can't approve pull requests. Only GitHub-owned actions and `gradle/actions` may run, each pinned to a commit SHA. Gradle caching uses the open-source provider. |
| Gradle wrapper | The wrapper verifies the distribution's SHA-256, and CI validates the wrapper JAR. |
| Signing material | Keystores, signing properties, `.env` files and local Claude Code state are never committed (`.gitignore`). Release signing keys come from Gradle properties or environment variables. |

These settings are applied from code by [`tools/github/apply-repo-settings.sh`](tools/github/apply-repo-settings.sh) and [`.github/rulesets/`](.github/rulesets/).

## App security

- HTTPS only: the network security configuration refuses cleartext traffic.
- Release builds are R8-minified and not debuggable.
- The app holds no secrets, analytics or personal data. Favourites stay on the device, and backup and device transfer are off.
