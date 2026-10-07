import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// region Versioning

/*
 * La version est lue depuis version.properties (Semantic Versioning).
 *   versionName = MAJEUR.MINEUR.CORRECTIF            ex. 1.4.2
 *   versionCode = MAJEUR * 10000 + MINEUR * 100 + CORRECTIF   ex. 10402
 * Le versionCode reste ainsi strictement croissant (MINEUR et CORRECTIF < 100).
 */
val versionFile: File = rootProject.file("version.properties")
val versionProps = Properties().apply { versionFile.inputStream().use { load(it) } }
val versionMajor = versionProps.getProperty("VERSION_MAJOR").toInt()
val versionMinor = versionProps.getProperty("VERSION_MINOR").toInt()
val versionPatch = versionProps.getProperty("VERSION_PATCH").toInt()

val appVersionName = "$versionMajor.$versionMinor.$versionPatch"
val appVersionCode = versionMajor * 10_000 + versionMinor * 100 + versionPatch

// endregion

android {
    namespace = "com.ynov.helloworld"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.ynov.helloworld"
        minSdk = 24
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
        }
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.maplibre.android)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// region Tâches de versioning

/*
 * ./gradlew bumpMajor   1.4.2 -> 2.0.0   changement incompatible
 * ./gradlew bumpMinor   1.4.2 -> 1.5.0   nouvelle fonctionnalité
 * ./gradlew bumpPatch   1.4.2 -> 1.4.3   correction de bug
 * ./gradlew printVersion                 affiche la version courante
 */
listOf("Major", "Minor", "Patch").forEach { part ->
    tasks.register("bump$part") {
        group = "versioning"
        description = "Incrémente la version ${part.lowercase()} dans version.properties."
        val file = versionFile
        doLast {
            val props = Properties().apply { file.inputStream().use { load(it) } }
            var major = props.getProperty("VERSION_MAJOR").toInt()
            var minor = props.getProperty("VERSION_MINOR").toInt()
            var patch = props.getProperty("VERSION_PATCH").toInt()
            when (part) {
                "Major" -> { major++; minor = 0; patch = 0 }
                "Minor" -> { minor++; patch = 0 }
                else -> patch++
            }
            check(minor < 100 && patch < 100) { "MINEUR et CORRECTIF doivent rester < 100 (versionCode)." }
            file.writeText(
                """
                |# Version de l'application (Semantic Versioning : MAJEUR.MINEUR.CORRECTIF)
                |# Ne pas modifier à la main : utiliser ./gradlew bumpMajor | bumpMinor | bumpPatch
                |VERSION_MAJOR=$major
                |VERSION_MINOR=$minor
                |VERSION_PATCH=$patch
                |""".trimMargin()
            )
            println("Nouvelle version : $major.$minor.$patch")
        }
    }
}

tasks.register("printVersion") {
    group = "versioning"
    description = "Affiche versionName et versionCode."
    val name = appVersionName
    val code = appVersionCode
    doLast { println("versionName=$name versionCode=$code") }
}

// endregion
