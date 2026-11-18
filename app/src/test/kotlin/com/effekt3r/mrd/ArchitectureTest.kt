package com.effekt3r.mrd

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * House rules that would otherwise only live in code review. Each test names the rule it guards and
 * fails with the offending files, so CI catches a violation whoever (or whatever) wrote the code.
 */
class ArchitectureTest {

    private val production = Konsist.scopeFromProduction()
    private val sourceFiles = production.files.filter { "/build-logic/" !in it.path }

    @Test
    fun featureModulesNeverImportAnotherFeature() {
        val violations = sourceFiles
            .filter { "/feature/" in it.path }
            .flatMap { file ->
                val ownFeature = file.packagee?.name.orEmpty()
                    .removePrefix("$ROOT_PACKAGE.feature.")
                    .substringBefore('.')
                file.imports
                    .map { it.name }
                    .filter { it.startsWith("$ROOT_PACKAGE.feature.") }
                    .filterNot { it.startsWith("$ROOT_PACKAGE.feature.$ownFeature.") }
                    .map { "${file.path} imports $it" }
            }
        assertNoViolations("Features are connected by :app, never by importing each other.", violations)
    }

    @Test
    fun modelAndCommonModulesHaveNoAndroidDependencies() {
        val violations = sourceFiles
            .filter { "/core/model/" in it.path || "/core/common/" in it.path }
            .flatMap { file ->
                file.imports
                    .map { it.name }
                    .filter { it.startsWith("android.") || it.startsWith("androidx.") }
                    .map { "${file.path} imports $it" }
            }
        assertNoViolations(":core:model and :core:common are plain Kotlin, so their tests run on the JVM.", violations)
    }

    @Test
    fun rawColoursOnlyAppearInTheDesignSystem() {
        val violations = sourceFiles
            .filter { "/core/designsystem/" !in it.path && "Color(0x" in it.text }
            .map { it.path }
        assertNoViolations(
            "Colours come from design-system tokens; raw Color(0x...) values belong in :core:designsystem.",
            violations
        )
    }

    @Test
    fun moneyIsNeverAFloatingPointNumber() {
        val moneyAsFloat = Regex("""\b\w*(?:[Cc]ents|[Ff]ee|[Pp]rice)\w*\s*:\s*(?:Double|Float)\b""")
        val violations = sourceFiles.flatMap { file ->
            moneyAsFloat.findAll(file.text).map { "${file.path}: ${it.value}" }.toList()
        }
        assertNoViolations("Money is integer cents (Long), never Double or Float.", violations)
    }

    @Test
    fun viewModelsNeverExposeMutableState() {
        val mutableInitialisers = listOf("MutableStateFlow(", "MutableSharedFlow(", "mutableStateOf(")
        val violations = production.classes()
            .withNameEndingWith("ViewModel")
            .flatMap { viewModel ->
                viewModel.properties()
                    .filter { it.hasPublicOrDefaultModifier }
                    .filter { property ->
                        val declaredMutable = property.type?.name?.startsWith("Mutable") == true
                        val inferredMutable = property.type == null && mutableInitialisers.any { it in property.text }
                        declaredMutable || inferredMutable
                    }
                    .map { "${viewModel.path}: ${viewModel.name}.${it.name}" }
            }
        assertNoViolations("ViewModels expose read-only state; changes go through their functions.", violations)
    }

    @Test
    fun repositoryImplementationsAreInternal() {
        val violations = production.classes()
            .withNameEndingWith("Repository")
            .filter { "/core/testing/" !in it.path && !it.hasInternalModifier }
            .map { "${it.path}: ${it.name}" }
        assertNoViolations(
            "Repository implementations are internal to :core:data; other modules see interfaces only.",
            violations
        )
    }

    @Test
    fun modelTypesAreImmutable() {
        val mutableTypes = Regex("""^(Mutable\w*|ArrayList|HashMap|HashSet|LinkedHashMap|LinkedHashSet)\b""")
        val violations = production.classes()
            .filter { "/core/model/" in it.path }
            .flatMap { model ->
                model.properties()
                    .filter { property ->
                        val isVar = Regex("""\bvar\b""").containsMatchIn(property.text.substringBefore('='))
                        val isMutableType = property.type?.name?.let { mutableTypes.containsMatchIn(it) } == true
                        isVar || isMutableType
                    }
                    .map { "${model.path}: ${model.name}.${it.name}" }
            }
        assertNoViolations(
            "compose-stability.conf marks :core:model stable, so its classes must stay immutable " +
                "(no var properties, no mutable collections).",
            violations
        )
    }

    private fun assertNoViolations(rule: String, violations: List<String>) {
        assertTrue("$rule\n" + violations.joinToString("\n"), violations.isEmpty())
    }

    private companion object {
        const val ROOT_PACKAGE = "com.effekt3r.mrd"
    }
}
