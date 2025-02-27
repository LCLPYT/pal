package work.lclpnet.pal.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.EntityView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.kibu.access.entity.MarkerEntityAccess;
import work.lclpnet.kibu.inv.prompt.OptionPrompt;
import work.lclpnet.kibu.inv.prompt.TextPrompt;
import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.kibu.translate.text.TextTranslatable;
import work.lclpnet.kibu.translate.util.LocaleUtil;

import javax.inject.Inject;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;
import java.util.OptionalDouble;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static net.minecraft.util.Formatting.*;
import static work.lclpnet.kibu.translate.text.FormatWrapper.styled;

public class MarkerConfigurator {

    public static final String PAL_MARKER_KEY = "pal:marker";

    private final Translations translations;

    @Inject
    public MarkerConfigurator(Translations translations) {
        this.translations = translations;
    }

    public void editBoosterPlate(ServerPlayerEntity player, BlockPos pos) {
        enum Option { HORIZONTAL, VERTICAL }

        var title = translations.translateText(player, "pal.edit_booster_plate.title");

        OptionPrompt.open(player, title, Arrays.asList(Option.values()), option -> switch (option) {
            case HORIZONTAL -> {
                var stack = new ItemStack(Items.BLAZE_POWDER);
                stack.set(DataComponentTypes.ITEM_NAME, translations.translateText(player, "pal.edit_booster_plate.horizontal").formatted(AQUA));
                yield stack;
            }
            case VERTICAL -> {
                var stack = new ItemStack(Items.FEATHER);
                stack.set(DataComponentTypes.ITEM_NAME, translations.translateText(player, "pal.edit_booster_plate.vertical").formatted(AQUA));
                yield stack;
            }
        }).thenAccept(o -> o.ifPresent(opt -> editStrength(player, pos, switch (opt) {
            case HORIZONTAL -> Property.HORIZONTAL_STRENGTH;
            case VERTICAL -> Property.VERTICAL_STRENGTH;
        })));
    }

    public void editStrength(ServerPlayerEntity player, BlockPos pos, Property property) {
        double current = getStrength(player.getServerWorld(), pos, property);

        String initial = decimalFormat(translations.getLocale(player)).format(current);

        var title = translations.translateText(player, "pal.edit_%s.title".formatted(property.id()));

        TextPrompt.open(player, title, initial, input -> unsignedDouble(input).isPresent())
                .thenAccept(val -> unsignedDouble(val.orElse(initial))
                        .ifPresent(strength -> {
                            if (abs(current - strength) > 1e-3) {
                                modifyStrength(player, pos, property, strength);
                            }
                        }));
    }

    private void modifyStrength(ServerPlayerEntity player, BlockPos pos, Property property, double strength) {
        setStrength(player.getServerWorld(), pos, property, strength);

        player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.MASTER, 0.5f, 2f);

        var localizedStrength = new TextTranslatable() {
            @Override
            public Text translateTo(String language) {
                Locale locale = LocaleUtil.getLocale(language);
                var df = decimalFormat(locale);

                return Text.literal(df.format(strength));
            }
        };

        translations.translateText("pal.edit_%s.changed".formatted(property.id()), styled(localizedStrength, YELLOW))
                .formatted(GREEN)
                .sendTo(player);
    }

    private static @NotNull DecimalFormat decimalFormat(Locale locale) {
        var df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(locale));
        df.setMaximumFractionDigits(6);
        df.setMinimumFractionDigits(1);
        return df;
    }

    public @Nullable NbtCompound getMarkerData(EntityView world, BlockPos pos) {
        var markers = world.getEntitiesByClass(MarkerEntity.class, new Box(pos), marker -> true);

        for (MarkerEntity marker : markers) {
            NbtCompound data = MarkerEntityAccess.getData(marker);

            if (isPalMarker(marker)) {
                return data.getCompound(PAL_MARKER_KEY);
            }
        }

        return null;
    }

    public double getStrength(EntityView world, BlockPos pos, Property property) {
        NbtCompound markerData = getMarkerData(world, pos);

        return getStrength(markerData, property);
    }

    public double getStrength(@Nullable NbtCompound markerData, Property property) {
        if (markerData != null && markerData.contains(property.id(), NbtElement.DOUBLE_TYPE)) {
            return max(0.0, markerData.getDouble(property.id()));
        }

        return 1.0;
    }

    public void setStrength(ServerWorld world, BlockPos pos, Property property, double strength) {
        strength = max(0.0, strength);

        var markers = world.getEntitiesByClass(MarkerEntity.class, new Box(pos), marker -> true);

        NbtCompound markerData = null;

        for (MarkerEntity marker : markers) {
            NbtCompound data = MarkerEntityAccess.getData(marker);

            if (!isPalMarker(marker)) continue;

            if (markerData != null) {
                marker.discard();
                continue;
            }

            markerData = data.getCompound(PAL_MARKER_KEY);
        }

        if (markerData == null) {
            var marker = new MarkerEntity(EntityType.MARKER, world);
            marker.setPosition(pos.toCenterPos());

            NbtCompound data = MarkerEntityAccess.getData(marker);
            markerData = new NbtCompound();
            data.put(PAL_MARKER_KEY, markerData);

            world.spawnEntity(marker);
        }

        markerData.putDouble(property.id(), strength);
    }

    private static OptionalDouble unsignedDouble(String input) {
        try {
            double d = Double.parseDouble(input.trim().replace(',', '.'));

            return d >= 0 ? OptionalDouble.of(d) : OptionalDouble.empty();
        } catch (NumberFormatException ignored) {
            return OptionalDouble.empty();
        }
    }

    public boolean isPalMarker(Entity entity) {
        if (!(entity instanceof MarkerEntity marker)) return false;

        NbtCompound data = MarkerEntityAccess.getData(marker);

        return data.contains(PAL_MARKER_KEY, NbtElement.COMPOUND_TYPE);
    }

    public enum Property {
        STRENGTH,
        HORIZONTAL_STRENGTH,
        VERTICAL_STRENGTH;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
