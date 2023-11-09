package work.lclpnet.pal.util;

import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Named;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class ImageManager {

    private final Path directory;

    @Inject
    public ImageManager(@Named("imagesPath") Path directory) {
        this.directory = directory;
    }

    public CompletableFuture<ImageSource> getSource(String name) {
        try {
            URL url = new URL(name);

            return CompletableFuture.completedFuture(url::openStream);
        } catch (MalformedURLException ignored) {}

        return CompletableFuture.supplyAsync(() -> {
            Path path = directory.resolve(name);

            if (!Files.isRegularFile(path)) {
                throw new RuntimeException(new FileNotFoundException(path.toString()));
            }

            return () -> Files.newInputStream(path);
        });
    }

    public CompletableFuture<BufferedImage> loadImage(ImageSource source) {
        return CompletableFuture.supplyAsync(() -> {
            try (var in = source.openStream()) {
                return ImageIO.read(in);
            } catch (IOException e) {
                throw new RuntimeException("Failed to open image", e);
            }
        });
    }

    public static BufferedImage[] getParts(BufferedImage image, MapSizes sizes, ImageMode mode) {
        int countX = Math.max(1, sizes.width());
        int countY = Math.max(1, sizes.height());

        final int tileSize = 128;

        int width = tileSize * countX;
        int height = tileSize * countY;

        if (mode == ImageMode.MATCH) {
            image = scaleToMatch(image, width, height);
        }

        image = cropCentered(image, width, height);

        return splitIntoTiles(image, tileSize);
    }

    @NotNull
    public static BufferedImage scaleToMatch(BufferedImage image, int width, int height) {
        int oldWidth = image.getWidth();
        int oldHeight = image.getHeight();

        double scaleX = (double) width / oldWidth;
        double scaleY = (double) height / oldHeight;

        double scale = Math.min(scaleX, scaleY);

        return scaleImage(image, (int) (oldWidth * scale), (int) (oldHeight * scale), scale, scale);
    }

    @NotNull
    private static BufferedImage scaleImage(BufferedImage image, int width, int height, double scaleX, double scaleY) {
        BufferedImage scaled = new BufferedImage(width, height, image.getType());

        AffineTransform transform = new AffineTransform();
        transform.scale(scaleX, scaleY);
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC);

        scaled = op.filter(image, scaled);

        return scaled;
    }

    @NotNull
    public static BufferedImage cropCentered(BufferedImage image, int width, int height) {
        int oldWidth = image.getWidth();
        int oldHeight = image.getHeight();

        BufferedImage cropped = new BufferedImage(width, height, image.getType());

        int dx = (width - oldWidth) / 2, dy = (height - oldHeight) / 2;

        Graphics2D graphics = cropped.createGraphics();
        graphics.drawImage(image, dx, dy, null);
        graphics.dispose();

        return cropped;
    }

    @NotNull
    public static BufferedImage[] splitIntoTiles(BufferedImage image, final int tileSize) {
        int countX = (int) Math.ceil(image.getWidth() / (float) tileSize);
        int countY = (int) Math.ceil(image.getHeight() / (float) tileSize);

        var images = new BufferedImage[countX * countY];

        for (int y = 0; y < countY; y++) {
            for (int x = 0; x < countX; x++) {
                int offsetX = x * tileSize;
                int offsetY = y * tileSize;

                int w = Math.min(tileSize, image.getWidth() - offsetX);
                int h = Math.min(tileSize, image.getHeight() - offsetY);

                BufferedImage sub = image.getSubimage(offsetX, offsetY, w, h);

                BufferedImage tmp = new BufferedImage(tileSize, tileSize, image.getType());
                Graphics2D graphics = tmp.createGraphics();
                graphics.drawImage(sub, 0, 0, null);
                graphics.dispose();

                images[y * countX + x] = tmp;
            }
        }

        return images;
    }
}
