package work.lclpnet.pal.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.kibu.access.entity.ServerPlayerAccess;
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
import static net.minecraft.ChatFormatting.*;
import static work.lclpnet.kibu.translate.text.FormatWrapper.styled;

public class MarkerConfigurator {

    public static final String PAL_MARKER_KEY = "pal:marker";
    public static final MapCodec<Data> PAL_MARKER_CODEC = Data.CODEC.fieldOf(PAL_MARKER_KEY);

    private final Translations translations;

    @Inject
    public MarkerConfigurator(Translations translations) {
        this.translations = translations;
    }

    public void editBoosterPlate(ServerPlayer player, BlockPos pos) {
        enum Option { HORIZONTAL, VERTICAL }

        var title = translations.translateText(player, "pal.edit_booster_plate.title");

        OptionPrompt.open(player, title, Arrays.asList(Option.values()), option -> switch (option) {
            case HORIZONTAL -> {
                var stack = new ItemStack(Items.BLAZE_POWDER);
                stack.set(DataComponents.ITEM_NAME, translations.translateText(player, "pal.edit_booster_plate.horizontal").withStyle(AQUA));
                yield stack;
            }
            case VERTICAL -> {
                var stack = new ItemStack(Items.FEATHER);
                stack.set(DataComponents.ITEM_NAME, translations.translateText(player, "pal.edit_booster_plate.vertical").withStyle(AQUA));
                yield stack;
            }
        }).thenAccept(o -> o.ifPresent(opt -> editStrength(player, pos, switch (opt) {
            case HORIZONTAL -> Property.HORIZONTAL_STRENGTH;
            case VERTICAL -> Property.VERTICAL_STRENGTH;
        })));
    }

    public void editStrength(ServerPlayer player, BlockPos pos, Property property) {
        editStrength(player, pos, property, () -> getStrength(player.level(), pos, property));
    }
    public void editStrength(ServerPlayer player, BlockPos pos, Property property, DoubleSupplier getter) {
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

    public void editElevator(ServerPlayer player, BlockPos pos) {
        enum Option { STRENGTH, DURATION }

        var title = translations.translateText(player, "pal.edit_elevator.title");

        OptionPrompt.open(player, title, Arrays.asList(Option.values()), option -> switch (option) {
            case STRENGTH -> {
                var stack = new ItemStack(Items.GLOWSTONE_DUST);
                stack.set(DataComponents.ITEM_NAME, translations.translateText(player, "pal.edit_elevator.strength").withStyle(AQUA));
                yield stack;
            }
            case DURATION -> {
                var stack = new ItemStack(Items.REDSTONE);
                stack.set(DataComponents.ITEM_NAME, translations.translateText(player, "pal.edit_elevator.duration").withStyle(AQUA));
                yield stack;
            }
        }).thenAccept(o -> o.ifPresent(opt -> {
            Property property = switch (opt) {
                case STRENGTH -> Property.STRENGTH;
                case DURATION -> Property.DURATION;
            };

            editStrength(player, pos, property, () -> switch (opt) {
                case DURATION -> 10;
                case STRENGTH -> getStrength(player.level(), pos, property);
            });
        }));
    }

    private void modifyStrength(ServerPlayer player, BlockPos pos, Property property, double strength) {
        setStrength(player.level(), pos, property, strength);

        ServerPlayerAccess.playSoundToPlayer(player, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, 0.5f, 2f);

        var localizedStrength = new TextTranslatable() {
            @Override
            public Component translateTo(String language) {
                Locale locale = LocaleUtil.getLocale(language);
                var df = decimalFormat(locale);

                return Component.literal(df.format(strength));
            }
        };

        translations.translateText("pal.edit_%s.changed".formatted(property.id()), styled(localizedStrength, YELLOW))
                .withStyle(GREEN)
                .sendTo(player);
    }

    private static @NotNull DecimalFormat decimalFormat(Locale locale) {
        var df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(locale));
        df.setMaximumFractionDigits(6);
        df.setMinimumFractionDigits(1);
        return df;
    }

    public @Nullable Data getMarkerData(EntityGetter world, BlockPos pos) {
        var markers = world.getEntitiesOfClass(Marker.class, new AABB(pos), marker -> true);

        for (Marker marker : markers) {
            Data data = getData(marker);

            if (data != null) {
                return data;
            }
        }

        return null;
    }

    private @Nullable Data getData(Marker marker) {
        CustomData customData = marker.get(DataComponents.CUSTOM_DATA);

        if (customData == null) return null;

        return PAL_MARKER_CODEC.codec().decode(NbtOps.INSTANCE, customData.copyTag())
                .resultOrPartial()
                .map(Pair::getFirst)
                .orElse(null);
    }

    private void setData(Marker marker, Data data) {
        CustomData customData = marker.get(DataComponents.CUSTOM_DATA);

        if (customData == null) return;

        PAL_MARKER_CODEC.codec().encode(data, NbtOps.INSTANCE, customData.copyTag())
                .resultOrPartial()
                .filter(nbt -> nbt instanceof CompoundTag)
                .ifPresent(nbt -> marker.setComponent(DataComponents.CUSTOM_DATA, CustomData.of((CompoundTag) nbt)));
    }

    public double getStrength(EntityGetter world, BlockPos pos, Property property) {
        Data markerData = getMarkerData(world, pos);

        return getStrength(markerData, property);
    }

    public double getStrength(@Nullable Data markerData, Property property) {
        if (markerData == null) {
            return 1.0;
        }

        return max(0.0, markerData.value(property).orElse(1.0));
    }

    public void setStrength(ServerLevel world, BlockPos pos, Property property, double strength) {
        strength = max(0.0, strength);

        var markers = world.getEntitiesOfClass(Marker.class, new AABB(pos), marker -> true);

        Marker markerEntity = null;
        Data markerData = Data.DEFAULT;

        for (Marker marker : markers) {
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
        markerEntity = new Marker(EntityTypes.MARKER, world);
        markerEntity.setPos(Vec3.atCenterOf(pos));

        setData(markerEntity, markerData);

        world.addFreshEntity(markerEntity);
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
        if (!(entity instanceof Marker marker)) return false;

        CustomData customData = marker.get(DataComponents.CUSTOM_DATA);

        return customData != null && customData.copyTag().contains(PAL_MARKER_KEY);
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
