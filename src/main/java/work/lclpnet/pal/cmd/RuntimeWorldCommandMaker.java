package work.lclpnet.pal.cmd;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.WorldOptions;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.kibu.translate.text.FormatWrapper;
import work.lclpnet.kibu.world.KibuWorlds;
import work.lclpnet.kibu.world.WorldManager;
import work.lclpnet.pal.PalMod;
import work.lclpnet.pal.cmd.arg.PersistentWorldSuggestionProvider;
import work.lclpnet.pal.cmd.arg.WorldSuggestionProvider;
import work.lclpnet.pal.cmd.arg.WorldTypeSuggestionProvider;
import work.lclpnet.pal.service.CommandService;
import work.lclpnet.pal.world.PalWorldTypes;
import work.lclpnet.pal.world.WorldType;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.Random;
import java.util.function.Function;

public class RuntimeWorldCommandMaker {

    private final CommandService commandService;
    private final Random random = new Random();

    public RuntimeWorldCommandMaker(CommandService commandService) {
        this.commandService = commandService;
    }

    public void inject(LiteralArgumentBuilder<CommandSourceStack> node) {
        var worldTypeProvider = new WorldTypeSuggestionProvider();

        node.then(Commands.literal("create")
                        .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                        .then(Commands.literal("temporary")
                                .then(Commands.argument("type", IdentifierArgument.id())
                                        .suggests(worldTypeProvider)
                                        .executes(this::createTemporaryWorld)
                                        .then(Commands.argument("seed", StringArgumentType.greedyString())
                                                .executes(this::createTemporaryWorldSeed))))
                        .then(Commands.literal("persistent")
                                .then(Commands.argument("id", IdentifierArgument.id())
                                        .then(Commands.argument("type", IdentifierArgument.id())
                                                .suggests(worldTypeProvider)
                                                .executes(this::createPersistentWorld)
                                                .then(Commands.argument("seed", StringArgumentType.greedyString())
                                                        .executes(this::createPersistentWorldSeed))))))
                .then(Commands.literal("unload")
                        .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                        .then(Commands.argument("world", IdentifierArgument.id())
                                .suggests(new WorldSuggestionProvider(this::isRuntimeWorld))
                                .executes(this::unload)))
                .then(Commands.literal("load")
                        .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                        .then(Commands.argument("id", IdentifierArgument.id())
                                .suggests(new PersistentWorldSuggestionProvider())
                                .executes(this::loadPersistentWorld)));
    }

    private boolean isRuntimeWorld(ServerLevel world) {
        MinecraftServer server = world.getServer();
        WorldManager worldManager = KibuWorlds.getInstance().getWorldManager(server);

        return worldManager.getRuntimeWorldHandle(world).isPresent();
    }

    private int createTemporaryWorld(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        MinecraftServer server = ctx.getSource().getServer();

        return createRuntimeWorld(ctx, null, worldConfig -> Fantasy.get(server).openTemporaryWorld(worldConfig));
    }

    private int createTemporaryWorldSeed(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String seed = StringArgumentType.getString(ctx, "seed");
        MinecraftServer server = ctx.getSource().getServer();

        return createRuntimeWorld(ctx, seed, worldConfig -> Fantasy.get(server).openTemporaryWorld(worldConfig));
    }

    private int createPersistentWorld(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Identifier id = IdentifierArgument.getId(ctx, "id");
        MinecraftServer server = ctx.getSource().getServer();

        validateIdentifier(ctx, id);

        return createRuntimeWorld(ctx, null, worldConfig -> Fantasy.get(server).getOrOpenPersistentWorld(id, worldConfig));
    }

    private int createPersistentWorldSeed(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Identifier id = IdentifierArgument.getId(ctx, "id");
        String seed = StringArgumentType.getString(ctx, "seed");
        MinecraftServer server = ctx.getSource().getServer();

        validateIdentifier(ctx, id);

        return createRuntimeWorld(ctx, seed, worldConfig -> Fantasy.get(server).getOrOpenPersistentWorld(id, worldConfig));
    }

    private int loadPersistentWorld(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Identifier id = IdentifierArgument.getId(ctx, "id");

        validateIdentifier(ctx, id);

        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        WorldManager worldManager = KibuWorlds.getInstance().getWorldManager(server);

        var optHandle = worldManager.openPersistentWorld(id);

        if (optHandle.isEmpty()) {
            throw commandService.createPersistedWorldFailedToLoadException(source, id);
        }

        sendCreationSuccess(id, source);

        return 1;
    }

    private void validateIdentifier(CommandContext<CommandSourceStack> ctx, Identifier id) throws CommandSyntaxException {
        String namespace = id.getNamespace();

        if (!PalMod.ID.equals(namespace)) return;

        throw commandService.createReservedWorldIdException(ctx.getSource(), id);
    }

    private int createRuntimeWorld(CommandContext<CommandSourceStack> ctx, @Nullable String seed, Function<RuntimeWorldConfig, RuntimeWorldHandle> factory) throws CommandSyntaxException {
        Identifier identifier = IdentifierArgument.getId(ctx, "type");

        MinecraftServer server = ctx.getSource().getServer();
        WorldType worldType = PalWorldTypes.getInstance().getWorldType(server, identifier);

        if (worldType == null) {
            throw commandService.createUnknownWorldTypeException(ctx.getSource(), identifier);
        }

        RuntimeWorldConfig worldConfig = new RuntimeWorldConfig();

        worldType.configure(() -> server, worldConfig);

        if (seed != null) {
            WorldOptions.parseSeed(seed).ifPresent(worldConfig::setSeed);
        } else {
            worldConfig.setSeed(random.nextLong());
        }

        RuntimeWorldHandle handle = factory.apply(worldConfig);

        Identifier worldId = handle.getRegistryKey().identifier();
        CommandSourceStack source = ctx.getSource();

        sendCreationSuccess(worldId, source);

        return 1;
    }

    private void sendCreationSuccess(Identifier worldId, CommandSourceStack source) {
        ClickEvent clickEvent = new ClickEvent.RunCommand("/world tp %s".formatted(worldId));
        HoverEvent hoverEvent = new HoverEvent.ShowText(commandService.translateText(source, "pal.cmd.world.create.success.tp_hover")
                .formatted(ChatFormatting.GREEN));

        source.sendSystemMessage(commandService.translateText(source, "pal.cmd.world.create.success",
                        FormatWrapper.styled(worldId, ChatFormatting.YELLOW),
                        commandService.translateText(source, "pal.cmd.world.create.success.tp")
                                .styled(style -> style.withClickEvent(clickEvent)
                                        .withHoverEvent(hoverEvent))
                                .formatted(ChatFormatting.AQUA))
                .formatted(ChatFormatting.GREEN));
    }

    private int unload(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerLevel world = WorldSuggestionProvider.getWorld(context, "world", commandService);

        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();

        WorldManager worldManager = KibuWorlds.getInstance().getWorldManager(server);
        var optHandle = worldManager.getRuntimeWorldHandle(world);

        if (optHandle.isEmpty()) {
            throw commandService.createNotUnloadableWorldException(source);
        }

        optHandle.get().unload();

        Identifier id = world.dimension().identifier();

        source.sendSystemMessage(commandService.translateText(source, "pal.cmd.world.unload.success",
                FormatWrapper.styled(id, ChatFormatting.YELLOW)).formatted(ChatFormatting.GREEN));

        return 1;
    }
}
