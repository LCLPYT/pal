package work.lclpnet.pal.cmd;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.SimpleMenuProvider;
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

public class InventoryCommand implements KibuCommand {

    private final CommandService commandService;

    @Inject
    public InventoryCommand(CommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    public void register(CommandRegistrar registrar) {
        registrar.registerCommand(command("inventory"));
        registrar.registerCommand(command("inv"));
    }

    private LiteralArgumentBuilder<CommandSourceStack> command(String name) {
        return Commands.literal(name)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(this::ownInventory)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(this::targetInventory));
    }

    private int ownInventory(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        openInventoryOf(player, player);

        return 1;
    }

    private void openInventoryOf(ServerPlayer player, ServerPlayer target) {
        Inventory inv = target.getInventory();
        MenuConstructor baseFactory = (syncId, inventory, p) -> new ChestMenu(MenuType.GENERIC_9x4, syncId, inventory, inv, 4);

        Translations translations = commandService.getTranslations();
        RootText title = translations.translateText(player, "pal.cmd.inv.title", target.getScoreboardName());

        player.openMenu(new SimpleMenuProvider(baseFactory, title));

        RootText text;

        if (player != target) {
            text = translations.translateText(player, "pal.cmd.inv.opened", styled(target.getScoreboardName()).formatted(ChatFormatting.YELLOW));
        } else {
            text = translations.translateText(player, "pal.cmd.inv.self");
        }

        player.sendSystemMessage(text.formatted(ChatFormatting.GREEN));
    }

    private int targetInventory(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

        openInventoryOf(player, target);

        return 1;
    }
}
