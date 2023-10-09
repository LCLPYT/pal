package work.lclpnet.pal.cmd;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GeneratorOptions;
import work.lclpnet.kibu.translate.text.FormatWrapper;
import work.lclpnet.kibu.world.KibuWorlds;
import work.lclpnet.kibu.world.WorldManager;
import work.lclpnet.pal.PalPlugin;
import work.lclpnet.pal.cmd.arg.PersistentWorldSuggestionProvider;
import work.lclpnet.pal.cmd.arg.WorldSuggestionProvider;
import work.lclpnet.pal.cmd.arg.WorldTypeSuggestionProvider;
import work.lclpnet.pal.service.CommandService;
import work.lclpnet.pal.world.PalWorldTypes;
import work.lclpnet.pal.world.WorldType;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.function.Function;

public class RuntimeWorldCommandMaker {

    private final CommandService commandService;
    private final Random random = new Random();

    public RuntimeWorldCommandMaker(CommandService commandService) {
        this.commandService = commandService;
    }

    public void inject(LiteralArgumentBuilder<ServerCommandSource> node) {
        var worldTypeProvider = new WorldTypeSuggestionProvider();

        node.then(CommandManager.literal("create")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(CommandManager.literal("temporary")
                                .then(CommandManager.argument("type", IdentifierArgumentType.identifier())
                                        .suggests(worldTypeProvider)
                                        .executes(this::createTemporaryWorld)
                                        .then(CommandManager.argument("seed", StringArgumentType.greedyString())
                                                .executes(this::createTemporaryWorldSeed))))
                        .then(CommandManager.literal("persistent")
                                .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                                        .then(CommandManager.argument("type", IdentifierArgumentType.identifier())
                                                .suggests(worldTypeProvider)
                                                .executes(this::createPersistentWorld)
                                                .then(CommandManager.argument("seed", StringArgumentType.greedyString())
                                                        .executes(this::createPersistentWorldSeed))))))
                .then(CommandManager.literal("unload")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(CommandManager.argument("world", IdentifierArgumentType.identifier())
                                .suggests(new WorldSuggestionProvider(this::isRuntimeWorld))
                                .executes(this::unload)))
                .then(CommandManager.literal("load")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                                .suggests(new PersistentWorldSuggestionProvider())
                                .executes(this::loadPersistentWorld)));
    }

    private boolean isRuntimeWorld(ServerWorld world) {
        MinecraftServer server = world.getServer();
        WorldManager worldManager = KibuWorlds.getInstance().getWorldManager(server);

        return worldManager.getRuntimeWorldHandle(world).isPresent();
    }

    private int createTemporaryWorld(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        MinecraftServer server = ctx.getSource().getServer();

        return createRuntimeWorld(ctx, null, worldConfig -> Fantasy.get(server).openTemporaryWorld(worldConfig));
    }

    private int createTemporaryWorldSeed(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        String seed = StringArgumentType.getString(ctx, "seed");
        MinecraftServer server = ctx.getSource().getServer();

        return createRuntimeWorld(ctx, seed, worldConfig -> Fantasy.get(server).openTemporaryWorld(worldConfig));
    }

    private int createPersistentWorld(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Identifier id = IdentifierArgumentType.getIdentifier(ctx, "id");
        MinecraftServer server = ctx.getSource().getServer();

        validateIdentifier(ctx, id);

        return createRuntimeWorld(ctx, null, worldConfig -> Fantasy.get(server).getOrOpenPersistentWorld(id, worldConfig));
    }

    private int createPersistentWorldSeed(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Identifier id = IdentifierArgumentType.getIdentifier(ctx, "id");
        String seed = StringArgumentType.getString(ctx, "seed");
        MinecraftServer server = ctx.getSource().getServer();

        validateIdentifier(ctx, id);

        return createRuntimeWorld(ctx, seed, worldConfig -> Fantasy.get(server).getOrOpenPersistentWorld(id, worldConfig));
    }

    private int loadPersistentWorld(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        Identifier id = IdentifierArgumentType.getIdentifier(ctx, "id");

        validateIdentifier(ctx, id);

        ServerCommandSource source = ctx.getSource();
        MinecraftServer server = source.getServer();
        WorldManager worldManager = KibuWorlds.getInstance().getWorldManager(server);

        var optHandle = worldManager.openPersistentWorld(id);

        if (optHandle.isEmpty()) {
            throw commandService.createPersistedWorldFailedToLoadException(source, id);
        }

        sendCreationSuccess(id, source);

        return 1;
    }

    private void validateIdentifier(CommandContext<ServerCommandSource> ctx, Identifier id) throws CommandSyntaxException {
        String namespace = id.getNamespace();

        if (!PalPlugin.ID.equals(namespace)) return;

        throw commandService.createReservedWorldIdException(ctx.getSource(), id);
    }

    private int createRuntimeWorld(CommandContext<ServerCommandSource> ctx, @Nullable String seed, Function<RuntimeWorldConfig, RuntimeWorldHandle> factory) throws CommandSyntaxException {
        Identifier identifier = IdentifierArgumentType.getIdentifier(ctx, "type");

        MinecraftServer server = ctx.getSource().getServer();
        WorldType worldType = PalWorldTypes.getInstance().getWorldType(server, identifier);

        if (worldType == null) {
            throw commandService.createUnknownWorldTypeException(ctx.getSource(), identifier);
        }

        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig();

        worldType.configure(() -> server, worldConfig);

        if (seed != null) {
            GeneratorOptions.parseSeed(seed).ifPresent(worldConfig::setSeed);
        } else {
            worldConfig.setSeed(random.nextLong());
        }

        RuntimeWorldHandle handle = factory.apply(worldConfig);

        Identifier worldId = handle.getRegistryKey().getValue();
        ServerCommandSource source = ctx.getSource();

        sendCreationSuccess(worldId, source);

        return 1;
    }

    private void sendCreationSuccess(Identifier worldId, ServerCommandSource source) {
        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/world tp %s".formatted(worldId));
        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, commandService.translateText(source, "pal.cmd.world.create.success.tp_hover")
                .formatted(Formatting.GREEN));

        source.sendMessage(commandService.translateText(source, "pal.cmd.world.create.success",
                        FormatWrapper.styled(worldId, Formatting.YELLOW),
                        commandService.translateText(source, "pal.cmd.world.create.success.tp")
                                .styled(style -> style.withClickEvent(clickEvent)
                                        .withHoverEvent(hoverEvent))
                                .formatted(Formatting.AQUA))
                .formatted(Formatting.GREEN));
    }

    private int unload(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerWorld world = WorldSuggestionProvider.getWorld(context, "world", commandService);

        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();

        WorldManager worldManager = KibuWorlds.getInstance().getWorldManager(server);
        var optHandle = worldManager.getRuntimeWorldHandle(world);

        if (optHandle.isEmpty()) {
            throw commandService.createNotUnloadableWorldException(source);
        }

        optHandle.get().unload();

        Identifier id = world.getRegistryKey().getValue();

        source.sendMessage(commandService.translateText(source, "pal.cmd.world.unload.success",
                FormatWrapper.styled(id, Formatting.YELLOW)).formatted(Formatting.GREEN));

        return 1;
    }
}
