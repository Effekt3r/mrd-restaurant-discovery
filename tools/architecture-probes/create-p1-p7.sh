#!/usr/bin/env bash
# Creates architecture-test probes P1-P7 in the current repo root. Remove them with remove-probes.sh.
set -euo pipefail
mkdir -p feature/probe/src/main/kotlin/com/effekt3r/mrd/feature/details feature/probe/src/main/kotlin/com/effekt3r/mrd/feature/probe \
  core/common/src/main/kotlin/com/effekt3r/mrd/core/common core/data/src/main/kotlin/com/effekt3r/mrd/core/data \
  core/model/src/main/kotlin/com/effekt3r/mrd/core/model \
  .worktrees/other-branch/core/model/src/main/kotlin/com/effekt3r/mrd/core/model

cat > feature/probe/src/main/kotlin/com/effekt3r/mrd/feature/details/ProbeCrossFeature.kt <<'EOF'
// In feature/probe but declares the details package: the old package-based check let this through.
package com.effekt3r.mrd.feature.details

import com.effekt3r.mrd.feature.details.navigation.RestaurantRoute // flagged: another feature
import com.effekt3r.mrd.feature.probe.ProbeThing // allowed: its own feature

val probeRoute = RestaurantRoute("r-1")
EOF

cat > core/common/src/main/kotlin/com/effekt3r/mrd/core/common/ProbeAndroid.kt <<'EOF'
package com.effekt3r.mrd.core.common

import androidx.annotation.VisibleForTesting // flagged

@VisibleForTesting
internal fun probeAndroidImport() = Unit
EOF

cat > feature/probe/src/main/kotlin/com/effekt3r/mrd/feature/probe/ProbeColours.kt <<'EOF'
package com.effekt3r.mrd.feature.probe

import androidx.compose.ui.graphics.Color

val upperCaseHex = Color(0XFF1D1D1B) // flagged
val namedArgument = Color(color = 0xFF6DCFF6) // flagged
val components = Color(red = 0.1f, green = 0.2f, blue = 0.3f) // flagged
val namedConstant = Color.White.copy(alpha = 0.5f) // flagged
val transparent = Color.Transparent // allowed
val unspecified = Color.Unspecified // allowed
val notAColour = lightenColor(0x20) // allowed: not the Color constructor
EOF

cat > feature/probe/src/main/kotlin/com/effekt3r/mrd/feature/probe/ProbeMoney.kt <<'EOF'
package com.effekt3r.mrd.feature.probe

class ProbeMoney(val deliveryFee: Double) { // flagged
    fun totalPrice(items: List<Long>): Double = items.sum().toDouble() // flagged
    val minimumOrderCents: Float? = null // flagged
    val price = 12.5 // flagged: an inferred Double
    fun rands(deliveryFeeCents: Long) = deliveryFeeCents / 100.0 // flagged
    val priceAlpha: Float = 0.5f // allowed
    val feedScrollOffset: Float = 0f // allowed
    val dimAmount: Float = 0.4f // allowed
    val rating: Double = 4.6 // allowed
    val deliveryFeeCents: Long? = 1500 // allowed
    fun wholeRands(cents: Long) = cents / 100 // allowed: integer division
}
EOF

cat > feature/probe/src/main/kotlin/com/effekt3r/mrd/feature/probe/ProbeViewModel.kt <<'EOF'
package com.effekt3r.mrd.feature.probe

class ProbeViewModel(
    private val repository: Any, // allowed: private
    savedStateHandle: Any // allowed: a parameter, not a property
) : ViewModel() {
    private val _uiState = MutableStateFlow(0) // allowed: private
    val uiState = _uiState // flagged: no explicit type (it's the MutableStateFlow)
    val readOnly: StateFlow<Int> = _uiState.asStateFlow() // allowed
    val events: Channel<Int> = Channel() // flagged
    var selectedId: String? = null // flagged: public setter
    var query: String by mutableStateOf("") // allowed: private setter
        private set
    val searchField: TextFieldState = TextFieldState() // allowed: spec section 4.4
    fun state(): MutableStateFlow<Int> = _uiState // flagged
}

class ProbeScreenState : ViewModel() {
    val count: kotlinx.coroutines.flow.MutableStateFlow<Int> = MutableStateFlow(0) // flagged: found by its parent
}
EOF

cat > core/data/src/main/kotlin/com/effekt3r/mrd/core/data/ProbeRepositories.kt <<'EOF'
package com.effekt3r.mrd.core.data

interface RestaurantRepository // allowed: the public interface
class RestaurantRepositoryImpl : RestaurantRepository // flagged
class RestaurantStore : RestaurantRepository // flagged: implements a repository
object CachedRestaurantRepository : RestaurantRepository // flagged
internal class DefaultRestaurantRepository : RestaurantRepository // allowed
private class LocalRestaurantRepository : RestaurantRepository // allowed: private
abstract class RepositoryModule // allowed: not a repository
EOF

cat > core/model/src/main/kotlin/com/effekt3r/mrd/core/model/ProbeModel.kt <<'EOF'
package com.effekt3r.mrd.core.model

data class ProbeRestaurant(
    var name: String, // flagged
    val tags: Array<String>, // flagged
    val groups: List<MutableList<String>>, // flagged
    val ids: kotlin.collections.MutableSet<String>, // flagged
    /** Kept as a val rather than a var so Compose can skip it. */
    val note: String, // allowed (the old text check flagged its KDoc)
    @Deprecated("was a var") val legacy: Int // allowed (the old text check flagged its annotation)
) {
    private val cache = mutableListOf<String>() // flagged

    companion object {
        var instances = 0 // flagged
    }
}

object ProbeDefaults {
    var counter = 0 // flagged
}

interface ProbeShape {
    var size: Int // flagged
}

var probeTopLevel = 0 // flagged
EOF

# Control: a nested worktree. Its violation must NOT be reported.
cat > .worktrees/other-branch/core/model/src/main/kotlin/com/effekt3r/mrd/core/model/NestedCheckout.kt <<'EOF'
package com.effekt3r.mrd.core.model

data class NestedCheckout(var x: Int)
EOF
echo "probes P1-P7 and the nested-worktree control created"
