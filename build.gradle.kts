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

/** Solo para empaquetar texturas al compilar (no va en el juego). */
val texturePacker: Configuration = configurations.create("texturePacker")

dependencies {
    texturePacker("com.badlogicgames.gdx:gdx-tools:$gdxVersion")

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

// Atlas de texturas: junta todas las imágenes de assets-raw/textures en atlas/game.atlas + game.png (ajustes en
// su pack.json). Para animar una textura basta con poner sus frames como nombre_0.png, nombre_1.png...
val texturesDir = layout.projectDirectory.dir("assets-raw/textures")
val generatedResources = layout.buildDirectory.dir("generated/atlas")
val packTextures = tasks.register<JavaExec>("packTextures") {
    group = "build"
    description = "Empaqueta las texturas de assets-raw/textures en un atlas."
    classpath = texturePacker
    mainClass = "com.badlogic.gdx.tools.texturepacker.TexturePacker"
    jvmArgs("-Djava.awt.headless=true")
    val outputDir = generatedResources.map { it.dir("atlas") }
    inputs.dir(texturesDir)
    outputs.dir(outputDir)
    argumentProviders.add(CommandLineArgumentProvider {
        listOf(texturesDir.asFile.path, outputDir.get().asFile.path, "game")
    })
    doFirst { outputDir.get().asFile.deleteRecursively() } // que no queden páginas viejas
}

sourceSets.main {
    resources.srcDir(files(generatedResources).builtBy(packTextures))
}

// Herramientas de desarrollo (no van en el juego). generateArt dibuja los gráficos provisionales en pixel art de
// suelos y estructuras en assets-raw/textures/pixel; solo hay que lanzarla a mano si se cambia el generador.
val tools: SourceSet = sourceSets.create("tools")
tasks.register<JavaExec>("generateArt") {
    group = "build"
    description = "Genera los gráficos provisionales de suelos y estructuras."
    classpath = tools.runtimeClasspath
    mainClass = "tools.PixelArtGenerator"
    jvmArgs("-Djava.awt.headless=true")
    args(texturesDir.dir("pixel").asFile.path)
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
    jvmArgs("--enable-native-access=ALL-UNNAMED") // los tests de cámara cargan código nativo de libGDX
    useJUnitPlatform()
}
