package com.effekt3r.mrd

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.declaration.KoPropertyDeclaration
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * House rules that would otherwise only live in code review. Each test names the rule it guards and
 * fails with the offending files, so CI catches a violation whoever (or whatever) wrote the code.
 *
 * Konsist reads the sources straight from disk, which Gradle can't see, so app/build.gradle.kts declares
 * them as inputs of the unit-test task. Without that, Gradle reports UP-TO-DATE (or a cached pass) after
 * a change to a module that isn't on :app's classpath.
 */
class ArchitectureTest {

    // Only this checkout's Gradle modules: never build-logic, and never nested git worktrees
    // (.worktrees/, .claude/worktrees/), which Konsist would otherwise scan as extra modules.
    private val production = Konsist.scopeFromProduction().slice { isProjectModule(it.moduleName) }.also { scope ->
        check(scope.files.any { it.moduleName == "app" }) {
            "Konsist reported no :app sources, so the module filter is wrong and every rule would pass unchecked."
        }
    }

    @Test
    fun featureModulesNeverImportAnotherFeature() {
        val violations = production.files
            .filter { it.moduleName.startsWith("feature/") }
            .flatMap { file ->
                val ownPackage =
                    "$ROOT_PACKAGE.feature." + file.moduleName.removePrefix("feature/").substringBefore('/')
                file.imports
                    .map { it.name }
                    .filter { it.startsWith("$ROOT_PACKAGE.feature.") }
                    .filterNot { it == ownPackage || it.startsWith("$ownPackage.") }
                    .map { "${file.projectPath} imports $it" }
            }
        assertNoViolations("Features are connected by :app, never by importing each other.", violations)
    }

    @Test
    fun modelAndCommonModulesHaveNoAndroidDependencies() {
        val violations = production.files
            .filter { it.moduleName == "core/model" || it.moduleName == "core/common" }
            .flatMap { file ->
                file.imports
                    .map { it.name }
                    .filter { it.startsWith("android.") || it.startsWith("androidx.") }
                    .map { "${file.projectPath} imports $it" }
            }
        assertNoViolations(":core:model and :core:common are plain Kotlin, so their tests run on the JVM.", violations)
    }

    @Test
    fun rawColoursOnlyAppearInTheDesignSystem() {
        val violations = production.files
            .filter { it.moduleName != "core/designsystem" }
            .flatMap { it.linesMatching(RAW_COLOUR) }
        assertNoViolations(
            "Colours come from design-system tokens, so both themes stay consistent. Color(...) values and named " +
                "Color constants (other than Transparent and Unspecified) belong in :core:designsystem.",
            violations
        )
    }

    @Test
    fun moneyIsNeverAFloatingPointNumber() {
        val violations = production.files.flatMap {
            it.linesMatching(MONEY_TYPED_AS_FLOATING_POINT, MONEY_FLOATING_POINT_LITERAL, CENTS_TO_FLOATING_POINT)
        }
        assertNoViolations("Money is integer cents (Long), never Double or Float, so amounts stay exact.", violations)
    }

    @Test
    fun viewModelsNeverExposeMutableState() {
        val violations = production.classes()
            .filter { it.name.endsWith("ViewModel") || it.hasParentWithName("ViewModel", "AndroidViewModel") }
            .flatMap { viewModel ->
                val properties = viewModel.properties(includeNested = false)
                    .filter { it.hasPublicOrDefaultModifier }
                    .mapNotNull { property ->
                        val type = property.type?.name
                        val problem = when {
                            type == null -> "has no explicit type"
                            MUTABLE_STATE_TYPE.containsMatchIn(type) -> "is a $type"
                            property.isVar && property.setter?.hasPrivateModifier != true -> "has a public setter"
                            else -> null
                        }
                        problem?.let { "${viewModel.projectPath}: ${viewModel.name}.${property.name} $it" }
                    }
                val functions = viewModel.functions(includeNested = false, includeLocal = false)
                    .filter { it.hasPublicOrDefaultModifier }
                    .filter { it.returnType?.name?.let(MUTABLE_STATE_TYPE::containsMatchIn) == true }
                    .map { "${viewModel.projectPath}: ${viewModel.name}.${it.name}() returns ${it.returnType?.name}" }
                properties + functions
            }
        assertNoViolations(
            "ViewModels expose read-only state (StateFlow, never MutableStateFlow) with an explicit type, so the " +
                "UI can't change it; changes go through the ViewModel's functions.",
            violations
        )
    }

    @Test
    fun repositoryImplementationsAreInternal() {
        val violations = production.classesAndObjects()
            .filter { isRepository(it.name) || it.hasParent { parent -> isRepository(parent.name) } }
            .filter { it.moduleName != "core/testing" && (it.hasPublicOrDefaultModifier || it.hasProtectedModifier) }
            .map { "${it.projectPath}: ${it.name}" }
        assertNoViolations(
            "Repository implementations are internal to :core:data, so other modules depend on its interfaces " +
                "and tests can swap in fakes. Only the fakes in :core:testing are public.",
            violations
        )
    }

