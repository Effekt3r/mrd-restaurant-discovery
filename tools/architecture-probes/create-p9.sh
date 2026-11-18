#!/usr/bin/env bash
# P9: a raw colour in a nested module (feature/browse/impl), and nothing else. It proves Gradle reruns the
# architecture tests for a change in a nested module. Run from the repository root; remove with remove-probes.sh.
set -euo pipefail
mkdir -p feature/browse/impl/src/main/kotlin/com/effekt3r/mrd/feature/browse/impl
cat > feature/browse/impl/src/main/kotlin/com/effekt3r/mrd/feature/browse/impl/ProbeNested.kt <<'EOF'
package com.effekt3r.mrd.feature.browse.impl

import androidx.compose.ui.graphics.Color

// The scrim is Color.Black at 40% opacity: a comment line, allowed
val nestedColour = Color(0xFF000000) // flagged: a raw colour in a nested module
EOF
echo "probe P9 created"
