# How we work

This is a solo project, but it is run the way a multi-squad team would run it, so the history shows how the work was done.

## Branches

| Branch | Purpose | Merges into |
|---|---|---|
| `master` | Reviewed releases only | — |
| `dev` | Integration: everything lands here first | `master`, through a release pull request |
| `feature/<name>` | New behaviour | `dev` |
| `fix/<name>` | Bug fixes | `dev` (a hotfix goes to `master`, then `master` is merged back into `dev`) |
| `chore/<name>` | Setup, tooling, docs, dependencies | `dev` |

Branches are short-lived and start from `dev`. Rulesets protect `master` and `dev`: no direct pushes, force pushes or deletions.

## Commits

- One logical step per commit, with its tests in the same commit.
- Subject: `type(scope): imperative summary`. The type is `feat`, `fix`, `chore`, `docs`, `test`, `refactor`, `build` or `ci`; the scope is the module or area.
- The body explains why and ends with a `Verified:` line saying how the change was checked: unit tests, Robolectric, emulator walk, R8 release build, or n/a for docs.
- Never use `--no-verify`, and never force-push a shared branch.

## Pull requests

- `feature/`, `fix/` and `chore/` branches target `dev`. Releases and hotfixes target `master`.
- Fill in the template: what and why, how it was verified, screenshots for UI changes, and AI notes.
- Pull requests are merged with a merge commit (squash and rebase are disabled), so each branch's commits stay in the history. The branch is then deleted; the pull request keeps the record.
- All review threads must be resolved, and the **Build and test** and **Secret scan** checks must pass, before merging.

## Releases

1. Bump `mrd.versionName` and `mrd.versionCode` in `gradle.properties`, the one place the app's version comes from, in a pull request into `dev`.
2. Open a release pull request from `dev` to `master`.
3. After it merges, get the owner's go-ahead before tagging: a tag publishes a release, and release tags can't be moved or deleted.
4. Tag `master` as `vX.Y.Z`. The tag runs [`release.yml`](.github/workflows/release.yml), in the `release` environment, which only `v*` tags can deploy from. It builds the R8-minified APK, signed with the upload key from that environment's secrets, checks that R8 found no missing keep rules and that the APK's certificate is the upload key's, and creates the GitHub Release with the APK attached. R8's `mapping.txt` stays with the workflow run.

Dependabot's security-fix pull requests, for direct dependencies in the version catalog, target `master`. After one merges, merge `master` back into `dev`, as with a hotfix.

## Local setup

```sh
git config core.hooksPath .githooks   # gitleaks secret scan on every commit (brew install gitleaks)
```

Repository settings are code: see [`tools/github/apply-repo-settings.sh`](tools/github/apply-repo-settings.sh) and [`.github/rulesets/`](.github/rulesets/).