    @Test
    fun modelTypesAreImmutable() {
        val members = production.classesAndInterfacesAndObjects()
            .filter { it.resideInPackage(MODEL_PACKAGE) }
            .flatMap { declaration ->
                val name = declaration.fullyQualifiedName?.removePrefix("$ROOT_PACKAGE.core.model.") ?: declaration.name
                declaration.properties(includeNested = false).mapNotNull { property ->
                    mutabilityOf(property)?.let { "${declaration.projectPath}: $name.${property.name} $it" }
                }
            }
        val topLevel = production.properties()
            .filter { it.isTopLevel && it.resideInPackage(MODEL_PACKAGE) }
            .mapNotNull { property -> mutabilityOf(property)?.let { "${property.projectPath}: ${property.name} $it" } }
        assertNoViolations(
            "compose-stability.conf marks com.effekt3r.mrd.core.model stable, so Compose skips recomposition when " +
                "these values are equal; they must stay immutable (no var, no mutable collections or arrays).",
            members + topLevel
        )
    }

    private fun mutabilityOf(property: KoPropertyDeclaration): String? {
        val type = property.type?.name
        return when {
            property.isVar -> "is a var"
            type != null && MUTABLE_MODEL_TYPE.containsMatchIn(type) -> "has the mutable type $type"
            type == null && MUTABLE_INITIALISER.containsMatchIn(property.text) -> "holds a mutable value"
            else -> null
        }
    }

    /** Each line that matches any of [patterns], as "path:line: code", so a failure points at the line. */
    private fun KoFileDeclaration.linesMatching(vararg patterns: Regex): List<String> {
        val lines = text.lines()
        return patterns
            .flatMap { pattern -> pattern.findAll(text).map { text.lineIndexAt(it.range.first) }.toList() }
            .distinct()
            .sorted()
            .map { index -> "$projectPath:${index + 1}: ${lines[index].trim()}" }
    }

    private fun String.lineIndexAt(offset: Int) = substring(0, offset).count { it == '\n' }

    private fun assertNoViolations(rule: String, violations: List<String>) {
        assertTrue("$rule\n" + violations.joinToString("\n"), violations.isEmpty())
    }

    private companion object {
        const val ROOT_PACKAGE = "com.effekt3r.mrd"

        /** The package compose-stability.conf declares stable, with its sub-packages (Konsist's `..`). */
        const val MODEL_PACKAGE = "$ROOT_PACKAGE.core.model.."

        // Any Color(...) constructor call, whichever overload, and named constants such as Color.White.
        // Transparent and Unspecified (and android.graphics.Color.TRANSPARENT) aren't colour choices.
        val RAW_COLOUR = Regex("""\bColor\s*\(|\bColor\.(?!Transparent\b|Unspecified\b|TRANSPARENT\b)\w+""")

        // A money name: fee, price, cents, cost, total or subtotal as the whole name or its last camelCase
        // word (deliveryFee, minimumOrderCents), so priceAlpha, feedOffset and distanceFeet don't match.
        const val MONEY_NAME =
            """(?:\w*[a-z0-9](?:Fees?|Prices?|Cents|Costs?|Total|Subtotal)|fees?|prices?|cents|costs?|total|subtotal)"""

        // deliveryFee: Double, fun totalPrice(items: List<Item>): Double, price: kotlin.Float
        val MONEY_TYPED_AS_FLOATING_POINT =
            Regex("""\b$MONEY_NAME\s*(?:\((?:[^()]|\([^()]*\))*\))?\s*:\s*(?:kotlin\.)?(?:Double|Float)\b""")

        // val deliveryFee = 2.99 or val price = 15f: an inferred Double or Float
        val MONEY_FLOATING_POINT_LITERAL =
            Regex("""\b(?:val|var)\s+$MONEY_NAME\s*=\s*-?(?:\d+\.\d+|\d+(?=[fF]))[fF]?\b""")

        // deliveryFeeCents / 100.0, cents / 100f, feeCents?.toDouble(); integer cents / 100 is fine
        val CENTS_TO_FLOATING_POINT =
            Regex("""\b(?:\w*[a-z0-9]Cents|cents)\s*(?:/\s*100(?:\.\d+[fF]?|[fF])\b|\??\.to(?:Double|Float)\(\))""")

        // MutableStateFlow, MutableSharedFlow, MutableState, Channel, SnapshotStateList..., qualified or not
        val MUTABLE_STATE_TYPE = Regex("""^(?:[\w.]+\.)?(?:Mutable\w*|Channel|SendChannel|SnapshotState\w*)\b""")

        // A mutable type anywhere in a declared type: List<MutableList<T>>, kotlin.collections.MutableSet<T>, IntArray
        val MUTABLE_MODEL_TYPE = Regex(
            """(?:^|[<,(\s.])(?:Mutable\w*|ArrayList|ArrayDeque|HashMap|HashSet|LinkedHashMap|LinkedHashSet|""" +
                """Array|[A-Z]\w*Array)\b"""
        )

        // An inferred property initialised with a mutable collection or array: = mutableListOf<T>(), = arrayOf(...)
        val MUTABLE_INITIALISER = Regex(
            """=\s*(?:mutable\w*Of|\w*[aA]rrayOf\w*|emptyArray|arrayListOf|hashMapOf|hashSetOf|linkedMapOf|""" +
                """linkedSetOf|ArrayList|ArrayDeque|HashMap|HashSet|LinkedHashMap|LinkedHashSet|Mutable\w*)\s*[<(]"""
        )

        /** This checkout's Gradle modules, by Konsist's module name: the path from the project root to src/. */
        fun isProjectModule(moduleName: String) =
            moduleName == "app" || moduleName.startsWith("core/") || moduleName.startsWith("feature/")

        fun isRepository(name: String) =
            name.substringBefore('<').let { it.endsWith("Repository") || it.endsWith("RepositoryImpl") }
    }
}
