plugins {
    `kotlin-dsl`
}

dependencies {
    // AGP on the buildSrc classpath is what the whole build uses: because buildSrc's
    // classloader is the parent of every build script's, the main build must apply
    // com.android.application WITHOUT a version (see the root and app plugins blocks).
    // The version still comes from the shared catalog's `agp` entry.
    implementation("com.android.tools.build:gradle:${libs.versions.agp.get()}")
}
