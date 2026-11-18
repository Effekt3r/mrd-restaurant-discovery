#!/usr/bin/env bash
# P8: Gradle-skip reproduction. A public repository in :core:data, a module that isn't on :app's test classpath.
set -euo pipefail
mkdir -p core/data/src/main/kotlin/com/effekt3r/mrd/core/data
cat > core/data/src/main/kotlin/com/effekt3r/mrd/core/data/PublicProbeRepository.kt <<'EOF'
package com.effekt3r.mrd.core.data

class PublicProbeRepository
EOF
echo "probe P8 created"
