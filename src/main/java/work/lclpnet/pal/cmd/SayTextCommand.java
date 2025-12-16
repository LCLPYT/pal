package work.lclpnet.pal.cmd;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.server.players.PlayerList;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import work.lclpnet.kibu.cmd.type.CommandFactory;
import work.lclpnet.kibu.cmd.type.CommandRegistrar;
import work.lclpnet.kibu.cmd.type.KibuCommand;
import work.lclpnet.pal.service.FormattingService;

import javax.inject.Inject;

public class SayTextCommand implements KibuCommand {

    private final FormattingService formattingService;

    @Inject
    public SayTextCommand(FormattingService formattingService) {
        this.formattingService = formattingService;
    }

    @Override
    public void register(CommandRegistrar registrar) {
        registrar.registerCommand(command());
    }

    private CommandFactory<CommandSourceStack> command() {
        return ctx -> Commands.literal("saytext")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("text")
                        .then(Commands.argument("message", ComponentArgument.textComponent(ctx.registryAccess()))
                                .executes(this::sayText)))
                .then(Commands.literal("string")
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(this::sayString)));
    }

    private int sayString(CommandContext<CommandSourceStack> ctx) {
        String str = StringArgumentType.getString(ctx, "message");
        Component text = formattingService.parseText(str, '&');

        broadcast(ctx.getSource(), text);

        return 1;
    }

    private int sayText(CommandContext<CommandSourceStack> ctx) {
        Component text = ComponentArgument.getRawComponent(ctx, "message");

        broadcast(ctx.getSource(), text);

        return 1;
    }

    private void broadcast(CommandSourceStack source, Component msg) {
        PlayerList playerManager = source.getServer().getPlayerList();
        playerManager.broadcastSystemMessage(msg, false);
    }
}
