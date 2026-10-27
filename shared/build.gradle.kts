plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()
    
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.serializationJson)
        }
        jvmMain.dependencies {
            implementation(libs.pdfbox)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
// `-Pescriba.layoutPreviewDir=<pasta>` grava PNGs das páginas do PDF de teste da diagramação para conferência visual.
tasks.withType<Test>().configureEach {
    providers.gradleProperty("escriba.layoutPreviewDir").orNull?.let { systemProperty("escriba.layoutPreviewDir", it) }
}
