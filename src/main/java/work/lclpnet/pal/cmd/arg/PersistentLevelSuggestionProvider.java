package work.lclpnet.pal.cmd.arg;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jetbrains.annotations.NotNull;
import work.lclpnet.kibu.world.mixin.MinecraftServerAccessor;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

public class PersistentLevelSuggestionProvider implements SuggestionProvider<CommandSourceStack> {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        MinecraftServer server = context.getSource().getServer();
        LevelStorageSource.LevelStorageAccess session = ((MinecraftServerAccessor) server).getStorageSource();

        Path dimDirectory = session.getLevelPath(LevelResource.ROOT).resolve("dimensions");

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

                        Identifier id = Identifier.fromNamespaceAndPath(namespace, pathBuilder.toString());
                        builder.suggest(id.toString());

                        return FileVisitResult.SKIP_SUBTREE;
                    }
                });
            } catch (IOException ignored) {}

            return builder.build();
        });
    }
}
