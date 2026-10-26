package assets;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class TextureIdTest {

    @Test
    void everyTextureHasItsFile() {
        for (TextureId id : TextureId.values()) {
            String path = "res/" + id.file + ".png";
            assertNotNull(getClass().getClassLoader().getResource(path), id + ": no existe " + path);
        }
    }
}
