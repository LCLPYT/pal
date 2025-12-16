package work.lclpnet.pal.cmd;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import work.lclpnet.kibu.cmd.type.CommandRegistrar;
import work.lclpnet.kibu.cmd.type.KibuCommand;
import work.lclpnet.kibu.translate.Translations;
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

    private LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("ping")
                .executes(this::pingSelf)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(this::pingOther));
    }

    private int pingSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();

        sendPingOf(player, player);

        return 1;
    }

    private void sendPingOf(ServerPlayer player, ServerPlayer target) {
        Translations translations = commandService.getTranslations();
        RootText text;

        long latencyMs = target.connection.latency();

        if (player == target) {
            text = translations.translateText(player, "pal.cmd.ping.self",
                    styled(latencyMs).formatted(ChatFormatting.YELLOW),
                    styled(latencyMs / 1000f).formatted(ChatFormatting.YELLOW));
        } else {
            text = translations.translateText(player, "pal.cmd.ping.other",
                    target.getScoreboardName(),
                    styled(latencyMs).formatted(ChatFormatting.YELLOW),
                    styled(latencyMs / 1000f).formatted(ChatFormatting.YELLOW));
        }

        player.sendSystemMessage(text.formatted(ChatFormatting.GREEN));
    }

    private int pingOther(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

        sendPingOf(player, target);

        return 1;
    }
}
