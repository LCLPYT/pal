package work.lclpnet.pal.cmd;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
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

    private LiteralArgumentBuilder<ServerCommandSource> command(String name) {
        return CommandManager.literal(name)
                .requires(s -> s.hasPermissionLevel(2))
                .executes(this::ownInventory)
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(this::targetInventory));
    }

    private int ownInventory(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();

        openInventoryOf(player, player);

        return 1;
    }

    private void openInventoryOf(ServerPlayerEntity player, ServerPlayerEntity target) {
        PlayerInventory inv = target.getInventory();
        ScreenHandlerFactory baseFactory = (syncId, inventory, p) -> new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X4, syncId, inventory, inv, 4);

        TranslationService translationService = commandService.getTranslationService();
        RootText title = translationService.translateText(player, "pal.cmd.inv.title", target.getEntityName());

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(baseFactory, title));

        RootText text;

        if (player != target) {
            text = translationService.translateText(player, "pal.cmd.inv.opened", styled(target.getEntityName()).formatted(Formatting.YELLOW));
        } else {
            text = translationService.translateText(player, "pal.cmd.inv.self");
        }

        player.sendMessage(text.formatted(Formatting.GREEN));
    }

    private int targetInventory(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");

        openInventoryOf(player, target);

        return 1;
    }
}
