package work.lclpnet.pal.cmd;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import work.lclpnet.kibu.cmd.type.CommandRegistrar;
import work.lclpnet.kibu.cmd.type.KibuCommand;
import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.kibu.translate.text.RootText;
import work.lclpnet.pal.service.CommandService;

import javax.inject.Inject;

import static work.lclpnet.kibu.translate.text.FormatWrapper.styled;

public class ChestCommand implements KibuCommand {

    private static final Component containerName = Component.translatable("container.enderchest");
    private final CommandService commandService;

    @Inject
    public ChestCommand(CommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    public void register(CommandRegistrar registrar) {
        registrar.registerCommand(command());
    }

    private LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("chest")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(this::ownChest)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(this::targetChest));
    }

    private int ownChest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        openChestOf(player, player);

        return 1;
    }

    private void openChestOf(ServerPlayer player, ServerPlayer target) {
        PlayerEnderChestContainer chestInventory = target.getEnderChestInventory();
        MenuConstructor baseFactory = (syncId, inventory, p) -> ChestMenu.threeRows(syncId, inventory, chestInventory);
        player.openMenu(new SimpleMenuProvider(baseFactory, containerName));

        Translations translations = commandService.getTranslations();
        RootText text;

        if (player != target) {
            text = translations.translateText(player, "pal.cmd.chest.opened", styled(target.getScoreboardName()).formatted(ChatFormatting.YELLOW));
        } else {
            text = translations.translateText(player, "pal.cmd.chest.self");
        }

        player.sendSystemMessage(text.formatted(ChatFormatting.GREEN));
    }

    private int targetChest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

        openChestOf(player, target);

        return 1;
    }
}
