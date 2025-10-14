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
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import work.lclpnet.kibu.world.mixin.MinecraftServerAccessor;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

public class PersistentWorldSuggestionProvider implements SuggestionProvider<ServerCommandSource> {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        MinecraftServer server = context.getSource().getServer();
        LevelStorage.Session session = ((MinecraftServerAccessor) server).getSession();

        Path dimDirectory = session.getDirectory(WorldSavePath.ROOT).resolve("dimensions");

        return CompletableFuture.supplyAsync(() -> {
            try {
                Files.walkFileTree(dimDirectory, EnumSet.noneOf(FileVisitOption.class), 16, new SimpleFileVisitor<>() {

                    @Override
                    public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) {
                        if (!Files.isRegularFile(dir.resolve("level.dat"))) {
                            return FileVisitResult.CONTINUE;
                        }

                        Path rel = dimDirectory.relativize(dir);

                        if (rel.getNameCount() < 2) {
                            return FileVisitResult.CONTINUE;
                        }

                        var it = rel.iterator();
                        String namespace = it.next().toString();
                        StringBuilder pathBuilder = new StringBuilder();

                        while (it.hasNext()) {
                            if (!pathBuilder.isEmpty()) pathBuilder.append('/');

                            pathBuilder.append(it.next());
                        }

                        Identifier id = Identifier.of(namespace, pathBuilder.toString());
                        builder.suggest(id.toString());

                        return FileVisitResult.SKIP_SUBTREE;
                    }
                });
            } catch (IOException ignored) {}

            return builder.build();
        });
    }
}
