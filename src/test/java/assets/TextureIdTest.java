package assets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData;

class TextureIdTest {

    /** Lee el atlas generado por {@code packTextures} (solo el texto: no hace falta ventana ni OpenGL). */
    @Test
    void everyTextureIsInTheAtlas() throws Exception {
        URL url = getClass().getClassLoader().getResource(Assets.ATLAS);
        assertNotNull(url, "No se ha generado el atlas " + Assets.ATLAS);
        FileHandle atlasFile = new FileHandle(new File(url.toURI()));
        TextureAtlasData data = new TextureAtlasData(atlasFile, atlasFile.parent(), true);

        Set<String> regions = new HashSet<>();
        data.getRegions().forEach(region -> regions.add(region.name));
        for (TextureId id : TextureId.values()) {
            assertTrue(regions.contains(id.file), id + ": falta assets-raw/textures/" + id.file + ".png");
        }
    }
}
