plugins {
    application
}

group = "game"
version = "1.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

val nativesDir = layout.projectDirectory.dir("JavaLibs/natives")

dependencies {
    // LWJGL 2.9.1 + slick-util, incluidos en el repositorio
    implementation(files("JavaLibs/jars/lwjgl.jar", "JavaLibs/jars/slick-util.jar"))

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "game.Game"
    // LWJGL 2 carga librerías nativas por JNI; evita el aviso de "restricted method" de Java 24+
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-options"))
}

tasks.named<JavaExec>("run") {
    workingDir = projectDir
    systemProperty("org.lwjgl.librarypath", nativesDir.asFile.absolutePath)
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get(),
            "Enable-Native-Access" to "ALL-UNNAMED",
        )
    }
}

// La distribución (installDist / distZip) incluye las DLLs en <app>/natives
distributions {
    main {
        contents {
            from(nativesDir) { into("natives") }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
