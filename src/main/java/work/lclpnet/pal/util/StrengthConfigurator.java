package work.lclpnet.pal.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.EntityView;
import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.kibu.translate.text.LocalizedFormat;
import work.lclpnet.pal.mixin.MarkerEntityAccessor;

import javax.inject.Inject;
import java.util.OptionalDouble;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static net.minecraft.util.Formatting.GREEN;
import static net.minecraft.util.Formatting.YELLOW;
import static work.lclpnet.kibu.translate.text.FormatWrapper.styled;

public class StrengthConfigurator {

    public static final String STRENGTH_KEY = "pal:strength";

    private final Translations translations;

    @Inject
    public StrengthConfigurator(Translations translations) {
        this.translations = translations;
    }

    public void edit(ServerPlayerEntity player, BlockPos pos) {
        double current = getStrength(player.getServerWorld(), pos);

        String initial = String.format(translations.getLocale(player), "%f", current);

        var title = translations.translateText(player, "pal.edit_strength.title");

        new TextPrompt(title, initial, input -> unsignedDouble(input).isPresent())
                .open(player)
                .thenAccept(val -> unsignedDouble(val.orElse(initial))
                        .ifPresent(strength -> {
                            if (abs(current - strength) > 1e-3) {
                                modifyStrength(player, pos, strength);
                            }
                        }));
    }

    private void modifyStrength(ServerPlayerEntity player, BlockPos pos, double strength) {
        setStrength(player.getServerWorld(), pos, strength);

        player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.MASTER, 0.5f, 2f);

        translations.translateText("pal.edit_strength.changed", styled(LocalizedFormat.format("%.2f", strength), YELLOW))
                .formatted(GREEN)
                .sendTo(player);
    }

    public double getStrength(EntityView world, BlockPos pos) {
        var markers = world.getEntitiesByClass(MarkerEntity.class, new Box(pos), marker -> true);

        for (MarkerEntity marker : markers) {
            NbtCompound data = ((MarkerEntityAccessor) marker).getData();

            if (!data.contains(STRENGTH_KEY, NbtElement.DOUBLE_TYPE)) continue;

            return max(0.0, data.getDouble(STRENGTH_KEY));
        }

        return 1.0;
    }

    public void setStrength(ServerWorld world, BlockPos pos, double strength) {
        strength = max(0.0, strength);

        var markers = world.getEntitiesByClass(MarkerEntity.class, new Box(pos), marker -> true);

        boolean found = false;

        for (MarkerEntity marker : markers) {
            NbtCompound data = ((MarkerEntityAccessor) marker).getData();

            if (!data.contains(STRENGTH_KEY, NbtElement.DOUBLE_TYPE)) continue;

            if (found) {
                marker.discard();
                continue;
            }

            found = true;
            data.putDouble(STRENGTH_KEY, strength);
        }

        if (found) return;

        var marker = new MarkerEntity(EntityType.MARKER, world);
        marker.setPosition(pos.toCenterPos());

        NbtCompound data = ((MarkerEntityAccessor) marker).getData();
        data.putDouble(STRENGTH_KEY, strength);

        world.spawnEntity(marker);
    }

    private static OptionalDouble unsignedDouble(String input) {
        try {
            double d = Double.parseDouble(input.trim().replace(',', '.'));

            return d >= 0 ? OptionalDouble.of(d) : OptionalDouble.empty();
        } catch (NumberFormatException ignored) {
            return OptionalDouble.empty();
        }
    }

    public boolean isMarker(Entity entity) {
        if (!(entity instanceof MarkerEntity marker)) return false;

        NbtCompound data = ((MarkerEntityAccessor) marker).getData();

        return data.contains(STRENGTH_KEY);
    }
}
