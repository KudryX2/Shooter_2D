package game;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Localiza las librerías nativas de LWJGL 2 (DLLs) para no depender de {@code -Djava.library.path}. */
final class NativeLibraries {
    private static final String LWJGL_PATH_PROPERTY = "org.lwjgl.librarypath";
    private static final String JINPUT_PATH_PROPERTY = "net.java.games.input.librarypath";

    private NativeLibraries() {
    }

    static void configure() {
        if (System.getProperty(LWJGL_PATH_PROPERTY) != null) {
            return;
        }
        for (Path candidate : candidates()) {
            if (Files.isDirectory(candidate)) {
                String path = candidate.toAbsolutePath().normalize().toString();
                System.setProperty(LWJGL_PATH_PROPERTY, path);
                System.setProperty(JINPUT_PATH_PROPERTY, path);
                return;
            }
        }
        System.err.println("Aviso: no se encontró la carpeta de librerías nativas de LWJGL. "
                + "Usa -D" + LWJGL_PATH_PROPERTY + "=<ruta>");
    }

    private static List<Path> candidates() {
        List<Path> candidates = new ArrayList<>();
        try {
            // Distribución: <app>/lib/juego.jar -> <app>/natives
            Path codeSource = Path.of(Game.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path dir = Files.isDirectory(codeSource) ? codeSource : codeSource.getParent();
            candidates.add(dir.resolveSibling("natives"));
            candidates.add(dir.resolve("natives"));
        } catch (Exception ignored) {
            // Sin ubicación de código: solo probamos rutas relativas
        }
        // Ejecutando desde la raíz del proyecto
        candidates.add(Path.of("JavaLibs", "natives"));
        candidates.add(Path.of("natives"));
        return candidates;
    }
}
