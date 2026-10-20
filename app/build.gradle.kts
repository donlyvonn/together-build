plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android { namespace = "app.together.preview"; compileSdk = 35
 defaultConfig { applicationId = "app.together.preview"; minSdk = 26; targetSdk = 35; versionCode = 3000; versionName = "0.30.0-preview" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 sourceSets.getByName("main").java.srcDir("../core/src/main/kotlin")
}

dependencies { testImplementation("junit:junit:4.13.2") }

android.sourceSets.getByName("test").java.srcDir("../core/src/test/kotlin")
