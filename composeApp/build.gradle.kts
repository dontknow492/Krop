import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.buildKonfig)
    alias(libs.plugins.serialization)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.jetbrains.compose.icons)
            implementation(libs.kotlinx.serialization.json)


            // KOin
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)


            //            filekit
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs.compose)
            // coil
            implementation(libs.coil.compose)
            implementation(libs.coil.network.okhttp)

            implementation(libs.buildKonfig)

            implementation(libs.reordable.item)
            implementation(libs.naphier)

            implementation(libs.compose.colorpicker)

        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}


compose.desktop {
    application {
        mainClass = "com.ghost.krop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.ghost.krop"
            packageVersion = "1.0.0"

        }
    }

}



buildkonfig {
    packageName = "com.ghost.krop"

    // Function to run shell commands
    fun runCommand(command: String): String {
        return try {
            val byteOut = ByteArrayOutputStream()
            project.exec {
                commandLine = command.split(" ")
                standardOutput = byteOut
                errorOutput = byteOut
                isIgnoreExitValue = true
            }
            byteOut.toString(Charsets.UTF_8).trim()
        } catch (e: Exception) {
            "unknown"
        }
    }

    // Get Git information
    val gitCommit = runCommand("git rev-parse --short HEAD")
    val gitBranch = runCommand("git rev-parse --abbrev-ref HEAD")
    val gitTag = runCommand("git describe --tags --always")
    val gitCommitCount = runCommand("git rev-list --count HEAD")
    val gitCommitTime = runCommand("git show -s --format=%ci HEAD")
    val isDirty = runCommand("git status --porcelain").isNotEmpty()

    // Get build time
    val buildTime = System.currentTimeMillis().toString()
    val buildDate = LocalDateTime.now().toString()

    // Get user/host info (optional)
    val userName = System.getProperty("user.name", "unknown")
    val hostName = runCommand("hostname").ifEmpty { "unknown" }

    defaultConfigs {
        // Static fields
        buildConfigField(Type.STRING, "APP_NAME", "Krop")
        buildConfigField(Type.STRING, "VERSION", "1.0.0")

        // Git information
        buildConfigField(Type.STRING, "GIT_COMMIT", "\"$gitCommit\"")
        buildConfigField(Type.STRING, "GIT_BRANCH", "\"$gitBranch\"")
        buildConfigField(Type.STRING, "GIT_TAG", "\"$gitTag\"")
        buildConfigField(Type.STRING, "GIT_COMMIT_COUNT", "\"$gitCommitCount\"")
        buildConfigField(Type.STRING, "GIT_COMMIT_TIME", "\"$gitCommitTime\"")
        buildConfigField(Type.BOOLEAN, "GIT_DIRTY", if (isDirty) "true" else "false")

        // Build information
        buildConfigField(Type.STRING, "BUILD_TIME", "\"$buildTime\"")
        buildConfigField(Type.STRING, "BUILD_DATE", "\"$buildDate\"")
        buildConfigField(Type.STRING, "BUILD_USER", "\"$userName\"")
        buildConfigField(Type.STRING, "BUILD_HOST", "\"$hostName\"")

        buildConfigField(Type.BOOLEAN, "DEBUG", "false")  // Default value
        buildConfigField(Type.STRING, "BUILD_TYPE", "\"unknown\"")  // Default value

        // Target-specific configs
        targetConfigs {
            create("debug") {
                buildConfigField(Type.BOOLEAN, "DEBUG", "true")
                buildConfigField(Type.STRING, "BUILD_TYPE", "\"debug\"")
                buildConfigField(Type.BOOLEAN, "LOGGING_ENABLED", "true")
            }

            create("release") {
                buildConfigField(Type.BOOLEAN, "DEBUG", "false")
                buildConfigField(Type.STRING, "BUILD_TYPE", "\"release\"")
                buildConfigField(Type.BOOLEAN, "LOGGING_ENABLED", "false")
            }
        }
    }
}