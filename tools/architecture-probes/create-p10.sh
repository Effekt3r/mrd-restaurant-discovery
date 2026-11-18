#!/usr/bin/env bash
# P10: nullable-fee money conversions, a KDoc line that must not count, and internal ViewModel state.
# Run from the repository root; remove with remove-probes.sh.
set -euo pipefail
mkdir -p feature/probe/src/main/kotlin/com/effekt3r/mrd/feature/probe
cat > feature/probe/src/main/kotlin/com/effekt3r/mrd/feature/probe/ProbeRound3.kt <<'EOF'
package com.effekt3r.mrd.feature.probe

/** Formats cents as rands (never cents / 100.0): a KDoc line, allowed. */
fun r1(deliveryFeeCents: Long?) = deliveryFeeCents?.let { it / 100.0 } // flagged
fun r2(deliveryFeeCents: Long?) = (deliveryFeeCents ?: 0L) / 100.0 // flagged
fun minutes(seconds: Long) = seconds / 60 // allowed

class ProbeRound3ViewModel : ViewModel() {
    internal val exposed: MutableStateFlow<Int> = MutableStateFlow(0) // flagged: internal counts
    val items: StateFlow<MutableList<Int>> = TODO() // flagged: a mutable type inside StateFlow
    private val hidden: MutableStateFlow<Int> = MutableStateFlow(0) // allowed: private
}
EOF
echo "probe P10 created"
