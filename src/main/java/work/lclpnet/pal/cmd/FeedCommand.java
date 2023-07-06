package work.lclpnet.pal.cmd;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import work.lclpnet.kibu.plugin.cmd.CommandRegistrar;
import work.lclpnet.kibu.translate.TranslationService;
import work.lclpnet.kibu.translate.text.RootText;
import work.lclpnet.pal.service.CommandService;

public class FeedCommand {

    private final CommandService commandService;

    public FeedCommand(CommandService commandService) {
        this.commandService = commandService;
    }

    public void register(CommandRegistrar registrar) {
        registrar.registerCommand(command());
    }

    private LiteralArgumentBuilder<ServerCommandSource> command() {
        return CommandManager.literal("feed")
                .requires(s -> s.hasPermissionLevel(2))
                .executes(this::feedSelf)
                .then(CommandManager.argument("players", EntityArgumentType.players())
                        .executes(this::feed));
    }

    private int feedSelf(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        feedPlayer(player);

        return 0;
    }

    private void feedPlayer(ServerPlayerEntity player) {
        HungerManager hungerManager = player.getHungerManager();

        hungerManager.setFoodLevel(20);
        hungerManager.setSaturationLevel(5f);

        TranslationService translationService = commandService.getTranslationService();
        player.sendMessage(translationService.translateText(player, "pal.cmd.feed.fed_you").formatted(Formatting.GREEN));
    }

    private int feed(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var players = EntityArgumentType.getPlayers(ctx, "players");

        for (ServerPlayerEntity player : players) {
            feedPlayer(player);
        }

        ServerCommandSource source = ctx.getSource();
        RootText msg;

        int count = players.size();

        if (count == 1) {
            msg = commandService.translateText(source, "pal.cmd.feed.single", players.iterator().next().getDisplayName().getString());
        } else {
            msg = commandService.translateText(source, "pal.cmd.feed.multiple", count);
        }

        source.sendMessage(msg.formatted(Formatting.GREEN));

        return count;
    }
}
