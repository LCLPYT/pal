package work.lclpnet.pal.cmd;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.WorldOptions;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.kibu.translate.text.FormatWrapper;
import work.lclpnet.kibu.world.KibuLevels;
import work.lclpnet.kibu.world.WorldManager;
import work.lclpnet.pal.PalMod;
import work.lclpnet.pal.cmd.arg.PersistentLevelSuggestionProvider;
import work.lclpnet.pal.cmd.arg.LevelSuggestionProvider;
import work.lclpnet.pal.cmd.arg.LevelTypeSuggestionProvider;
import work.lclpnet.pal.service.CommandService;
import work.lclpnet.pal.level.PalLevelTypes;
import work.lclpnet.pal.level.LevelType;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.fantasy.RuntimeLevelHandle;

import java.util.Random;
import java.util.function.Function;

public class RuntimeWorldCommandMaker {

    private final CommandService commandService;
    private final Random random = new Random();

    public RuntimeWorldCommandMaker(CommandService commandService) {
        this.commandService = commandService;
    }

    public void inject(LiteralArgumentBuilder<CommandSourceStack> node) {
        var levelTypeProvider = new LevelTypeSuggestionProvider();

        node.then(Commands.literal("create")
                        .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                        .then(Commands.literal("temporary")
                                .then(Commands.argument("type", IdentifierArgument.id())
                                        .suggests(levelTypeProvider)
                                        .executes(this::createTemporaryLevel)
                                        .then(Commands.argument("seed", StringArgumentType.greedyString())
                                                .executes(this::createTemporaryLevelSeed))))
                        .then(Commands.literal("persistent")
                                .then(Commands.argument("id", IdentifierArgument.id())
                                        .then(Commands.argument("type", IdentifierArgument.id())
                                                .suggests(levelTypeProvider)
                                                .executes(this::createPersistentLevel)
                                                .then(Commands.argument("seed", StringArgumentType.greedyString())
                                                        .executes(this::createPersistentLevelSeed))))))
                .then(Commands.literal("unload")
                        .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                        .then(Commands.argument("world", IdentifierArgument.id())
                                .suggests(new LevelSuggestionProvider(this::isRuntimeLevel))
                                .executes(this::unload)))
                .then(Commands.literal("load")
                        .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                        .then(Commands.argument("id", IdentifierArgument.id())
                                .suggests(new PersistentLevelSuggestionProvider())
                                .executes(this::loadPersistentLevel)));
    }

    private boolean isRuntimeLevel(ServerLevel level) {
        MinecraftServer server = level.getServer();
        WorldManager worldManager = KibuLevels.getInstance().getWorldManager(server);

        return worldManager.getRuntimeLevelHandle(level).isPresent();
    }

    private int createTemporaryLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        MinecraftServer server = ctx.getSource().getServer();

        return createRuntimeLevel(ctx, null, levelConfig -> Fantasy.get(server).openTemporaryLevel(levelConfig));
    }

    private int createTemporaryLevelSeed(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String seed = StringArgumentType.getString(ctx, "seed");
        MinecraftServer server = ctx.getSource().getServer();

        return createRuntimeLevel(ctx, seed, levelConfig -> Fantasy.get(server).openTemporaryLevel(levelConfig));
    }

    private int createPersistentLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Identifier id = IdentifierArgument.getId(ctx, "id");
        MinecraftServer server = ctx.getSource().getServer();

        validateIdentifier(ctx, id);

        return createRuntimeLevel(ctx, null, levelConfig -> Fantasy.get(server).getOrOpenPersistentLevel(id, levelConfig));
    }

    private int createPersistentLevelSeed(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Identifier id = IdentifierArgument.getId(ctx, "id");
        String seed = StringArgumentType.getString(ctx, "seed");
        MinecraftServer server = ctx.getSource().getServer();

        validateIdentifier(ctx, id);

        return createRuntimeLevel(ctx, seed, levelConfig -> Fantasy.get(server).getOrOpenPersistentLevel(id, levelConfig));
    }

    private int loadPersistentLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Identifier id = IdentifierArgument.getId(ctx, "id");

        validateIdentifier(ctx, id);

        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        WorldManager worldManager = KibuLevels.getInstance().getWorldManager(server);

        var optHandle = worldManager.openPersistentLevel(id);

        if (optHandle.isEmpty()) {
            throw commandService.createPersistedLevelFailedToLoadException(source, id);
        }

        sendCreationSuccess(id, source);

        return 1;
    }

    private void validateIdentifier(CommandContext<CommandSourceStack> ctx, Identifier id) throws CommandSyntaxException {
        String namespace = id.getNamespace();

        if (!PalMod.ID.equals(namespace)) return;

        throw commandService.createReservedLevelIdException(ctx.getSource(), id);
    }

    private int createRuntimeLevel(CommandContext<CommandSourceStack> ctx, @Nullable String seed, Function<RuntimeLevelConfig, RuntimeLevelHandle> factory) throws CommandSyntaxException {
        Identifier identifier = IdentifierArgument.getId(ctx, "type");

        MinecraftServer server = ctx.getSource().getServer();
        LevelType levelType = PalLevelTypes.getInstance().getLevelType(server, identifier);

        if (levelType == null) {
            throw commandService.createUnknownLevelTypeException(ctx.getSource(), identifier);
        }

        RuntimeLevelConfig levelConfig = new RuntimeLevelConfig();

        levelType.configure(() -> server, levelConfig);

        if (seed != null) {
            WorldOptions.parseSeed(seed).ifPresent(levelConfig::setSeed);
        } else {
            levelConfig.setSeed(random.nextLong());
        }

        RuntimeLevelHandle handle = factory.apply(levelConfig);

        Identifier levelId = handle.getRegistryKey().identifier();
        CommandSourceStack source = ctx.getSource();

        sendCreationSuccess(levelId, source);

        return 1;
    }

    private void sendCreationSuccess(Identifier levelId, CommandSourceStack source) {
        ClickEvent clickEvent = new ClickEvent.RunCommand("/world tp %s".formatted(levelId));
        HoverEvent hoverEvent = new HoverEvent.ShowText(commandService.translateText(source, "pal.cmd.world.create.success.tp_hover")
                .withStyle(ChatFormatting.GREEN));

        source.sendSystemMessage(commandService.translateText(source, "pal.cmd.world.create.success",
                        FormatWrapper.styled(levelId, ChatFormatting.YELLOW),
                        commandService.translateText(source, "pal.cmd.world.create.success.tp")
                                .withStyle(style -> style.withClickEvent(clickEvent)
                                        .withHoverEvent(hoverEvent))
                                .withStyle(ChatFormatting.AQUA))
                .withStyle(ChatFormatting.GREEN));
    }

    private int unload(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerLevel level = LevelSuggestionProvider.getLevel(context, "world", commandService);

        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();

        WorldManager worldManager = KibuLevels.getInstance().getWorldManager(server);
        var optHandle = worldManager.getRuntimeLevelHandle(level);

        if (optHandle.isEmpty()) {
            throw commandService.createNotUnloadableLevelException(source);
        }

        optHandle.get().unload();

        Identifier id = level.dimension().identifier();

        source.sendSystemMessage(commandService.translateText(source, "pal.cmd.world.unload.success",
                FormatWrapper.styled(id, ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN));

        return 1;
    }
}
