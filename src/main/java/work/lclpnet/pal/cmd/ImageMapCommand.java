package work.lclpnet.pal.cmd;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import work.lclpnet.kibu.map.MapColorUtil;
import work.lclpnet.kibu.map.MapUtil;
import work.lclpnet.kibu.map.mixin.MapStateAccessor;
import work.lclpnet.kibu.plugin.cmd.CommandRegistrar;
import work.lclpnet.kibu.plugin.cmd.KibuCommand;
import work.lclpnet.kibu.translate.TranslationService;
import work.lclpnet.pal.cmd.arg.ImageSuggestionProvider;
import work.lclpnet.pal.service.CommandService;
import work.lclpnet.pal.util.ImageManager;
import work.lclpnet.pal.util.ImageMode;
import work.lclpnet.pal.util.MapSizes;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.util.Formatting.RED;

public class ImageMapCommand implements KibuCommand {

    private final ImageSuggestionProvider imageSuggestions;
    private final ImageManager imageManager;
    private final CommandService commandService;

    @Inject
    public ImageMapCommand(ImageSuggestionProvider imageSuggestions, ImageManager imageManager, CommandService commandService) {
        this.imageSuggestions = imageSuggestions;
        this.imageManager = imageManager;
        this.commandService = commandService;
    }

    @Override
    public void register(CommandRegistrar registrar) {
        registrar.registerCommand(literal("imagemap")
                .requires(s -> s.hasPermissionLevel(2))
                .then(argument("image", StringArgumentType.string())
                        .suggests(imageSuggestions)
                        .executes(this::doSimple)
                        .then(argument("sizes", StringArgumentType.word())
                                .suggests(this::suggestSizes)
                                .executes(ctx -> doSizes(ctx, ImageMode.MATCH))
                                .then(literal("match")
                                        .executes(ctx -> doSizes(ctx, ImageMode.MATCH)))
                                .then(literal("crop")
                                        .executes(ctx -> doSizes(ctx, ImageMode.CROP))))));
    }

    private int doSimple(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
        String name = StringArgumentType.getString(ctx, "image");

        loadImageAndDo(player, name, image -> giveMaps(image, player, new MapSizes(1, 1), ImageMode.MATCH));

        return 1;
    }

    private int doSizes(CommandContext<ServerCommandSource> ctx, ImageMode mode) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "image");
        MapSizes sizes = parseSizes(source, StringArgumentType.getString(ctx, "sizes"));

        ServerPlayerEntity player = source.getPlayerOrThrow();

        loadImageAndDo(player, name, image -> giveMaps(image, player, sizes, mode));

        return 1;
    }

    private void loadImageAndDo(ServerPlayerEntity player, String name, Consumer<BufferedImage> action) {
        TranslationService translationService = commandService.getTranslationService();

        imageManager.getSource(name)
                .thenApply(s -> {
                    player.sendMessage(translationService.translateText(player, "pal.cmd.imagemap.processing"));
                    return s;
                })
                .thenCompose(imageManager::loadImage)
                .exceptionally(error -> {
                    player.sendMessage(translationService.translateText(player, "pal.cmd.imagemap.not_found")
                            .formatted(RED));
                    return null;
                })
                .thenAccept(image -> {
                    if (image != null) {
                        action.accept(image);
                    }
                })
                .exceptionally(error -> {
                    player.sendMessage(translationService.translateText(player, "pal.cmd.imagemap.error")
                            .formatted(RED));
                    return null;
                });
    }

    private void giveMaps(BufferedImage image, ServerPlayerEntity player, MapSizes sizes, ImageMode mode) {
        ServerWorld world = player.getServerWorld();
        BufferedImage[] parts = ImageManager.getParts(image, sizes, mode);

        for (BufferedImage part : parts) {
            ItemStack stack = new ItemStack(Items.FILLED_MAP);

            int id = MapUtil.allocateMapId(world, 0, 0, 0, false, false, world.getRegistryKey());
            String name = FilledMapItem.getMapName(id);

            MapState mapState = world.getMapState(name);
            if (mapState == null) throw new IllegalStateException();

            ((MapStateAccessor) mapState).setLocked(true);

            byte[] pixels = MapColorUtil.toBytes(part);
            System.arraycopy(pixels, 0, mapState.colors, 0, Math.min(pixels.length, mapState.colors.length));

            MapUtil.setMapId(stack, id);

            player.giveItemStack(stack);
        }
    }

    private CompletableFuture<Suggestions> suggestSizes(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        return builder.suggest("1x1").suggest("2x2").suggest("2x1").buildFuture();
    }

    private MapSizes parseSizes(ServerCommandSource source, String src) throws CommandSyntaxException {
        String[] parts = src.split("x");

        if (parts.length != 2) throw commandService.createInvalidMapSizesException(source);

        int width, height;

        try {
            width = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            throw commandService.createInvalidIntException(source, parts[0]);
        }

        try {
            height = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw commandService.createInvalidIntException(source, parts[1]);
        }

        return new MapSizes(width, height);
    }
}
