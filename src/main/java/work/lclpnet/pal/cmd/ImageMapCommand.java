package work.lclpnet.pal.cmd;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import work.lclpnet.kibu.cmd.type.CommandRegistrar;
import work.lclpnet.kibu.cmd.type.KibuCommand;
import work.lclpnet.kibu.map.MapColorUtil;
import work.lclpnet.kibu.map.MapUtil;
import work.lclpnet.kibu.map.mixin.MapItemSavedDataAccessor;
import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.pal.cmd.arg.ImageSuggestionProvider;
import work.lclpnet.pal.service.CommandService;
import work.lclpnet.pal.util.ImageManager;
import work.lclpnet.pal.util.ImageMode;
import work.lclpnet.pal.util.MapSizes;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static net.minecraft.ChatFormatting.RED;
import static net.minecraft.ChatFormatting.YELLOW;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static work.lclpnet.kibu.translate.text.FormatWrapper.styled;

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
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
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

    private int doSimple(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "image");

        loadImageAndDo(player, name, image -> giveMaps(image, player, new MapSizes(1, 1), ImageMode.MATCH));

        return 1;
    }

    private int doSizes(CommandContext<CommandSourceStack> ctx, ImageMode mode) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "image");
        MapSizes sizes = parseSizes(source, StringArgumentType.getString(ctx, "sizes"));

        ServerPlayer player = source.getPlayerOrException();

        loadImageAndDo(player, name, image -> giveMaps(image, player, sizes, mode));

        return 1;
    }

    private void loadImageAndDo(ServerPlayer player, String name, Consumer<BufferedImage> action) {
        Translations translations = commandService.getTranslations();

        imageManager.getSource(name)
                .thenApply(s -> {
                    player.sendSystemMessage(translations.translateText(player, "pal.cmd.imagemap.processing"));
                    return s;
                })
                .thenCompose(imageManager::loadImage)
                .exceptionally(error -> {
                    player.sendSystemMessage(translations.translateText(player, "pal.cmd.imagemap.not_found", styled(name, YELLOW))
                            .formatted(RED));
                    return null;
                })
                .thenAccept(image -> {
                    if (image != null) {
                        action.accept(image);
                    }
                })
                .exceptionally(error -> {
                    player.sendSystemMessage(translations.translateText(player, "pal.cmd.imagemap.error")
                            .formatted(RED));
                    return null;
                });
    }

    private void giveMaps(BufferedImage image, ServerPlayer player, MapSizes sizes, ImageMode mode) {
        ServerLevel world = player.level();
        BufferedImage[] parts = ImageManager.getParts(image, sizes, mode);

        for (BufferedImage part : parts) {
            ItemStack stack = new ItemStack(Items.FILLED_MAP);

            MapId id = MapUtil.allocateMapId(world, 0, 0, 0, false, false, world.dimension());
            MapItemSavedData mapState = world.getMapData(id);

            if (mapState == null) throw new IllegalStateException();

            ((MapItemSavedDataAccessor) mapState).setLocked(true);

            byte[] pixels = MapColorUtil.toBytes(part);
            System.arraycopy(pixels, 0, mapState.colors, 0, Math.min(pixels.length, mapState.colors.length));

            stack.set(DataComponents.MAP_ID, id);

            player.addItem(stack);
        }
    }

    private CompletableFuture<Suggestions> suggestSizes(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return builder.suggest("1x1").suggest("2x2").suggest("2x1").buildFuture();
    }

    private MapSizes parseSizes(CommandSourceStack source, String src) throws CommandSyntaxException {
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
