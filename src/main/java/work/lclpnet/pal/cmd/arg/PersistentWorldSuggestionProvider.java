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
import java.util.concurrent.CompletableFuture;

public class PersistentWorldSuggestionProvider implements SuggestionProvider<ServerCommandSource> {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        MinecraftServer server = context.getSource().getServer();
        LevelStorage.Session session = ((MinecraftServerAccessor) server).getSession();

        Path dimDirectory = session.getDirectory(WorldSavePath.ROOT)
                .resolve("dimensions");

        try (var files = Files.find(dimDirectory, 2,
                (path, attr) -> dimDirectory.relativize(path).getNameCount() == 2)) {

            files.filter(path -> Files.isRegularFile(path.resolve("level.dat")))
                    .map(path -> {
                        Path rel = dimDirectory.relativize(path);
                        String namespace = rel.getParent().getFileName().toString();
                        String name = rel.getFileName().toString();

                        Identifier id = new Identifier(namespace, name);

                        return id.toString();
                    }).forEach(builder::suggest);

        } catch (IOException ignored) {}

        return builder.buildFuture();
    }
}
