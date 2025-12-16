package work.lclpnet.pal.cmd;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import work.lclpnet.kibu.cmd.type.CommandFactory;
import work.lclpnet.kibu.cmd.type.CommandRegistrar;
import work.lclpnet.kibu.cmd.type.KibuCommand;
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

    private CommandFactory<CommandSourceStack> command() {
        return ctx -> Commands.literal("rename")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.literal("text")
                                .then(Commands.argument("text", ComponentArgument.textComponent(ctx.registryAccess()))
                                        .executes(this::renameText)))
                        .then(Commands.literal("string")
                                .then(Commands.argument("string", StringArgumentType.greedyString())
                                        .executes(this::renameString))));
    }

    private int renameText(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
        Component text = ComponentArgument.getRawComponent(ctx, "text");

        return renameTo(ctx, player, text);
    }

    private int renameString(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
        String string = StringArgumentType.getString(ctx, "string");

        Component name = formattingService.parseText(string, '&');

        return renameTo(ctx, player, name);
    }

    private int renameTo(CommandContext<CommandSourceStack> ctx, ServerPlayer player, Component name) {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);

        CommandSourceStack src = ctx.getSource();
        boolean self = src.getPlayer() == player;

        if (stack.isEmpty()) {
            src.sendSystemMessage(commandService.translateText(src, self ? "pal.cmd.rename.no_item.self" : "pal.cmd.rename.no_item").formatted(ChatFormatting.RED));
            return 0;
        }

        stack.set(DataComponents.CUSTOM_NAME, name.copy().setStyle(name.getStyle().applyTo(Style.EMPTY.withItalic(false))));

        MutableComponent msgName = name.copy().setStyle(name.getStyle().applyTo(Style.EMPTY.applyFormat(ChatFormatting.WHITE)));
        src.sendSystemMessage(commandService.translateText(src, self ? "pal.cmd.rename.renamed.self" : "pal.cmd.rename.renamed", msgName).formatted(ChatFormatting.GREEN));

        return 1;
    }
}
