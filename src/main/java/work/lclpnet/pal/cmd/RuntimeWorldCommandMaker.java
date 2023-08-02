package work.lclpnet.pal.cmd;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import work.lclpnet.kibu.translate.text.FormatWrapper;
import work.lclpnet.kibu.world.KibuWorlds;
import work.lclpnet.pal.cmd.arg.WorldSuggestionProvider;
import work.lclpnet.pal.service.CommandService;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.function.Function;

public class RuntimeWorldCommandMaker {

    private final CommandService commandService;

    public RuntimeWorldCommandMaker(CommandService commandService) {
        this.commandService = commandService;
    }

    public void inject(LiteralArgumentBuilder<ServerCommandSource> node) {
        node.then(CommandManager.literal("create")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(CommandManager.literal("temporary")
                                .then(CommandManager.argument("type", DimensionArgumentType.dimension())
                                        .executes(this::createTemporaryWorld)))
                        .then(CommandManager.literal("persistent")
                                .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                                        .then(CommandManager.argument("type", DimensionArgumentType.dimension())
                                                .executes(this::createPersistentWorld)))))
                .then(CommandManager.literal("unload")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(CommandManager.argument("world", IdentifierArgumentType.identifier())
                                .suggests(new WorldSuggestionProvider(commandService, this::isRuntimeWorld))
                                .executes(this::unload)));
    }

    private boolean isRuntimeWorld(ServerWorld world) {
        return KibuWorlds.getInstance().getRuntimeWorldHandle(world).isPresent();
    }

    private int createTemporaryWorld(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        MinecraftServer server = ctx.getSource().getServer();

        return createRuntimeWorld(ctx, worldConfig -> Fantasy.get(server).openTemporaryWorld(worldConfig));
    }

    private int createRuntimeWorld(CommandContext<ServerCommandSource> ctx, Function<RuntimeWorldConfig, RuntimeWorldHandle> factory) throws CommandSyntaxException {
        ServerWorld world = DimensionArgumentType.getDimensionArgument(ctx, "type");

        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                .setDimensionType(world.getDimensionEntry())
                .setGenerator(world.getChunkManager().getChunkGenerator());

        RuntimeWorldHandle handle = factory.apply(worldConfig);

        Identifier worldId = handle.getRegistryKey().getValue();
        ServerCommandSource source = ctx.getSource();

        source.sendMessage(commandService.translateText(source, "pal.cmd.world.create.success",
                FormatWrapper.styled(worldId, Formatting.YELLOW),
                FormatWrapper.styled(world.getDimensionKey().getValue(), Formatting.YELLOW)).formatted(Formatting.GREEN));

        return 1;
    }

    private int createPersistentWorld(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Identifier id = IdentifierArgumentType.getIdentifier(ctx, "id");
        MinecraftServer server = ctx.getSource().getServer();

        return createRuntimeWorld(ctx, worldConfig -> Fantasy.get(server).getOrOpenPersistentWorld(id, worldConfig));
    }

    private int unload(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerWorld world = WorldSuggestionProvider.getWorld(context, "world", commandService);

        ServerCommandSource source = context.getSource();

        var optHandle = KibuWorlds.getInstance().getRuntimeWorldHandle(world);

        if (optHandle.isEmpty()) {
            throw commandService.createNotUnloadableWorldException(source);
        }

        optHandle.get().unload();

        Identifier id = world.getDimensionKey().getValue();

        source.sendMessage(commandService.translateText(source, "pal.cmd.world.unload.success",
                FormatWrapper.styled(id)));

        return 1;
    }
}
