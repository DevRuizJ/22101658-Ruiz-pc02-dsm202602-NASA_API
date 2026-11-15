import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}
val nasaApiKey = localProperties.getProperty("NASA_API_KEY")
    ?.takeIf { it.isNotBlank() }
    ?: throw GradleException("Add NASA_API_KEY to the ignored local.properties file.")
val escapedNasaApiKey = nasaApiKey
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "dev.ruizj.appnasa"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.ruizj.appnasa"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "NASA_API_KEY", "\"$escapedNasaApiKey\"")
        buildConfigField("String", "NEOWS_BASE_URL", "\"https://api.nasa.gov/\"")
        buildConfigField("String", "NEOWS_FEED_ENDPOINT", "\"neo/rest/v1/feed\"")
        buildConfigField("String", "NEOWS_LOOKUP_ENDPOINT", "\"neo/rest/v1/neo/\"")
        buildConfigField("String", "APOD_BASE_URL", "\"https://science.nasa.gov/\"")
        buildConfigField("String", "APOD_ENDPOINT", "\"wp-json/wp/v2/apod-basic\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
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
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}