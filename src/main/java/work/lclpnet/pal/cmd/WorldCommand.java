package work.lclpnet.pal.cmd;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
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

    private LiteralArgumentBuilder<CommandSourceStack> command() {
        var node = Commands.literal("world")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("tp")
                        .then(Commands.argument("world", ResourceLocationArgument.id())
                                .suggests(new WorldSuggestionProvider())
                                .executes(this::teleportSelf)
                                .then(Commands.argument("entities", EntityArgument.entities())
                                        .executes(this::teleport))));

        if (FabricLoader.getInstance().isModLoaded("fantasy")) {
            registerModificationCommands(node);
        }

        return node;
    }

    private void registerModificationCommands(LiteralArgumentBuilder<CommandSourceStack> node) {
        new RuntimeWorldCommandMaker(commandService).inject(node);
    }

    private int teleport(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel world = WorldSuggestionProvider.getWorld(ctx, "world", commandService);
        var entities = EntityArgument.getEntities(ctx, "entities");

        var spawnPoint = world.getRespawnData();

        for (Entity entity : entities) {
            BlockPos pos = entity.adjustSpawnLocation(world, spawnPoint.pos());
            teleportEntity(entity, world, pos, spawnPoint.yaw(), spawnPoint.pitch());
        }

        CommandSourceStack source = ctx.getSource();
        RootText msg;

        int count = entities.size();

        if (count == 1) {
            msg = commandService.translateText(source, "pal.cmd.world.teleport.single",
                    styled(entities.iterator().next().getScoreboardName()).formatted(ChatFormatting.YELLOW),
                    styled(world.dimension().location()).formatted(ChatFormatting.YELLOW));
        } else {
            msg = commandService.translateText(source, "pal.cmd.world.teleport.multiple",
                    styled(count).formatted(ChatFormatting.YELLOW),
                    styled(world.dimension().location()).formatted(ChatFormatting.YELLOW));
        }

        source.sendSystemMessage(msg.formatted(ChatFormatting.GREEN));

        return count;
    }

    private int teleportSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel world = WorldSuggestionProvider.getWorld(ctx, "world", commandService);

        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();

        var spawnPoint = world.getRespawnData();
        BlockPos pos = player.adjustSpawnLocation(world, spawnPoint.pos());

        teleportEntity(player, world, pos, spawnPoint.yaw(), spawnPoint.pitch());

        Translations translations = commandService.getTranslations();
        ResourceLocation id = world.dimension().location();

        source.sendSystemMessage(translations.translateText(source, "pal.cmd.world.teleport.single",
                styled(player.getScoreboardName(), ChatFormatting.YELLOW),
                styled(id, ChatFormatting.YELLOW)).formatted(ChatFormatting.GREEN));

        return 1;
    }

    private void teleportEntity(Entity entity, ServerLevel world, BlockPos pos, float yaw, float pitch) {
        entity.teleportTo(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.of(), yaw, pitch, true);
    }
}
