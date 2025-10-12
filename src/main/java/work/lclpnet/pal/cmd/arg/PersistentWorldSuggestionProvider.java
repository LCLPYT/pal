package work.lclpnet.pal.cmd.arg;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage;
import work.lclpnet.kibu.world.mixin.MinecraftServerAccessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

public class PersistentWorldSuggestionProvider implements SuggestionProvider<ServerCommandSource> {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        MinecraftServer server = context.getSource().getServer();
        LevelStorage.Session session = ((MinecraftServerAccessor) server).getSession();

        Path dimDirectory = session.getDirectory(WorldSavePath.ROOT).resolve("dimensions");

        return CompletableFuture.supplyAsync(() -> {
            try (var files = Files.find(dimDirectory, 16, (path, attr)
                    -> dimDirectory.relativize(path).getNameCount() >= 2)) {

                files.filter(path -> Files.isRegularFile(path.resolve("level.dat")))
                        .map(path -> {
                            Path rel = dimDirectory.relativize(path);

                            var it = rel.iterator();
                            String namespace = it.next().toString();
                            StringBuilder pathBuilder = new StringBuilder();

                            while (it.hasNext()) {
                                if (!pathBuilder.isEmpty()) {
                                    pathBuilder.append('/');
                                }

                                pathBuilder.append(it.next());
                            }

                            Identifier id = Identifier.of(namespace, pathBuilder.toString());

                            return id.toString();
                        })
                        .forEach(builder::suggest);

            } catch (IOException ignored) {}

            return builder.build();
        });
    }
}
