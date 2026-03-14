import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
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
            packageName = "Krop"
            packageVersion = "1.0.0"
            description = "Krop Desktop Image Annotation Tool"
            copyright = "© 2024 Ghost"
            vendor = "Ghost"

            // This adds the icon to the installer and the installed app
            // Place an icon.ico (Win) or icon.icns (Mac) in src/jvmMain/resources
//            iconFile.set(project.file("src/jvmMain/resources/icon.ico"))

            windows {
                shortcut = true
                menu = true
                menuGroup = "Krop"
                // Keep this UUID constant for future versions of Krop
                upgradeUuid = "6f8c2b1a-83d4-4e2a-9b1c-3d5f7a2e8c1b"
                iconFile.set(project.file("src/jvmMain/composeResources/drawable/krop.png"))
            }

            macOS {
                bundleID = "com.ghost.krop"
                dockName = "Krop"
                signing {
                    // This is only needed if you have an Apple Developer account
//                    bundleElementSigning {
//                        enabled.set(false)
//                    }
                }
                iconFile.set(project.file("src/jvmMain/composeResources/drawable/krop.png"))
//                iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
            }

            linux {
                shortcut = true
                packageName = "krop" // Linux packages are usually lowercase
                appRelease = "1"
                appCategory = "Utility"
                menuGroup = "Office"
//                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
            }
        }
    }

}



buildkonfig {
    packageName = "com.ghost.krop"

    // Get build time
    val buildTime = System.currentTimeMillis().toString()
    val buildDate = LocalDateTime.now().toString()
    val userName = System.getProperty("user.name", "unknown")

    defaultConfigs {
        buildConfigField(Type.STRING, "APP_NAME", "Krop")
        buildConfigField(Type.STRING, "VERSION", "1.0.0")

        // Build information
        buildConfigField(Type.STRING, "BUILD_TIME", "\"$buildTime\"")
        buildConfigField(Type.STRING, "BUILD_DATE", "\"$buildDate\"")
        buildConfigField(Type.STRING, "BUILD_USER", "\"$userName\"")

        // Changed BUILD_HOST to a static string since we removed runCommand
        buildConfigField(Type.STRING, "BUILD_HOST", "\"unknown\"")

        buildConfigField(Type.BOOLEAN, "DEBUG", "true")
        buildConfigField(Type.STRING, "BUILD_TYPE", "\"unknown\"")

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