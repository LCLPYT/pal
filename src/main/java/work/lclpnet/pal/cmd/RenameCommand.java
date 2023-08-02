package work.lclpnet.pal.cmd;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import work.lclpnet.kibu.plugin.cmd.CommandRegistrar;
import work.lclpnet.kibu.plugin.cmd.KibuCommand;
import work.lclpnet.pal.service.CommandService;
import work.lclpnet.pal.service.FormattingService;

import javax.inject.Inject;

public class RenameCommand implements KibuCommand {

    private final CommandService commandService;
    private final FormattingService formattingService;

    @Inject
    public RenameCommand(CommandService commandService, FormattingService formattingService) {
        this.commandService = commandService;
        this.formattingService = formattingService;
    }

    @Override
    public void register(CommandRegistrar registrar) {
        registrar.registerCommand(command());
    }

    private LiteralArgumentBuilder<ServerCommandSource> command() {
        return CommandManager.literal("rename")
                .requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .then(CommandManager.literal("text")
                                .then(CommandManager.argument("text", TextArgumentType.text())
                                        .executes(this::renameText)))
                        .then(CommandManager.literal("string")
                                .then(CommandManager.argument("string", StringArgumentType.greedyString())
                                        .executes(this::renameString))));
    }

    private int renameText(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "target");
        Text text = TextArgumentType.getTextArgument(ctx, "text");

        return renameTo(ctx, player, text);
    }

    private int renameString(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "target");
        String string = StringArgumentType.getString(ctx, "string");

        Text name = formattingService.parseText(string, '&');

        return renameTo(ctx, player, name);
    }

    private int renameTo(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, Text name) {
        ItemStack stack = player.getStackInHand(Hand.MAIN_HAND);

        ServerCommandSource src = ctx.getSource();
        boolean self = src.getPlayer() == player;

        if (stack.isEmpty()) {
            src.sendMessage(commandService.translateText(src, self ? "pal.cmd.rename.no_item.self" : "pal.cmd.rename.no_item").formatted(Formatting.RED));
            return 0;
        }

        stack.setCustomName(name.copy().setStyle(name.getStyle().withParent(Style.EMPTY.withItalic(false))));

        MutableText msgName = name.copy().setStyle(name.getStyle().withParent(Style.EMPTY.withFormatting(Formatting.WHITE)));
        src.sendMessage(commandService.translateText(src, self ? "pal.cmd.rename.renamed.self" : "pal.cmd.rename.renamed", msgName).formatted(Formatting.GREEN));

        return 1;
    }
}
