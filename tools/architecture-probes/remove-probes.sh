#!/usr/bin/env bash
# Removes every probe file and the now-empty directories the probes created. It deletes only the exact probe
# files (plus the probe-only feature/probe and .worktrees/other-branch folders), never tracked or real code.
set -euo pipefail
rm -rf feature/probe .worktrees/other-branch
rm -f core/common/src/main/kotlin/com/effekt3r/mrd/core/common/ProbeAndroid.kt \
      core/data/src/main/kotlin/com/effekt3r/mrd/core/data/ProbeRepositories.kt \
      core/data/src/main/kotlin/com/effekt3r/mrd/core/data/PublicProbeRepository.kt \
      core/model/src/main/kotlin/com/effekt3r/mrd/core/model/ProbeModel.kt \
      feature/browse/impl/src/main/kotlin/com/effekt3r/mrd/feature/browse/impl/ProbeNested.kt
for d in core/common/src core/data/src core/model/src feature/browse/impl; do
  [ -d "$d" ] && find "$d" -type d -empty -delete
done
rmdir .worktrees 2>/dev/null || true
echo "probes removed"
git status --porcelain
