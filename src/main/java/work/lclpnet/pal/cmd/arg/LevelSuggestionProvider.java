package work.lclpnet.pal.cmd.arg;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import work.lclpnet.pal.service.CommandService;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class LevelSuggestionProvider implements SuggestionProvider<CommandSourceStack> {

    private final Predicate<ServerLevel> predicate;

    public LevelSuggestionProvider() {
        this(_ -> true);
    }

    public LevelSuggestionProvider(Predicate<ServerLevel> predicate) {
        this.predicate = predicate;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        MinecraftServer server = context.getSource().getServer();

        for (var key : server.levelKeys()) {
            ServerLevel level = server.getLevel(key);
            if (level == null) continue;

            if (predicate.test(level)) {
                builder.suggest(key.identifier().toString());
            }
        }

        return builder.buildFuture();
    }

    @NotNull
    public static ServerLevel getLevel(CommandContext<CommandSourceStack> ctx, String name, CommandService commandService) throws CommandSyntaxException {
        Identifier levelId = IdentifierArgument.getId(ctx, name);

        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();

        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, levelId);

        ServerLevel level = server.getLevel(key);

        if (level == null) {
            throw commandService.createUnknownLevelException(source, levelId);
        }

        return level;
    }
}
