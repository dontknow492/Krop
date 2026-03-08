import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

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

    // This is the "Base" configuration
    defaultConfigs {
        // Notice: No quotes around Type.STRING
        buildConfigField(Type.STRING, "APP_NAME", "Krop")
        buildConfigField(Type.STRING, "VERSION", "1.0.0")
        targetConfigs {
            create("debug") {
                buildConfigField(Type.BOOLEAN, "IS_DEBUG", "true")
                buildConfigField(Type.STRING, "BUILD_TYPE", "debug")
            }

            create("release") {
                buildConfigField(Type.BOOLEAN, "IS_DEBUG", "false")
                buildConfigField(Type.STRING, "BUILD_TYPE", "release")
            }
        }
    }

}
