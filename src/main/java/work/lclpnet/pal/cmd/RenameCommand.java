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
import org.jetbrains.annotations.Nullable;
import work.lclpnet.kibu.plugin.cmd.CommandRegistrar;
import work.lclpnet.pal.service.CommandService;

import java.util.ArrayList;
import java.util.List;

public class RenameCommand {

    private final CommandService commandService;

    public RenameCommand(CommandService commandService) {
        this.commandService = commandService;
    }

    public void register(CommandRegistrar registrar) {
        registrar.registerCommand(command());
    }

    private LiteralArgumentBuilder<ServerCommandSource> command() {
        return CommandManager.literal("rename")
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

        String[] parts = string.split("&");
        Style style = Style.EMPTY;

        List<MutableText> texts = new ArrayList<>();
        StringBuilder carry = new StringBuilder(parts[0]);

        for (int i = 1, partsLength = parts.length; i < partsLength; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;

            Formatting format = getFormatting(part.charAt(0));

            if (format == null) {
                carry.append(part);
                continue;
            }

            if (carry.length() > 0) {
                texts.add(Text.literal(carry.toString()).setStyle(style));
                carry.setLength(0);
            }

            style = style.withFormatting(format);

            texts.add(Text.literal(part.substring(1)).setStyle(style));
        }

        if (carry.length() > 0) {
            texts.add(Text.literal(carry.toString()).setStyle(style));
        }

        if (texts.isEmpty()) {
            return renameTo(ctx, player, Text.empty());
        }

        MutableText root = texts.get(0);

        for (int i = 1, len = texts.size(); i < len; i++) {
            root.append(texts.get(i));
        }

        return renameTo(ctx, player, root);
    }

    @Nullable
    private Formatting getFormatting(char spec) {
        return switch (spec) {
            case '0' -> Formatting.BLACK;
            case '1' -> Formatting.DARK_BLUE;
            case '2' -> Formatting.DARK_GREEN;
            case '3' -> Formatting.DARK_AQUA;
            case '4' -> Formatting.DARK_RED;
            case '5' -> Formatting.DARK_PURPLE;
            case '6' -> Formatting.GOLD;
            case '7' -> Formatting.GRAY;
            case '8' -> Formatting.DARK_GRAY;
            case '9' -> Formatting.BLUE;
            case 'a' -> Formatting.GREEN;
            case 'b' -> Formatting.AQUA;
            case 'c' -> Formatting.RED;
            case 'd' -> Formatting.LIGHT_PURPLE;
            case 'e' -> Formatting.YELLOW;
            case 'f' -> Formatting.WHITE;
            case 'k' -> Formatting.OBFUSCATED;
            case 'l' -> Formatting.BOLD;
            case 'm' -> Formatting.STRIKETHROUGH;
            case 'n' -> Formatting.UNDERLINE;
            case 'o' -> Formatting.ITALIC;
            case 'r' -> Formatting.RESET;
            default -> null;
        };
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
