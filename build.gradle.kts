import java.util.Locale

val useFFM = true
val useWGPU = true

val osName = System.getProperty("os.name").lowercase(Locale.ROOT)
val osArch = System.getProperty("os.arch").lowercase(Locale.ROOT)

val lwjglVersion = "3.4.2"
val lwjgl3awtVersion = "0.2.3"
val jomlVersion = "1.10.9"
val jomlPrimitivesVersion = "1.10.0"

val lwjglNatives = when {
    osName.contains("windows") -> {
        when {
            osArch.contains("aarch64") || osArch.contains("arm64") ->
                "natives-windows-arm64"
            else ->
                "natives-windows"
        }
    }

    osName.contains("mac") || osName.contains("darwin") -> {
        when {
            osArch.contains("aarch64") || osArch.contains("arm64") ->
                "natives-macos-arm64"
            else ->
                "natives-macos"
        }
    }

    osName.contains("linux") || osName.contains("unix") -> {
        when {
            osArch.contains("aarch64") || osArch.contains("arm64") ->
                "natives-linux-arm64"
            osArch.startsWith("arm") ->
                "natives-linux-arm32"
            osArch.startsWith("ppc64le") ->
                "natives-linux-ppc64le"
            osArch.startsWith("riscv64") ->
                "natives-linux-riscv64"
            else ->
                "natives-linux"
        }
    }

    else -> {
        error(
            "Unsupported platform: os.name=$osName, os.arch=$osArch"
        )
    }
}

println("LWJGL natives: $lwjglNatives")

plugins {
    id("java")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}

