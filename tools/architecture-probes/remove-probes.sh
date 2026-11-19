#!/usr/bin/env bash
# Removes exactly the probe files the create-p*.sh scripts write, then prunes directories left empty.
# It never deletes anything else, so it is safe in the main checkout, where .worktrees/ holds real worktrees.
set -euo pipefail
rm -f \
  feature/probe/src/main/kotlin/com/effekt3r/mrd/feature/details/ProbeCrossFeature.kt \
  feature/probe/src/main/kotlin/com/effekt3r/mrd/feature/probe/ProbeColours.kt \
  feature/probe/src/main/kotlin/com/effekt3r/mrd/feature/probe/ProbeMoney.kt \
  feature/probe/src/main/kotlin/com/effekt3r/mrd/feature/probe/ProbeViewModel.kt \
  feature/probe/src/main/kotlin/com/effekt3r/mrd/feature/probe/ProbeRound3.kt \
  core/common/src/probe/kotlin/com/effekt3r/mrd/core/common/ProbeAndroid.kt \
  core/data/src/probe/kotlin/com/effekt3r/mrd/core/data/ProbeRepositories.kt \
  core/data/src/probe/kotlin/com/effekt3r/mrd/core/data/PublicProbeRepository.kt \
  core/model/src/probe/kotlin/com/effekt3r/mrd/core/model/ProbeModel.kt \
  feature/browse/impl/src/main/kotlin/com/effekt3r/mrd/feature/browse/impl/ProbeNested.kt \
  .worktrees/zz-architecture-probe-control/core/model/src/main/kotlin/com/effekt3r/mrd/core/model/NestedCheckout.kt
# Prune only directories that are now empty, starting from the probe roots.
for d in feature/probe core/common/src/probe core/data/src/probe core/model/src/probe feature/browse/impl \
  .worktrees/zz-architecture-probe-control; do
  if [ -d "$d" ]; then find "$d" -depth -type d -empty -delete; fi
done
# Parents that only existed for the probes (never removed if anything else is inside).
for d in core/common/src core/data/src core/model/src .worktrees; do rmdir "$d" 2>/dev/null || true; done
echo "probes removed"
git status --porcelain
