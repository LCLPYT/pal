package work.lclpnet.pal.cmd.arg;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class ImageSuggestionProvider implements SuggestionProvider<ServerCommandSource> {

    private final Path path;
    private final Logger logger;

    @Inject
    public ImageSuggestionProvider(@Named("imagesPath") Path path, Logger logger) {
        this.path = path;
        this.logger = logger;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        return CompletableFuture.supplyAsync(() -> {
            if (!Files.isDirectory(path)) {
                return builder.build();
            }

            try (var files = Files.walk(path, 32)) {
                files.filter(Files::isRegularFile)
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .forEach(builder::suggest);
            } catch (IOException e) {
                logger.error("Failed to list image directory", e);
            }

            return builder.build();
        });
    }
}