group = "choccymalk"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // -------------------------------------------------------------------------
    // JUnit
    // -------------------------------------------------------------------------

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // -------------------------------------------------------------------------
    // jWebGPU
    // -------------------------------------------------------------------------

    /*implementation("com.github.xpenatan.jWebGPU:webgpu-core:0.3.4")

    if (useFFM) {
        implementation(
            "com.github.xpenatan.jWebGPU:webgpu-desktop-ffm:0.3.4"
        )
    } else {
        implementation(
            "com.github.xpenatan.jWebGPU:webgpu-desktop-jni:0.3.4"
        )
    }

    when {
        osName.contains("windows") -> {
            if (useFFM) {
                if (useWGPU) {
                    println("Using WGPU with FFM on Windows")
                    runtimeOnly(
                        "com.github.xpenatan.jWebGPU:webgpu-desktop-ffm-wgpu_windows_x64:0.3.4"
                    )
                } else {
                    println("Using Dawn with FFM on Windows")
                    runtimeOnly(
                        "com.github.xpenatan.jWebGPU:webgpu-desktop-ffm-dawn_windows_x64:0.3.4"
                    )
                }
            } else {
                if (useWGPU) {
                    println("Using WGPU with JNI on Windows")
                    runtimeOnly(
                        "com.github.xpenatan.jWebGPU:webgpu-desktop-jni-wgpu_windows_x64:0.3.4"
                    )
                } else {
                    println("Using Dawn with JNI on Windows")
                    runtimeOnly(
                        "com.github.xpenatan.jWebGPU:webgpu-desktop-jni-dawn_windows_x64:0.3.4"
                    )
                }
            }
        }

        osName.contains("mac") -> {
            if (useFFM) {
                if (useWGPU) {
                    println("Using WGPU with FFM on macOS")
                    runtimeOnly(
                        "com.github.xpenatan.jWebGPU:webgpu-desktop-ffm-wgpu_mac_arm64:0.3.4"
                    )
                } else {
                    println("Using Dawn with FFM on macOS")
                    runtimeOnly(
                        "com.github.xpenatan.jWebGPU:webgpu-desktop-ffm-dawn_mac_arm64:0.3.4"
                    )
                }
            } else {
                if (useWGPU) {
                    println("Using WGPU with JNI on macOS")
                    runtimeOnly(
                        "com.github.xpenatan.jWebGPU:webgpu-desktop-jni-wgpu_mac_arm64:0.3.4"
                    )
                } else {
                    println("Using Dawn with JNI on macOS")
                    runtimeOnly(
                        "com.github.xpenatan.jWebGPU:webgpu-desktop-jni-dawn_mac_arm64:0.3.4"
                    )
                }
            }
        }

        else -> {
            println("$osName isn't supported by the jWebGPU native configuration")
        }
    }*/

    // -------------------------------------------------------------------------
    // LWJGL
    // -------------------------------------------------------------------------

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    //implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl", "lwjgl")

    implementation("org.lwjgl:lwjgl-assimp")
    implementation("org.lwjgl:lwjgl-bgfx")
    implementation("org.lwjgl:lwjgl-egl")
    implementation("org.lwjgl:lwjgl-fmod")
    implementation("org.lwjgl:lwjgl-freetype")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-harfbuzz")
    implementation("org.lwjgl:lwjgl-hwloc")
    //implementation("org.lwjgl:lwjgl-jawt")
    implementation("org.lwjgl:lwjgl-jemalloc")
    implementation("org.lwjgl:lwjgl-ktx")
    implementation("org.lwjgl:lwjgl-llvm")
    implementation("org.lwjgl:lwjgl-lmdb")
    implementation("org.lwjgl:lwjgl-lz4")
    implementation("org.lwjgl:lwjgl-meshoptimizer")
    implementation("org.lwjgl:lwjgl-mimalloc")
    implementation("org.lwjgl:lwjgl-msdfgen")
    implementation("org.lwjgl:lwjgl-nanovg")
    implementation("org.lwjgl:lwjgl-nfd")
    implementation("org.lwjgl:lwjgl-nuklear")
    implementation("org.lwjgl:lwjgl-odbc")
    implementation("org.lwjgl:lwjgl-openal")
    implementation("org.lwjgl:lwjgl-opencl")
    implementation("org.lwjgl:lwjgl-opengl")
    implementation("org.lwjgl:lwjgl-opengles")
    implementation("org.lwjgl:lwjgl-openxr")
    implementation("org.lwjgl:lwjgl-opus")
    implementation("org.lwjgl:lwjgl-par")
    implementation("org.lwjgl:lwjgl-remotery")
    implementation("org.lwjgl:lwjgl-renderdoc")
    implementation("org.lwjgl:lwjgl-rpmalloc")
    implementation("org.lwjgl:lwjgl-sdl")
    implementation("org.lwjgl:lwjgl-shaderc")
    implementation("org.lwjgl:lwjgl-spng")
    implementation("org.lwjgl:lwjgl-spvc")
    implementation("org.lwjgl:lwjgl-stb")
    implementation("org.lwjgl:lwjgl-tinyexr")
    implementation("org.lwjgl:lwjgl-tinyfd")
    implementation("org.lwjgl:lwjgl-vma")
    implementation("org.lwjgl:lwjgl-vulkan")
    implementation("org.lwjgl:lwjgl-xxhash")
    implementation("org.lwjgl:lwjgl-yoga")
    implementation("org.lwjgl:lwjgl-zstd")

    // -------------------------------------------------------------------------
    // LWJGL natives
    //
    // IMPORTANT:
    // Maven classifier syntax is:
    //
    //     group:artifact:version:classifier
    //
    // NOT:
    //
    //     group:artifact::classifier
    // -------------------------------------------------------------------------
    println("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNatives")
    //runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNatives")

    runtimeOnly("org.lwjgl", "lwjgl", classifier = "natives-windows")
    runtimeOnly("org.lwjgl", "lwjgl-opengl", classifier = "natives-windows")

    runtimeOnly("org.lwjgl:lwjgl-assimp:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-bgfx:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-freetype:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-harfbuzz:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-hwloc:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-jemalloc:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-ktx:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-llvm:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-lmdb:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-lz4:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-meshoptimizer:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-mimalloc:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-msdfgen:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-nanovg:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-nfd:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-nuklear:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-openal:$lwjglVersion:$lwjglNatives")
    //runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:$lwjglNatives")
    //runtimeOnly("org.lwjgl:lwjgl-opengles:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-openxr:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opus:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-par:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-remotery:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-rpmalloc:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-sdl:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-shaderc:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-spng:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-spvc:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-tinyexr:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-tinyfd:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-vma:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-xxhash:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-yoga:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-zstd:$lwjglVersion:$lwjglNatives")

    // Vulkan is not needed separately on macOS in most LWJGL setups.
    // Keep this only if your application actually uses the LWJGL Vulkan binding.
    if (lwjglNatives == "natives-macos-arm64") {
        runtimeOnly("org.lwjgl:lwjgl-vulkan:$lwjglVersion:$lwjglNatives")
    }

    // -------------------------------------------------------------------------
    // Other libraries
    // -------------------------------------------------------------------------

    //implementation("org.lwjglx:lwjgl3-awt:$lwjgl3awtVersion")
    implementation("org.joml:joml:$jomlVersion")
    implementation("org.joml:joml-primitives:$jomlPrimitivesVersion")
}

tasks.test {
    useJUnitPlatform()
}