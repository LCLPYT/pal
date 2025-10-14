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
import work.lclpnet.kibu.cmd.type.CommandRegistrar;
import work.lclpnet.kibu.cmd.type.KibuCommand;
import work.lclpnet.kibu.translate.Translations;
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
                                .suggests(new WorldSuggestionProvider())
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

        var spawnPoint = world.getSpawnPoint();

        for (Entity entity : entities) {
            BlockPos pos = entity.getWorldSpawnPos(world, spawnPoint.getPos());
            teleportEntity(entity, world, pos, spawnPoint.yaw(), spawnPoint.pitch());
        }

        ServerCommandSource source = ctx.getSource();
        RootText msg;

        int count = entities.size();

        if (count == 1) {
            msg = commandService.translateText(source, "pal.cmd.world.teleport.single",
                    styled(entities.iterator().next().getNameForScoreboard()).formatted(Formatting.YELLOW),
                    styled(world.getRegistryKey().getValue()).formatted(Formatting.YELLOW));
        } else {
            msg = commandService.translateText(source, "pal.cmd.world.teleport.multiple",
                    styled(count).formatted(Formatting.YELLOW),
                    styled(world.getRegistryKey().getValue()).formatted(Formatting.YELLOW));
        }

        source.sendMessage(msg.formatted(Formatting.GREEN));

        return count;
    }

    private int teleportSelf(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerWorld world = WorldSuggestionProvider.getWorld(ctx, "world", commandService);

        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        var spawnPoint = world.getSpawnPoint();
        BlockPos pos = player.getWorldSpawnPos(world, spawnPoint.getPos());

        teleportEntity(player, world, pos, spawnPoint.yaw(), spawnPoint.pitch());

        Translations translations = commandService.getTranslations();
        Identifier id = world.getRegistryKey().getValue();

        source.sendMessage(translations.translateText(source, "pal.cmd.world.teleport.single",
                styled(player.getNameForScoreboard(), Formatting.YELLOW),
                styled(id, Formatting.YELLOW)).formatted(Formatting.GREEN));

        return 1;
    }

    private void teleportEntity(Entity entity, ServerWorld world, BlockPos pos, float yaw, float pitch) {
        entity.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.of(), yaw, pitch, true);
    }
}
