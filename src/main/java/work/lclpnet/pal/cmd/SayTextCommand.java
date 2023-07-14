package work.lclpnet.pal.cmd;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import work.lclpnet.kibu.plugin.cmd.CommandRegistrar;
import work.lclpnet.pal.service.FormattingService;

import javax.inject.Inject;

public class SayTextCommand implements PalCommand {

    private final FormattingService formattingService;

    @Inject
    public SayTextCommand(FormattingService formattingService) {
        this.formattingService = formattingService;
    }

    @Override
    public void register(CommandRegistrar registrar) {
        registrar.registerCommand(command());
    }

    private LiteralArgumentBuilder<ServerCommandSource> command() {
        return CommandManager.literal("saytext")
                .requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.literal("text")
                        .then(CommandManager.argument("message", TextArgumentType.text())
                                .executes(this::sayText)))
                .then(CommandManager.literal("string")
                        .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                .executes(this::sayString)));
    }

    private int sayString(CommandContext<ServerCommandSource> ctx) {
        String str = StringArgumentType.getString(ctx, "message");
        Text text = formattingService.parseText(str, '&');

        broadcast(ctx.getSource(), text);

        return 1;
    }

    private int sayText(CommandContext<ServerCommandSource> ctx) {
        Text text = TextArgumentType.getTextArgument(ctx, "message");

        broadcast(ctx.getSource(), text);

        return 1;
    }

    private void broadcast(ServerCommandSource source, Text msg) {
        PlayerManager playerManager = source.getServer().getPlayerManager();
        playerManager.broadcast(msg, false);
    }
}
