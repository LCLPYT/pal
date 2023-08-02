package work.lclpnet.pal.cmd;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import work.lclpnet.kibu.plugin.cmd.CommandRegistrar;
import work.lclpnet.kibu.plugin.cmd.KibuCommand;
import work.lclpnet.kibu.translate.TranslationService;
import work.lclpnet.kibu.translate.text.RootText;
import work.lclpnet.pal.cmd.arg.WorldSuggestionProvider;
import work.lclpnet.pal.service.CommandService;

import javax.inject.Inject;
import java.util.Set;

import static work.lclpnet.kibu.translate.text.FormatWrapper.styled;

public class WorldCommand implements KibuCommand {

    private final CommandService commandService;

    @Inject
    public WorldCommand(CommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    public void register(CommandRegistrar registrar) {
        registrar.registerCommand(command());
    }

    private LiteralArgumentBuilder<ServerCommandSource> command() {
        var node = CommandManager.literal("world")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("tp")
                        .then(CommandManager.argument("world", IdentifierArgumentType.identifier())
                                .suggests(new WorldSuggestionProvider(commandService))
                                .executes(this::teleportSelf)
                                .then(CommandManager.argument("entities", EntityArgumentType.entities())
                                        .executes(this::teleport))));

        if (FabricLoader.getInstance().isModLoaded("fantasy")) {
            registerModificationCommands(node);
        }

        return node;
    }

    private void registerModificationCommands(LiteralArgumentBuilder<ServerCommandSource> node) {
        new RuntimeWorldCommandMaker(commandService).inject(node);
    }

    private int teleport(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerWorld world = WorldSuggestionProvider.getWorld(ctx, "world", commandService);
        var entities = EntityArgumentType.getEntities(ctx, "entities");

        for (Entity entity : entities) {
            teleportEntity(entity, world);
        }

        ServerCommandSource source = ctx.getSource();
        RootText msg;

        int count = entities.size();

        if (count == 1) {
            msg = commandService.translateText(source, "pal.cmd.world.teleport.single", styled(entities.iterator().next().getDisplayName().getString()).formatted(Formatting.YELLOW));
        } else {
            msg = commandService.translateText(source, "pal.cmd.world.teleport.multiple", styled(count).formatted(Formatting.YELLOW));
        }

        source.sendMessage(msg.formatted(Formatting.GREEN));

        return count;
    }

    private int teleportSelf(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerWorld world = WorldSuggestionProvider.getWorld(ctx, "world", commandService);

        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        teleportEntity(player, world);

        TranslationService translationService = commandService.getTranslationService();
        Identifier id = world.getRegistryKey().getValue();

        source.sendMessage(translationService.translateText(source, "pal.cmd.world.teleport.single",
                styled(player.getEntityName(), Formatting.YELLOW),
                styled(id, Formatting.YELLOW)).formatted(Formatting.GREEN));

        return 1;
    }

    private void teleportEntity(Entity entity, ServerWorld world) {
        BlockPos spawnPos = world.getSpawnPos();
        entity.teleport(world, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, Set.of(), 0, 0);
    }
}
