/*
Testing:

./gradlew build
./gradlew run --args="/Users/tonywu/IdeaProjects/JavatoKotlin/src/main/kotlin/Sample.kt"

./gradlew clean shadowJar
java -jar main.jar /Sample.kt

 */

package com.example

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.lexer.KtTokens
import java.io.File
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer

fun createKotlinEnvironment(): KotlinCoreEnvironment {
    val configuration = CompilerConfiguration().apply {
        put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
    }
    val disposable = Disposer.newDisposable()
    return KotlinCoreEnvironment.createForProduction(
        disposable,
        configuration,
        EnvironmentConfigFiles.JVM_CONFIG_FILES
    )
}

fun parseKotlinFile(environment: KotlinCoreEnvironment, file: File): KtFile {
    val fileContent = file.readText()
    val psiFactory = KtPsiFactory(environment.project)
    return psiFactory.createFile(fileContent)
}

// This function returns a concise signature for a declaration.
fun printSignature(declaration: KtNamedDeclaration): String {
    return when (declaration) {
        is KtFunction -> {
            val name = declaration.name ?: "<anonymous>"
            val params = declaration.valueParameters.joinToString(", ") { it.name ?: "_" }
            "fun $name($params)"
        }
        is KtClass -> {
            val name = declaration.name ?: "<anonymous>"
            val builder = StringBuilder("class $name")
            // Look for public functions inside the class.
            val publicFunctions = declaration.declarations.filterIsInstance<KtFunction>().filter {
                it.modifierList?.hasModifier(KtTokens.PUBLIC_KEYWORD) ?: true
            }
            if (publicFunctions.isNotEmpty()) {
                builder.append(" {")
                publicFunctions.forEach { function ->
                    val fname = function.name ?: "<anonymous>"
                    val params = function.valueParameters.joinToString(", ") { it.name ?: "_" }
                    builder.append("\n   fun $fname($params)")
                }
                builder.append("\n}")
            }
            builder.toString()
        }
        else -> declaration.text // fallback for other declaration types
    }
}

fun printPublicDeclarations(ktFile: KtFile) {
    ktFile.declarations.forEach { declaration ->
        if (declaration is KtNamedDeclaration) {
            // If no modifier list is present, it's public by default.
            if (declaration.modifierList?.hasModifier(KtTokens.PUBLIC_KEYWORD) ?: true) {
                println(printSignature(declaration))
            }
        }
    }
}

fun collectKotlinFiles(path: String): List<File> {
    val file = File(path)
    return if (file.isDirectory) {
        file.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    } else if (file.isFile && file.extension == "kt") {
        listOf(file)
    } else {
        emptyList()
    }
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: Provide the path to a Kotlin source file or directory.")
        return
    }

    val path = args[0]
    val kotlinFiles = collectKotlinFiles(path)
    if (kotlinFiles.isEmpty()) {
        println("No Kotlin files found at: $path")
        return
    }

    val environment = createKotlinEnvironment()
    try {
        kotlinFiles.forEach { file ->
            try {
                val ktFile = parseKotlinFile(environment, file)
                printPublicDeclarations(ktFile)
            } catch (e: Exception) {
                println("Error processing file ${file.absolutePath}: ${e.message}")
            }
        }
    } finally {
        Disposer.dispose(environment.project)
    }
}
