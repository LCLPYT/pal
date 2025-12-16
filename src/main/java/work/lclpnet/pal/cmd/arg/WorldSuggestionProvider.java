package work.lclpnet.pal.cmd.arg;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import work.lclpnet.pal.service.CommandService;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class WorldSuggestionProvider implements SuggestionProvider<CommandSourceStack> {

    private final Predicate<ServerLevel> predicate;

    public WorldSuggestionProvider() {
        this(world -> true);
    }

    public WorldSuggestionProvider(Predicate<ServerLevel> predicate) {
        this.predicate = predicate;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        MinecraftServer server = context.getSource().getServer();
        if (server == null) return builder.buildFuture();

        for (var key : server.levelKeys()) {
            ServerLevel world = server.getLevel(key);
            if (world == null) continue;

            if (predicate.test(world)) {
                builder.suggest(key.location().toString());
            }
        }

        return builder.buildFuture();
    }

    @NotNull
    public static ServerLevel getWorld(CommandContext<CommandSourceStack> ctx, String name, CommandService commandService) throws CommandSyntaxException {
        ResourceLocation worldId = ResourceLocationArgument.getId(ctx, name);

        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();

        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, worldId);

        ServerLevel world = server.getLevel(key);

        if (world == null) {
            throw commandService.createUnknownWorldException(source, worldId);
        }

        return world;
    }
}
