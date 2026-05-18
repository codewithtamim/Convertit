plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    val packageName = "com.nasahacker.convertit"
    namespace = packageName
    compileSdk = 36

    defaultConfig {
        applicationId = packageName
        minSdk = 24
        targetSdk = 36
        versionCode = 47
        versionName = "1.3.14-lts"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")
        }
    }

}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
    implementation("org.thebytearray.lib:taglib:1.0.3")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
}

// Gradle 9 + AGP 9: compile*JavaWithJavac validates annotationProcessors.json; ensure javaPreCompile runs first
// (avoids "annotationProcessors.json doesn't exist" after partial cleans or task ordering glitches).
afterEvaluate {
    tasks.withType(org.gradle.api.tasks.compile.JavaCompile::class.java).configureEach {
        val n = name
        if (!n.startsWith("compile") || !n.endsWith("JavaWithJavac")) return@configureEach
        val variant = n.removePrefix("compile").removeSuffix("JavaWithJavac")
        val pre = tasks.findByName("javaPreCompile$variant")
        if (pre != null) {
            dependsOn(pre)
        }
    }
}
