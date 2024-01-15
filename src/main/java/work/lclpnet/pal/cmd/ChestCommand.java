package work.lclpnet.pal.cmd;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerFactory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import work.lclpnet.kibu.plugin.cmd.CommandRegistrar;
import work.lclpnet.kibu.plugin.cmd.KibuCommand;
import work.lclpnet.kibu.translate.TranslationService;
import work.lclpnet.kibu.translate.text.RootText;
import work.lclpnet.pal.service.CommandService;

import javax.inject.Inject;

import static work.lclpnet.kibu.translate.text.FormatWrapper.styled;

public class ChestCommand implements KibuCommand {

    private static final Text containerName = Text.translatable("container.enderchest");
    private final CommandService commandService;

    @Inject
    public ChestCommand(CommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    public void register(CommandRegistrar registrar) {
        registrar.registerCommand(command());
    }

    private LiteralArgumentBuilder<ServerCommandSource> command() {
        return CommandManager.literal("chest")
                .requires(s -> s.hasPermissionLevel(2))
                .executes(this::ownChest)
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(this::targetChest));
    }

    private int ownChest(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();

        openChestOf(player, player);

        return 1;
    }

    private void openChestOf(ServerPlayerEntity player, ServerPlayerEntity target) {
        EnderChestInventory chestInventory = target.getEnderChestInventory();
        ScreenHandlerFactory baseFactory = (syncId, inventory, p) -> GenericContainerScreenHandler.createGeneric9x3(syncId, inventory, chestInventory);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(baseFactory, containerName));

        TranslationService translationService = commandService.getTranslationService();
        RootText text;

        if (player != target) {
            text = translationService.translateText(player, "pal.cmd.chest.opened", styled(target.getNameForScoreboard()).formatted(Formatting.YELLOW));
        } else {
            text = translationService.translateText(player, "pal.cmd.chest.self");
        }

        player.sendMessage(text.formatted(Formatting.GREEN));
    }

    private int targetChest(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");

        openChestOf(player, target);

        return 1;
    }
}
