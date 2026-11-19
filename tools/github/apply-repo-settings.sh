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

# Public-repo security.
# Secret scanning with push protection: pushes containing known secret formats are rejected.
gh api -X PATCH "repos/$REPO" \
  -f 'security_and_analysis[secret_scanning][status]=enabled' \
  -f 'security_and_analysis[secret_scanning_push_protection][status]=enabled' >/dev/null
# Dependabot alerts and automatic security-fix pull requests.
gh api -X PUT "repos/$REPO/vulnerability-alerts" >/dev/null
gh api -X PUT "repos/$REPO/automated-security-fixes" >/dev/null
# Vulnerabilities are reported privately (Security tab), never in public issues.
gh api -X PUT "repos/$REPO/private-vulnerability-reporting" >/dev/null
# Unused features stay off to keep the public surface small.
gh api -X PATCH "repos/$REPO" -F has_wiki=false -F has_projects=false >/dev/null
echo "security: secret scanning + push protection, Dependabot alerts + fixes, private reporting"

# GitHub Actions: the workflow token is read-only unless a job asks for more, workflows
# can't approve pull requests, and workflows from outside contributors wait for approval.
gh api -X PUT "repos/$REPO/actions/permissions/workflow" \
  -f default_workflow_permissions=read -F can_approve_pull_request_reviews=false >/dev/null
gh api -X PUT "repos/$REPO/actions/permissions/fork-pr-contributor-approval" \
  -f approval_policy=all_external_contributors >/dev/null
echo "actions: read-only token, no PR approvals, outside contributors need approval"

# Actions supply chain: only GitHub-owned actions and gradle/actions may run, and every action must be
# pinned to a full commit SHA.
gh api -X PUT "repos/$REPO/actions/permissions" \
  -F enabled=true -f allowed_actions=selected -F sha_pinning_required=true >/dev/null
gh api -X PUT "repos/$REPO/actions/permissions/selected-actions" \
  -F github_owned_allowed=true -F verified_allowed=false -f 'patterns_allowed[]=gradle/actions/*' >/dev/null
echo "actions: GitHub-owned and gradle/actions only, pinned to commit SHAs"
