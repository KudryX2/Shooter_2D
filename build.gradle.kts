plugins {
    application
}

group = "game"
version = "1.1.0"

val gdxVersion = "1.14.2"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // libGDX con el backend de escritorio (LWJGL 3) y sus librerías nativas para Windows, Linux y macOS
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    runtimeOnly("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val isMac = System.getProperty("os.name").lowercase().contains("mac")

application {
    mainClass = "game.DesktopLauncher"
    // LWJGL carga librerías nativas; evita el aviso de "restricted method" de Java 24+.
    // En macOS, GLFW necesita que la ventana se cree en el primer hilo.
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED") +
            if (isMac) listOf("-XstartOnFirstThread") else emptyList()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-options"))
}

tasks.named<JavaExec>("run") {
    workingDir = projectDir
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get(),
            "Enable-Native-Access" to "ALL-UNNAMED",
        )
    }
}

tasks.test {
    useJUnitPlatform()
}
