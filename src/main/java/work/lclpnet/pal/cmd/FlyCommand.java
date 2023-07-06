package work.lclpnet.pal.cmd;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import work.lclpnet.kibu.plugin.cmd.CommandRegistrar;
import work.lclpnet.kibu.translate.TranslationService;
import work.lclpnet.kibu.translate.text.RootText;
import work.lclpnet.pal.service.CommandService;

import static work.lclpnet.kibu.translate.text.FormatWrapper.styled;

public class FlyCommand {

    private final CommandService commandService;

    public FlyCommand(CommandService commandService) {
        this.commandService = commandService;
    }

    public void register(CommandRegistrar registrar) {
        registrar.registerCommand(command());
    }

    private LiteralArgumentBuilder<ServerCommandSource> command() {
        return CommandManager.literal("fly")
                .requires(s -> s.hasPermissionLevel(2))
                .executes(this::flySelf)
                .then(CommandManager.argument("players", EntityArgumentType.players())
                        .executes(this::fly));
    }

    private int flySelf(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        enableFlying(player);

        return 0;
    }

    private void enableFlying(ServerPlayerEntity player) {
        PlayerAbilities abilities = player.getAbilities();

        abilities.allowFlying = !abilities.allowFlying;

        if (!abilities.allowFlying && abilities.flying) {
            abilities.flying = false;
        }

        player.sendAbilitiesUpdate();

        TranslationService translationService = commandService.getTranslationService();
        Text text;

        if (abilities.allowFlying) {
            text = translationService.translateText(player, "pal.cmd.fly.enabled").formatted(Formatting.GREEN);
        } else {
            text = translationService.translateText(player, "pal.cmd.fly.disabled").formatted(Formatting.RED);
        }

        player.sendMessage(text);
    }

    private int fly(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var players = EntityArgumentType.getPlayers(ctx, "players");

        for (ServerPlayerEntity player : players) {
            enableFlying(player);
        }

        ServerCommandSource source = ctx.getSource();
        RootText msg;

        int count = players.size();

        if (count == 1) {
            msg = commandService.translateText(source, "pal.cmd.fly.single", styled(players.iterator().next().getDisplayName().getString()).formatted(Formatting.YELLOW));
        } else {
            msg = commandService.translateText(source, "pal.cmd.fly.multiple", styled(count).formatted(Formatting.YELLOW));
        }

        source.sendMessage(msg.formatted(Formatting.GREEN));

        return count;
    }
}
