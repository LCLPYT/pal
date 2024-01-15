package work.lclpnet.pal.cmd;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import work.lclpnet.kibu.plugin.cmd.CommandRegistrar;
import work.lclpnet.kibu.plugin.cmd.KibuCommand;
import work.lclpnet.kibu.translate.TranslationService;
import work.lclpnet.kibu.translate.text.RootText;
import work.lclpnet.pal.service.CommandService;

import javax.inject.Inject;

import static work.lclpnet.kibu.translate.text.FormatWrapper.styled;

public class PingCommand implements KibuCommand {

    private final CommandService commandService;

    @Inject
    public PingCommand(CommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    public void register(CommandRegistrar registrar) {
        registrar.registerCommand(command());
    }

    private LiteralArgumentBuilder<ServerCommandSource> command() {
        return CommandManager.literal("ping")
                .executes(this::pingSelf)
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(this::pingOther));
    }

    private int pingSelf(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        sendPingOf(player, player);

        return 1;
    }

    private void sendPingOf(ServerPlayerEntity player, ServerPlayerEntity target) {
        TranslationService translationService = commandService.getTranslationService();
        RootText text;

        long latencyMs = target.networkHandler.getLatency();

        if (player == target) {
            text = translationService.translateText(player, "pal.cmd.ping.self",
                    styled(latencyMs).formatted(Formatting.YELLOW),
                    styled(latencyMs / 1000f).formatted(Formatting.YELLOW));
        } else {
            text = translationService.translateText(player, "pal.cmd.ping.other",
                    target.getNameForScoreboard(),
                    styled(latencyMs).formatted(Formatting.YELLOW),
                    styled(latencyMs / 1000f).formatted(Formatting.YELLOW));
        }

        player.sendMessage(text.formatted(Formatting.GREEN));
    }

    private int pingOther(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");

        sendPingOf(player, target);

        return 1;
    }
}
