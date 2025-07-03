package work.lclpnet.pal.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtOps;
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
import java.util.Optional;
import java.util.OptionalDouble;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.util.Optional.empty;
import static net.minecraft.util.Formatting.*;
import static work.lclpnet.kibu.translate.text.FormatWrapper.styled;

public class MarkerConfigurator {

    public static final String PAL_MARKER_KEY = "pal:marker";
    public static final MapCodec<Data> PAL_MARKER_CODEC = Data.CODEC.fieldOf(PAL_MARKER_KEY);

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
        double current = getStrength(player.getWorld(), pos, property);

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
        setStrength(player.getWorld(), pos, property, strength);

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

    public @Nullable Data getMarkerData(EntityView world, BlockPos pos) {
        var markers = world.getEntitiesByClass(MarkerEntity.class, new Box(pos), marker -> true);

        for (MarkerEntity marker : markers) {
            Data data = getData(marker);

            if (data != null) {
                return data;
            }
        }

        return null;
    }

    private @Nullable Data getData(MarkerEntity marker) {
        NbtComponent customData = marker.get(DataComponentTypes.CUSTOM_DATA);

        if (customData == null) return null;

        return customData.get(PAL_MARKER_CODEC).result().orElse(null);
    }

    private void setData(MarkerEntity marker, Data data) {
        NbtComponent customData = marker.get(DataComponentTypes.CUSTOM_DATA);

        if (customData == null) return;

        customData.with(NbtOps.INSTANCE, PAL_MARKER_CODEC, data)
                .ifSuccess(component -> marker.setComponent(DataComponentTypes.CUSTOM_DATA, component));

    }

    public double getStrength(EntityView world, BlockPos pos, Property property) {
        Data markerData = getMarkerData(world, pos);

        return getStrength(markerData, property);
    }

    public double getStrength(@Nullable Data markerData, Property property) {
        if (markerData == null) {
            return 1.0;
        }

        return max(0.0, markerData.strength(property).orElse(1.0));
    }

    public void setStrength(ServerWorld world, BlockPos pos, Property property, double strength) {
        strength = max(0.0, strength);

        var markers = world.getEntitiesByClass(MarkerEntity.class, new Box(pos), marker -> true);

        MarkerEntity markerEntity = null;
        Data markerData = Data.DEFAULT;

        for (MarkerEntity marker : markers) {
            Data data = getData(marker);

            if (data == null) continue;

            // eliminate duplicate markers
            if (markerEntity != null) {
                marker.discard();
                continue;
            }

            markerEntity = marker;
            markerData = data;
        }

        markerData = markerData.with(property, strength);

        if (markerEntity != null) {
            setData(markerEntity, markerData);
            return;
        }

        // create new marker if none exists
        markerEntity = new MarkerEntity(EntityType.MARKER, world);
        markerEntity.setPosition(pos.toCenterPos());

        setData(markerEntity, markerData);

        world.spawnEntity(markerEntity);
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

        NbtComponent customData = marker.get(DataComponentTypes.CUSTOM_DATA);

        return customData != null && customData.contains(PAL_MARKER_KEY);
    }

    public record Data(Optional<Double> strength, Optional<Double> horizontal, Optional<Double> vertical) {

        public static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                PalCodecs.POSITIVE_DOUBLE.optionalFieldOf(Property.STRENGTH.id()).forGetter(Data::strength),
                PalCodecs.POSITIVE_DOUBLE.optionalFieldOf(Property.HORIZONTAL_STRENGTH.id()).forGetter(Data::horizontal),
                PalCodecs.POSITIVE_DOUBLE.optionalFieldOf(Property.VERTICAL_STRENGTH.id()).forGetter(Data::vertical)
        ).apply(instance, Data::new));

        public static final Data DEFAULT = new Data(empty(), empty(), empty());

        public Optional<Double> strength(Property property) {
            return switch (property) {
                case STRENGTH -> strength;
                case HORIZONTAL_STRENGTH -> horizontal;
                case VERTICAL_STRENGTH -> vertical;
            };
        }

        public Data with(Property property, double value) {
            return switch (property) {
                case STRENGTH -> new Data(Optional.of(value), horizontal, vertical);
                case HORIZONTAL_STRENGTH -> new Data(strength, Optional.of(value), vertical);
                case VERTICAL_STRENGTH -> new Data(strength, horizontal, Optional.of(value));
            };
        }
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
