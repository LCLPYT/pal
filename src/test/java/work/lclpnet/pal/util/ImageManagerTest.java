package work.lclpnet.pal.util;

import com.github.romankh3.image.comparison.ImageComparison;
import com.github.romankh3.image.comparison.model.ImageComparisonResult;
import com.github.romankh3.image.comparison.model.ImageComparisonState;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class ImageManagerTest {

    private static Path debugDir = null;

    @Test
    void splitIntoTiles() throws IOException {
        var original = loadImage("images/img.png");
        var img_0_0 = loadImage("images/img_0_0.png");
        var img_1_0 = loadImage("images/img_1_0.png");
        var img_0_1 = loadImage("images/img_0_1.png");
        var img_1_1 = loadImage("images/img_1_1.png");

        var tiles = ImageManager.splitIntoTiles(original, 128);
        assertEquals(4, tiles.length);

        assertImageEquals(img_0_0, tiles[0]);
        assertImageEquals(img_1_0, tiles[1]);
        assertImageEquals(img_0_1, tiles[2]);
        assertImageEquals(img_1_1, tiles[3]);
    }

    @Test
    void cropCenteredBigger() throws IOException {
        var original = loadImage("images/img.png");
        var bigger = ImageManager.cropCentered(original, 296, 296);
        var expected = loadImage("images/2x_img.png");

        assertImageEquals(expected, bigger);
    }

    @Test
    void cropCenteredSmaller() throws IOException {
        var original = loadImage("images/img.png");
        var bigger = ImageManager.cropCentered(original, 74, 74);
        var expected = loadImage("images/0_5x_img.png");

        assertImageEquals(expected, bigger);
    }

    @Test
    void cropCenteredBiggerX() throws IOException {
        var original = loadImage("images/img.png");
        var bigger = ImageManager.cropCentered(original, 296, 74);
        var expected = loadImage("images/2x_x_img.png");

        assertImageEquals(expected, bigger);
    }

    @Test
    void cropCenteredBiggerY() throws IOException {
        var original = loadImage("images/img.png");
        var bigger = ImageManager.cropCentered(original, 74, 296);
        var expected = loadImage("images/2x_y_img.png");

        assertImageEquals(expected, bigger);
    }

    @Test
    void scaleToMatchBigger() throws IOException {
        var original = loadImage("images/img.png");
        var bigger = ImageManager.scaleToMatch(original, 296, 296);
        var expected = loadImage("images/2x_scale_bicubic_img.png");

        assertImageEquals(expected, bigger);
    }

    @Test
    void scaleToMatchSmaller() throws IOException {
        var original = loadImage("images/img.png");
        var bigger = ImageManager.scaleToMatch(original, 74, 74);
        var expected = loadImage("images/0_5x_scale_bicubic_img.png");

        assertImageEquals(expected, bigger);
    }

    @Test
    void scaleToMatchSmallerX() throws IOException {
        var original = loadImage("images/img.png");
        var bigger = ImageManager.scaleToMatch(original, 74, 296);
        var expected = loadImage("images/0_5x_scale_bicubic_img.png");

        assertImageEquals(expected, bigger);
    }

    @Test
    void scaleToMatchSmallerY() throws IOException {
        var original = loadImage("images/img.png");
        var bigger = ImageManager.scaleToMatch(original, 296, 74);
        var expected = loadImage("images/0_5x_scale_bicubic_img.png");

        assertImageEquals(expected, bigger);
    }

    @Test
    void scaleToMatchPortrait() throws IOException {
        var original = loadImage("images/banner.jpg");
        var bigger = ImageManager.scaleToMatch(original, 128, 128);
        var expected = loadImage("images/banner_scaled.png");

        assertImageEquals(expected, bigger);
    }

    @Test
    void scaleToMatchLandscape() throws IOException {
        var original = loadImage("images/landscape.jpg");
        var bigger = ImageManager.scaleToMatch(original, 128, 128);
        var expected = loadImage("images/landscape_scaled.png");

        assertImageEquals(expected, bigger);
    }

    @Test
    void scaleToMatchToLandscape() throws IOException {
        var original = loadImage("images/img_128px.png");
        var bigger = ImageManager.scaleToMatch(original, 296, 128);

        assertImageEquals(original, bigger);
    }

    private static void assertImageEquals(BufferedImage expected, BufferedImage actual) throws IOException {
        ImageComparisonResult result = new ImageComparison(expected, actual).compareImages();

        if (result.getImageComparisonState() == ImageComparisonState.MATCH) return;

        Path dir = getDebugDirectory();
        String id = UUID.randomUUID().toString();

        Path exportPath = dir.resolve(id + "_diff.png");

        saveImage(expected, dir.resolve(id + "_expected.png"));
        saveImage(actual, dir.resolve(id + "_actual.png"));
        saveImage(result.getResult(), exportPath);

        fail("Images are not equal. Wrote diff to " + exportPath.toAbsolutePath());
    }

    private static BufferedImage loadImage(String src) throws IOException {
        try (var in = ImageManagerTest.class.getClassLoader().getResourceAsStream(src)) {
            return ImageIO.read(Objects.requireNonNull(in));
        }
    }

    private static Path getDebugDirectory() throws IOException {
        if (debugDir == null) {
            debugDir = Files.createTempDirectory("paltest");
        }

        return debugDir;
    }

    private static void saveImage(BufferedImage image, Path path) throws IOException {
        try (var out = Files.newOutputStream(path)) {
            ImageIO.write(image, "png", out);
        }
    }
}