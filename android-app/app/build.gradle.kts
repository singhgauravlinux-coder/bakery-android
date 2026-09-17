plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.crumbandember.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.crumbandember.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // Release signing comes from environment variables so CI can sign
    // uat/production builds with a real key (see .github/workflows/
    // _mobile-pipeline.yml) while a local `./gradlew assembleRelease` with
    // no env vars set silently falls back to the debug keystore — good
    // enough to smoke-test a release build locally, never good enough to
    // accidentally ship.
    val hasReleaseSigningEnv = System.getenv("ANDROID_KEYSTORE_PATH") != null
    signingConfigs {
        if (hasReleaseSigningEnv) {
            create("release") {
                storeFile = file(System.getenv("ANDROID_KEYSTORE_PATH")!!)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseSigningEnv) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    // ---------------------------------------------------------------------
    // Environment flavors — one per ArgoCD-managed overlay in k8s/overlays/.
    // Each flavor points at exactly the ingress host that environment's
    // bakery-ingress currently serves (see k8s/overlays/<env>/patch-ingress-
    // host.yaml), so "which backend am I hitting" never needs manual
    // configuration or a rebuild with hand-edited constants — you just pick
    // the flavor that matches the environment you're testing against.
    //
    // applicationIdSuffix lets dev/uat/production builds coexist as separate
    // apps on one device/emulator, exactly the way the backend's dev/uat/
    // production namespaces coexist in one cluster.
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            // Emulator loopback alias for the host machine — matches
            // `docker compose up` / a port-forwarded dev cluster on :3000.
            // Swap to "http://dev.bakery.local/api/" once dev.bakery.local
            // resolves for you (real cluster + DNS/hosts entry) instead of
            // a local compose stack.
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:3000/api/\"")
            buildConfigField("String", "ENVIRONMENT", "\"dev\"")
        }
        create("uat") {
            dimension = "environment"
            applicationIdSuffix = ".uat"
            versionNameSuffix = "-uat"
            buildConfigField("String", "API_BASE_URL", "\"http://uat.bakery.local/api/\"")
            buildConfigField("String", "ENVIRONMENT", "\"uat\"")
        }
        create("production") {
            dimension = "environment"
            // No suffix: this is the one that ships to real users / the store.
            buildConfigField("String", "API_BASE_URL", "\"https://bakery.local/api/\"")
            buildConfigField("String", "ENVIRONMENT", "\"production\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Async images (product photos from media-service)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Local token/session storage
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
