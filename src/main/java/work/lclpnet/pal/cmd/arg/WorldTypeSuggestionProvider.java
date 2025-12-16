package work.lclpnet.pal.cmd.arg;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import work.lclpnet.pal.world.PalWorldTypes;

import java.util.concurrent.CompletableFuture;

public class WorldTypeSuggestionProvider implements SuggestionProvider<CommandSourceStack> {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        MinecraftServer server = context.getSource().getServer();

        PalWorldTypes.getInstance()
                .getWorldTypes(server)
                .stream()
                .map(ResourceLocation::toString)
                .forEach(builder::suggest);

        return builder.buildFuture();
    }
}
