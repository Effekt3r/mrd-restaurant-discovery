#!/usr/bin/env bash
# Applies this repository's GitHub settings from code: merge policy and the rulesets in
# .github/rulesets/. Safe to re-run; rulesets are created or updated by name.
# Requires the gh CLI, authenticated as a repository admin, and jq.
set -euo pipefail

REPO="${1:-$(gh repo view --json nameWithOwner --jq .nameWithOwner)}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
echo "Configuring $REPO"

# Merge policy: merge commits only, so every branch's individual commits stay visible on
# dev and master. Merged branches are deleted; the pull request keeps the record.
gh api -X PATCH "repos/$REPO" \
  -F allow_merge_commit=true -F allow_squash_merge=false -F allow_rebase_merge=false \
  -f merge_commit_title=PR_TITLE -f merge_commit_message=BLANK \
  -F delete_branch_on_merge=true >/dev/null
echo "merge policy: merge commits only, branches deleted after merge"

# Rulesets: master and dev only change through pull requests; release tags are immutable.
for file in "$ROOT"/.github/rulesets/*.json; do
  name="$(jq -r .name "$file")"
  id="$(gh api "repos/$REPO/rulesets" --jq ".[] | select(.name == \"$name\") | .id")"
  if [ -n "$id" ]; then
    gh api -X PUT "repos/$REPO/rulesets/$id" --input "$file" >/dev/null
    echo "ruleset $name: updated"
  else
    gh api -X POST "repos/$REPO/rulesets" --input "$file" >/dev/null
    echo "ruleset $name: created"
  fi
done
