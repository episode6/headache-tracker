import com.android.build.api.artifact.SingleArtifact
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
    alias(libs.plugins.metro)
}

// derived from self.versions.name in the root build script (see the formula there)
val selfVersionCode: Int by rootProject.extra
val selfIsSnapshot: Boolean by rootProject.extra
val selfAppName: String by rootProject.extra
val selfAppId: String by rootProject.extra

android {
    namespace = "com.episode6.headachetracker"
    compileSdk = 35

    buildFeatures {
        // for the snapshot-aware app_name resValue in defaultConfig
        resValues = true
    }

    defaultConfig {
        // snapshot builds (everything except CI release-tag builds) get their own
        // applicationId and launcher label so they can be installed side-by-side with
        // the released app instead of overwriting it (the namespace above stays fixed,
        // so R + manifest class refs are unaffected)
        applicationId = selfAppId
        resValue("string", "app_name", selfAppName)
        // "Check for updates" just opens a browser page — this app must never request
        // the INTERNET permission, so there is no in-app update check. Snapshots point
        // at the main-branch commit log, releases at the latest GitHub release.
        resValue(
            "string", "check_for_updates_url",
            if (selfIsSnapshot) "https://github.com/episode6/headache-tracker/commits/main/"
            else "https://github.com/episode6/headache-tracker/releases/latest",
        )
        // snapshot builds keep the calendar foreground but swap the purple background
        // for dark charcoal, so the two installs are distinguishable at a glance;
        // placeholders resolve at manifest merge, so lint + resource shrinking still
        // see the concrete @mipmap reference per build
        manifestPlaceholders["appIcon"] =
            if (selfIsSnapshot) "@mipmap/ic_launcher_snapshot" else "@mipmap/ic_launcher"
        manifestPlaceholders["appIconRound"] =
            if (selfIsSnapshot) "@mipmap/ic_launcher_round_snapshot" else "@mipmap/ic_launcher_round"
        minSdk = 26
        targetSdk = 35
        versionCode = selfVersionCode
        versionName = self.versions.name.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            // the debug keystore is committed to the repo (standard debug credentials,
            // not a secret) so CI-built and local debug APKs share a signature and can
            // overwrite each other on-device instead of failing with a signature mismatch
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            // CI decodes the ANDROID_KEYSTORE secret to a file and exports these
            // env vars; without them (local builds, PR CI) release stays unsigned
            val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_ROOT_PASSWORD")
                keyAlias = "episode6"
                keyPassword = System.getenv("ANDROID_KEYSTORE_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        debug {
            // debug installs get their own id so they don't clobber (or get blocked by
            // a signature mismatch with) an installed CI-built snapshot APK
            applicationIdSuffix = ".debug"
        }
        release {
            // R8 runs in full mode by default on AGP 8+; keep rules live in proguard-rules.pro
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// LicenseNotices.kt embeds THIRD_PARTY_LICENSES.md so the in-app licenses screen always
// shows the same document the repo ships
abstract class GenerateLicenseNoticesTask : DefaultTask() {
    @get:InputFile
    abstract val noticesFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val escaped = noticesFile.get().asFile.readText()
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("$", "\\$")
            .replace("\r", "")
            .replace("\n", "\\n")
        val outFile = outDir.get().file("com/episode6/headachetracker/LicenseNotices.kt").asFile
        outFile.parentFile.mkdirs()
        outFile.writeText(
            """
            |package com.episode6.headachetracker
            |
            |/** Generated from THIRD_PARTY_LICENSES.md at build time; do not edit. */
            |object LicenseNotices {
            |    const val MARKDOWN: String = "$escaped"
            |}
            |""".trimMargin()
        )
    }
}

val generateLicenseNotices = tasks.register<GenerateLicenseNoticesTask>("generateLicenseNotices") {
    noticesFile.set(rootProject.layout.projectDirectory.file("THIRD_PARTY_LICENSES.md"))
}

// The release APK's transitive dependency set is pinned to expected-dependencies.txt
// (group:artifact, versions live in the catalog) so a new library — direct or
// transitive — can't ship without a deliberate review: every artifact needs a
// THIRD_PARTY_LICENSES.md entry, and none may pull network code into an app that
// must stay fully offline.
abstract class VerifyAppDependenciesTask : DefaultTask() {
    @get:Input
    abstract val actualDependencies: SetProperty<String>

    @get:InputFile
    abstract val expectedFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val expected = expectedFile.get().asFile.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .toSortedSet()
        val actual = actualDependencies.get().toSortedSet()
        if (expected == actual) return
        throw GradleException(buildString {
            appendLine("Release dependencies don't match ${expectedFile.get().asFile.name}.")
            (actual - expected).forEach { appendLine("  unexpected: $it") }
            (expected - actual).forEach { appendLine("  missing:    $it") }
            appendLine("If this change is intentional, run ./gradlew :app:writeExpectedDependencies")
            appendLine("and update THIRD_PARTY_LICENSES.md to match.")
        })
    }
}

abstract class WriteExpectedAppDependenciesTask : DefaultTask() {
    @get:Input
    abstract val actualDependencies: SetProperty<String>

    @get:OutputFile
    abstract val expectedFile: RegularFileProperty

    @TaskAction
    fun write() {
        expectedFile.get().asFile.writeText(
            "# Transitive runtime dependencies of the release APK (group:artifact).\n" +
                "# Verified by :app:verifyReleaseDependencies (runs with `check`); regenerate\n" +
                "# with :app:writeExpectedDependencies and keep THIRD_PARTY_LICENSES.md in sync.\n" +
                actualDependencies.get().toSortedSet().joinToString("\n", postfix = "\n")
        )
    }
}

// Guards the no-network product spec at the artifact level: the merged release
// manifest must declare exactly these permissions, so neither our own manifest nor
// a library's can (re)introduce INTERNET.
abstract class VerifyAppPermissionsTask : DefaultTask() {
    @get:InputFile
    abstract val mergedManifest: RegularFileProperty

    @get:Input
    abstract val expectedPermissions: SetProperty<String>

    @TaskAction
    fun verify() {
        val actual = Regex("<uses-permission[^>]*android:name=\"([^\"]+)\"")
            .findAll(mergedManifest.get().asFile.readText())
            .map { it.groupValues[1] }
            // androidx-core auto-defines this app-private signature permission (named
            // after the applicationId) to sandbox unexported dynamic receivers; it
            // grants no capability, so it's exempt from the allowlist
            .filterNot { it.endsWith(".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION") }
            .toSortedSet()
        val expected = expectedPermissions.get().toSortedSet()
        if (expected == actual) return
        throw GradleException(buildString {
            appendLine("Merged release manifest permissions don't match the expected set.")
            (actual - expected).forEach { appendLine("  unexpected: $it") }
            (expected - actual).forEach { appendLine("  missing:    $it") }
            appendLine("This app must stay fully offline (never add INTERNET — see AGENTS.md);")
            appendLine("deliberate permission changes update the list in app/build.gradle.kts.")
        })
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.kotlin?.addGeneratedSourceDirectory(
            generateLicenseNotices,
            GenerateLicenseNoticesTask::outDir,
        )

        if (variant.name == "release") {
            // resolution happens lazily at execution time via the rootComponent provider
            val dependencies = variant.runtimeConfiguration.incoming.resolutionResult.rootComponent
                .map { root ->
                    val seen = LinkedHashSet<ResolvedComponentResult>()
                    val queue = ArrayDeque(listOf(root))
                    while (queue.isNotEmpty()) {
                        val component = queue.removeFirst()
                        if (seen.add(component)) {
                            queue.addAll(
                                component.dependencies
                                    .filterIsInstance<ResolvedDependencyResult>()
                                    .map { it.selected }
                            )
                        }
                    }
                    seen.mapNotNull { component ->
                        (component.id as? ModuleComponentIdentifier)?.let { "${it.group}:${it.module}" }
                    }.toSortedSet()
                }
            val expectedDependenciesFile = layout.projectDirectory.file("expected-dependencies.txt")

            val verifyDependencies =
                tasks.register<VerifyAppDependenciesTask>("verifyReleaseDependencies") {
                    actualDependencies.set(dependencies)
                    expectedFile.set(expectedDependenciesFile)
                }
            tasks.register<WriteExpectedAppDependenciesTask>("writeExpectedDependencies") {
                actualDependencies.set(dependencies)
                expectedFile.set(expectedDependenciesFile)
            }
            val verifyPermissions =
                tasks.register<VerifyAppPermissionsTask>("verifyReleasePermissions") {
                    mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
                    expectedPermissions.set(
                        setOf(
                            "android.permission.POST_NOTIFICATIONS",
                            "android.permission.RECEIVE_BOOT_COMPLETED",
                            "android.permission.SCHEDULE_EXACT_ALARM",
                            "android.permission.USE_EXACT_ALARM",
                        )
                    )
                }
            tasks.named("check") { dependsOn(verifyDependencies, verifyPermissions) }
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.adaptive)
    implementation(libs.androidx.compose.adaptive.layout)
    implementation(libs.androidx.compose.adaptive.navigation)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.material)
    implementation(libs.metro.runtime)
    implementation(libs.metrox.viewmodel)
    implementation(libs.metrox.viewmodel.compose)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.test.rules)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    "ksp"(libs.androidx.room.compiler)
}