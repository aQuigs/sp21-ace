plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.aquigs.sp21ace"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.aquigs.sp21ace"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        // As fast as a store build: Compose runs several times slower in a debuggable build, and R8 speeds it up further.
        // Signed with this machine's debug key rather than a release key kept somewhere, so it installs over a debug build
        // made here and keeps its practice history.
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

// Refreshes the shared copies described in CLAUDE.md when sync-common is on PATH. The configuration cache records isFile
// but not canExecute, so isFile is what makes installing or removing the command re-run this lookup.
val syncCommon = System.getenv("PATH").orEmpty().split(File.pathSeparator)
    .map { File(it, "sync-common") }
    .firstOrNull { it.isFile && it.canExecute() }
val syncShared = mapOf("syncSharedScripts" to "scripts", "syncSharedWorkflows" to ".github/workflows").map { (name, dir) ->
    tasks.register<Exec>(name) {
        enabled = syncCommon != null
        workingDir = rootDir
        commandLine(syncCommon?.path ?: "sync-common", dir)
    }
}
tasks.named("preBuild") { dependsOn(syncShared) }

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)

    // Declares the empty ComponentActivity that Compose UI tests render a single composable into
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    // ui-test-junit4 only brings Espresso 3.5.0 at runtime, which cannot inject input on API 34 and later
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
