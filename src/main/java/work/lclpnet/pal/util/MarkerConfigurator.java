package work.lclpnet.pal.util;

import com.mojang.datafixers.util.Pair;
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
import net.minecraft.nbt.NbtCompound;
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
import java.util.function.DoubleSupplier;

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
        editStrength(player, pos, property, () -> getStrength(player.getEntityWorld(), pos, property));
    }
    public void editStrength(ServerPlayerEntity player, BlockPos pos, Property property, DoubleSupplier getter) {
        double current = getter.getAsDouble();

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

    public void editElevator(ServerPlayerEntity player, BlockPos pos) {
        enum Option { STRENGTH, DURATION }

        var title = translations.translateText(player, "pal.edit_elevator.title");

        OptionPrompt.open(player, title, Arrays.asList(Option.values()), option -> switch (option) {
            case STRENGTH -> {
                var stack = new ItemStack(Items.GLOWSTONE_DUST);
                stack.set(DataComponentTypes.ITEM_NAME, translations.translateText(player, "pal.edit_elevator.strength").formatted(AQUA));
                yield stack;
            }
            case DURATION -> {
                var stack = new ItemStack(Items.REDSTONE);
                stack.set(DataComponentTypes.ITEM_NAME, translations.translateText(player, "pal.edit_elevator.duration").formatted(AQUA));
                yield stack;
            }
        }).thenAccept(o -> o.ifPresent(opt -> {
            Property property = switch (opt) {
                case STRENGTH -> Property.STRENGTH;
                case DURATION -> Property.DURATION;
            };

            editStrength(player, pos, property, () -> switch (opt) {
                case DURATION -> 10;
                case STRENGTH -> getStrength(player.getEntityWorld(), pos, property);
            });
        }));
    }

    private void modifyStrength(ServerPlayerEntity player, BlockPos pos, Property property, double strength) {
        setStrength(player.getEntityWorld(), pos, property, strength);

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

        return PAL_MARKER_CODEC.codec().decode(NbtOps.INSTANCE, customData.copyNbt())
                .resultOrPartial()
                .map(Pair::getFirst)
                .orElse(null);
    }

    private void setData(MarkerEntity marker, Data data) {
        NbtComponent customData = marker.get(DataComponentTypes.CUSTOM_DATA);

        if (customData == null) return;

        PAL_MARKER_CODEC.codec().encode(data, NbtOps.INSTANCE, customData.copyNbt())
                .resultOrPartial()
                .filter(nbt -> nbt instanceof NbtCompound)
                .ifPresent(nbt -> marker.setComponent(DataComponentTypes.CUSTOM_DATA, NbtComponent.of((NbtCompound) nbt)));
    }

    public double getStrength(EntityView world, BlockPos pos, Property property) {
        Data markerData = getMarkerData(world, pos);

        return getStrength(markerData, property);
    }

    public double getStrength(@Nullable Data markerData, Property property) {
        if (markerData == null) {
            return 1.0;
        }

        return max(0.0, markerData.value(property).orElse(1.0));
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

        return customData != null && customData.copyNbt().contains(PAL_MARKER_KEY);
    }

    public record Data(
            Optional<Double> strength,
            Optional<Double> horizontal,
            Optional<Double> vertical,
            Optional<Double> duration
    ) {
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                PalCodecs.POSITIVE_DOUBLE.optionalFieldOf(Property.STRENGTH.id()).forGetter(Data::strength),
                PalCodecs.POSITIVE_DOUBLE.optionalFieldOf(Property.HORIZONTAL_STRENGTH.id()).forGetter(Data::horizontal),
                PalCodecs.POSITIVE_DOUBLE.optionalFieldOf(Property.VERTICAL_STRENGTH.id()).forGetter(Data::vertical),
                PalCodecs.POSITIVE_DOUBLE.optionalFieldOf(Property.DURATION.id()).forGetter(Data::duration)
        ).apply(instance, Data::new));

        public static final Data DEFAULT = new Data(empty(), empty(), empty(), empty());

        public Optional<Double> value(Property property) {
            return switch (property) {
                case STRENGTH -> strength;
                case HORIZONTAL_STRENGTH -> horizontal;
                case VERTICAL_STRENGTH -> vertical;
                case DURATION -> duration;
            };
        }

        public Data with(Property property, double value) {
            return switch (property) {
                case STRENGTH -> new Data(Optional.of(value), horizontal, vertical, duration);
                case HORIZONTAL_STRENGTH -> new Data(strength, Optional.of(value), vertical, duration);
                case VERTICAL_STRENGTH -> new Data(strength, horizontal, Optional.of(value), duration);
                case DURATION -> new Data(strength, horizontal, vertical, Optional.of(value));
            };
        }
    }

    public enum Property {
        STRENGTH,
        HORIZONTAL_STRENGTH,
        VERTICAL_STRENGTH,
        DURATION;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
