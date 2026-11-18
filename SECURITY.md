# Security

This repository is a take-home assessment, not a production service. It's public, so it's run with the same care as one.

## Reporting a vulnerability

Use GitHub's private vulnerability reporting: **Security → Report a vulnerability**. Please don't open a public issue.

## How the repository is protected

| Control | What it does |
|---|---|
| Rulesets on `master` and `dev` | Changes arrive only through pull requests. Force pushes and branch deletion are blocked, and review threads must be resolved before merging. |
| Ruleset on `v*` tags | Release tags can't be moved or deleted. |
| Required checks | **Build and test** and **Secret scan** must pass before anything merges into `dev` or `master`. |
| Secret scanning with push protection | Pushes that contain known secret formats are rejected. |
| Local secret scan | A gitleaks pre-commit hook in `.githooks/` stops secrets before they're committed (enable it with `git config core.hooksPath .githooks`). |
| CI secret scan | gitleaks scans the full history on every pull request and every push to `dev` and `master`. The binary is checksum-verified. |
| Dependabot | Vulnerability alerts and automatic security-fix pull requests. Version updates wait 30 days after a release. |
| GitHub Actions | The workflow token is read-only by default, and workflows can't approve pull requests. Workflows from outside contributors wait for approval. Only GitHub-owned actions and `gradle/actions` may run, each pinned to a commit SHA. |
| Gradle wrapper | The wrapper verifies the distribution's SHA-256, and CI validates the wrapper JAR. |
| Signing material | Keystores, signing properties, `.env` files and local Claude Code state are never committed (`.gitignore`). Release signing keys come from the environment. |

These settings are applied from code by [`tools/github/apply-repo-settings.sh`](tools/github/apply-repo-settings.sh) and [`.github/rulesets/`](.github/rulesets/).

## App security

- HTTPS only: the network security configuration refuses cleartext traffic.
- Release builds are R8-minified and not debuggable.
- The app holds no secrets, analytics or personal data. Favourites stay on the device, and backup is off.
