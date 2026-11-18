# Security

This repository is a take-home assessment, not a production service. It is public, so it is run with the same care as one.

## Reporting a vulnerability

Use GitHub's private vulnerability reporting: **Security → Report a vulnerability**. Please don't open a public issue.

## How the repository is protected

| Control | What it does |
|---|---|
| Rulesets on `master` and `dev` | Changes arrive only through pull requests. Force pushes and branch deletion are blocked, and review threads must be resolved before merging. |
| Ruleset on `v*` tags | Release tags can't be moved or deleted. |
| Secret scanning with push protection | Pushes that contain known secret formats are rejected. |
| Local secret scan | A gitleaks pre-commit hook in `.githooks/` stops secrets before they are committed (enable it with `git config core.hooksPath .githooks`). |
| Dependabot | Vulnerability alerts and automatic security-fix pull requests. |
| GitHub Actions | The workflow token is read-only by default, workflows can't approve pull requests, and workflows from outside contributors wait for approval. |
| `.gitignore` | Keystores, signing properties, `.env` files and local Claude Code state are never committed. |

These settings are applied from code by [`tools/github/apply-repo-settings.sh`](tools/github/apply-repo-settings.sh) and [`.github/rulesets/`](.github/rulesets/).
