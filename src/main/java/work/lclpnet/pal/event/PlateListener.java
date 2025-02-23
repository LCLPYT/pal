package work.lclpnet.pal.event;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.*;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import work.lclpnet.kibu.access.VelocityModifier;
import work.lclpnet.kibu.hook.HookListenerModule;
import work.lclpnet.kibu.hook.HookRegistrar;
import work.lclpnet.kibu.hook.ServerTickHooks;
import work.lclpnet.kibu.hook.entity.ServerLivingEntityHooks;
import work.lclpnet.kibu.hook.player.PlayerJumpCallback;
import work.lclpnet.kibu.hook.player.PlayerSneakCallback;
import work.lclpnet.kibu.hook.util.OnGroundDetector;
import work.lclpnet.kibu.hook.world.PressurePlateCallback;
import work.lclpnet.kibu.scheduler.api.Scheduler;
import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.pal.config.PalConfig;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import static java.lang.Math.*;
import static net.minecraft.util.math.MathHelper.floor;

public class PlateListener implements HookListenerModule {

    private static final double PLATFORM_TRIGGER_DIST = 1.d;

    private final PalConfig config;
    private final Scheduler scheduler;
    private final Set<UUID> noFall = new HashSet<>();
    private final Set<UUID> padCooldown = new HashSet<>(), teleporterCooldown = new HashSet<>();
    private final Translations translations;

    @Inject
    public PlateListener(PalConfig config, Scheduler scheduler, Translations translations) {
        this.config = config;
        this.scheduler = scheduler;
        this.translations = translations;
    }

    @Override
    public void registerListeners(HookRegistrar registrar) {
        registrar.registerHook(PressurePlateCallback.HOOK, this::onPressurePlate);
        registrar.registerHook(ServerLivingEntityHooks.ALLOW_DAMAGE, this::allowDamage);
        registrar.registerHook(ServerTickHooks.END_SERVER_TICK, this::serverTickEnd);

        registrar.registerHook(PlayerJumpCallback.HOOK, (player) -> {
            onJump(player);
            return false;
        });

        registrar.registerHook(PlayerSneakCallback.HOOK, (player, sneaking) -> {
            onSneak(player, sneaking);
            return false;
        });
    }

    private boolean onPressurePlate(World world, BlockPos pos, Entity entity) {
        if (!config.enablePlates
                || !(entity instanceof ServerPlayerEntity player)
                || !world.getBlockState(pos).isOf(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE)) {
            return false;
        }

        BlockState below = world.getBlockState(pos.down());

        if (!below.isOf(Blocks.GOLD_BLOCK)) return false;

        Vec3d rotation = player.getRotationVector();
        rotation.multiply(config.plateStrength);

        Vec3d velocity = new Vec3d(rotation.getX(), config.plateMotionY, rotation.getZ());
        VelocityModifier.setVelocity(player, velocity);

        preventFallDamageOnce(player);

        return true;
    }

    private boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayerEntity player) || !source.isOf(DamageTypes.FALL)) {
            return true;
        }

        synchronized (this) {
            return !noFall.remove(player.getUuid());
        }
    }

    private synchronized void preventFallDamageOnce(ServerPlayerEntity player) {
        noFall.add(player.getUuid());
    }

    private synchronized void serverTickEnd(MinecraftServer server) {
        PlayerManager manager = server.getPlayerManager();

        noFall.removeIf(uuid -> {
            ServerPlayerEntity player = manager.getPlayer(uuid);

            return player == null || player.isDisconnected() || !player.isAlive() || (OnGroundDetector.isOnGroundServer(player) && (player.fallDistance <= 0));
        });
    }

    private void onJump(ServerPlayerEntity player) {
        if (!player.isOnGround() || !(player.getWorld() instanceof ServerWorld world)) return;

        Vec3d pos = player.getPos();
        var blockPos = new BlockPos.Mutable();

        if (config.enablePads && findPad(world, pos, blockPos)) {
            handleJumpPad(player, world, blockPos);
            return;
        }

        if (config.enableTeleporters) {
            blockPos.set(floor(pos.getX()), floor(pos.getY()) - 1, floor(pos.getZ()));

            if (isTeleporter(world, blockPos)
                    && !teleporterCooldown.contains(player.getUuid())
                    && findTeleporterAbove(world, blockPos)) {

                useTeleporter(player, world, blockPos);
            }
        }
    }

    private void onSneak(ServerPlayerEntity player, boolean sneaking) {
        if (!sneaking || player.getAbilities().flying || !(player.getWorld() instanceof ServerWorld world)) return;

        Vec3d pos = player.getPos();
        var blockPos = new BlockPos.Mutable();

        if (config.enableElevators && findElevator(world, pos, blockPos)) {
            useElevator(player, world, blockPos);
            return;
        }

        if (config.enableTeleporters) {
            blockPos.set(floor(pos.getX()), floor(pos.getY()) - 1, floor(pos.getZ()));

            if (isTeleporter(world, blockPos)
                    && !teleporterCooldown.contains(player.getUuid())
                    && findTeleporterBelow(world, blockPos)) {

                useTeleporter(player, world, blockPos);
            }
        }
    }

    private void useElevator(ServerPlayerEntity player, ServerWorld world, BlockPos.Mutable pos) {
        double strength = calculatePadStrength(world, pos, config.elevatorLegacyAmount);

        player.removeStatusEffect(StatusEffects.LEVITATION);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 200, (int) (strength * 5) + 10));

        Vec3d velocity = player.getVelocity();
        velocity = new Vec3d(0, velocity.getY(), 0);
        VelocityModifier.setVelocity(player, velocity);

        final double startX = player.getX(), startZ = player.getZ();

        scheduler.interval(task -> {
            double x = player.getX(), y = player.getY(), z = player.getZ();

            if (abs(x - startX) > PLATFORM_TRIGGER_DIST || abs(z - startZ) > PLATFORM_TRIGGER_DIST) {
                player.removeStatusEffect(StatusEffects.LEVITATION);
            }

            StatusEffectInstance effect = player.getStatusEffect(StatusEffects.LEVITATION);

            if (effect == null) {
                task.cancel();

                var particle = new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, Blocks.PURPUR_BLOCK.getDefaultState());
                world.spawnParticles(particle, x, y, z, 100, 1, 1, 1, 0);
                world.playSound(null, x, y, z, SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 2, 1);

                preventFallDamageOnce(player);

                return;
            }

            int duration = effect.getDuration();

            if (duration > 45) {
                world.spawnParticles(ParticleTypes.FIREWORK, x, y + 0.75, z, 5, 0.1, 0.1, 0.1, 0.25);
            } else if (duration == 40) {
                world.spawnParticles(ParticleTypes.FIREWORK, x, y, z, 20, 0.1, 0.1, 0.1, 0);
            } else if (duration == 30) {
                world.spawnParticles(ParticleTypes.FIREWORK, x, y, z, 15, 0.1, 0.1, 0.1, 0);
            } else if (duration == 20) {
                world.spawnParticles(ParticleTypes.FIREWORK, x, y, z, 10, 0.1, 0.1, 0.1, 0);
            } else if (duration == 10) {
                world.spawnParticles(ParticleTypes.FIREWORK, x, y, z, 5, 0.1, 0.1, 0.1, 0);
            }
        }, 1, 0);
    }

    private void handleJumpPad(ServerPlayerEntity player, ServerWorld world, BlockPos.Mutable pos) {
        UUID uuid = player.getUuid();

        if (padCooldown.contains(uuid)) return;

        double amount = calculatePadStrength(world, pos, config.padLegacyAmount);

        Vec3d velocity = player.getVelocity();
        velocity = new Vec3d(velocity.getX(), amount, velocity.getZ());
        VelocityModifier.setVelocity(player, velocity);
        player.velocityModified = true;
        player.velocityDirty = true;

        preventFallDamageOnce(player);

        padCooldown.add(uuid);

        scheduler.timeout(() -> padCooldown.remove(uuid), 5);

        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 3, 2);
    }

    private double calculatePadStrength(World world, BlockPos.Mutable pos, boolean legacy) {
        int emeraldBlocks = countBlocks(world, pos);

        if (legacy) {
            return emeraldBlocks + 1;
        }

        if (emeraldBlocks <= 0) return 1;

        return 1.25 + emeraldBlocks / 5d;
    }

    private int countBlocks(World world, BlockPos.Mutable pos) {
        final int minY = world.getBottomY();

        int i = 0;

        for (int y = pos.getY() - 1; y >= minY; y--) {
            pos.setY(y);
            BlockState state = world.getBlockState(pos);

            if (!state.isOf(Blocks.EMERALD_BLOCK)) break;

            i++;
        }

        return i;
    }

    private boolean findPad(BlockView world, Vec3d pos, BlockPos.Mutable blockPos) {
        // if there is no valid block underneath, terminate early
        blockPos.set(floor(pos.x), floor(pos.y) - 1, floor(pos.z));
        BlockState state = world.getBlockState(blockPos);

        if (!state.isOf(Blocks.PISTON) && !state.isOf(Blocks.IRON_BLOCK)) {
            return false;
        }

        return find3x3(pos, blockPos, p -> isPad(world, p));
    }

    private boolean findElevator(BlockView world, Vec3d pos, BlockPos.Mutable blockPos) {
        // if there is no valid block underneath, terminate early
        blockPos.set(floor(pos.x), floor(pos.y) - 1, floor(pos.z));
        BlockState state = world.getBlockState(blockPos);

        if (!state.isOf(Blocks.PISTON) && !state.isOf(Blocks.DIAMOND_BLOCK) && !state.isOf(Blocks.BEACON)) {
            return false;
        }

        return find3x3(pos, blockPos, p -> isElevator(world, p));
    }

    private boolean find3x3(Vec3d pos, BlockPos.Mutable blockPos, Predicate<BlockPos.Mutable> predicate) {
        int x = floor(pos.x);
        int y = floor(pos.y) - 1;
        int z = floor(pos.z);

        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                double distToCenter = max(abs(x + ox + 0.5 - pos.getX()), abs(z + oz + 0.5 - pos.getZ()));

                if (distToCenter > PLATFORM_TRIGGER_DIST) continue;

                blockPos.set(x + ox, y, z + oz);

                if (predicate.test(blockPos)) {
                    blockPos.set(x + ox, y, z + oz);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isPad(BlockView world, BlockPos.Mutable pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        BlockState state = world.getBlockState(pos);

        if (!state.isOf(Blocks.PISTON) || state.get(PistonBlock.FACING) != Direction.UP || !isSurroundedByPistons(world, pos)) {
            return false;
        }

        pos.set(x, y, z);

        return isCorneredBy(pos, p -> world.getBlockState(p).isOf(Blocks.IRON_BLOCK));
    }

    private boolean isElevator(BlockView world, BlockPos.Mutable pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        BlockState state = world.getBlockState(pos);

        if (!state.isOf(Blocks.BEACON) || !isSurroundedByPistons(world, pos)) {
            return false;
        }

        pos.set(x, y, z);

        return isCorneredBy(pos, p -> world.getBlockState(p).isOf(Blocks.DIAMOND_BLOCK));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isSurroundedByPistons(BlockView world, BlockPos.Mutable pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        for (Direction direction : Direction.Type.HORIZONTAL) {
            pos.set(x + direction.getOffsetX(), y, z + direction.getOffsetZ());

            BlockState state = world.getBlockState(pos);

            if (!state.isOf(Blocks.PISTON) || state.get(PistonBlock.FACING) != direction.getOpposite()) {
                return false;
            }
        }

        return true;
    }

    private boolean isCorneredBy(BlockPos.Mutable pos, Predicate<BlockPos> predicate) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        // check (-1, -1), (1, -1), (-1, 1), (1, 1)
        for (int i = 0; i < 4; i++) {
            int ox = ((i & 1) << 1) - 1;
            int oz = (i & 2) - 1;

            pos.set(x + ox, y, z + oz);

            if (!predicate.test(pos)) {
                return false;
            }
        }

        return true;
    }

    private void useTeleporter(ServerPlayerEntity player, ServerWorld world, BlockPos target) {
        if (!hasSpaceOn(world, player, target)) {
            player.sendMessage(translations.translateText(player, "pal.teleporter.blocked").formatted(Formatting.RED));
            return;
        }

        double destX = target.getX() + 0.5, destY = target.getY() + 1, destZ = target.getZ() + 0.5;

        UUID uuid = player.getUuid();
        teleporterCooldown.add(uuid);

        scheduler.timeout(() -> teleporterCooldown.remove(uuid), 5);

        player.requestTeleport(destX, destY, destZ);
        world.playSound(null, destX, destY, destZ, SoundEvents.ENTITY_POLAR_BEAR_STEP, SoundCategory.PLAYERS, 0.5f, 2f);
        world.spawnParticles(ParticleTypes.CLOUD, destX, destY, destZ, 25, 0.2, 0.2, 0.2d, 0.05d);

        Vec3d velocity = player.getVelocity();
        velocity = new Vec3d(velocity.getX(), 0, velocity.getZ());
        VelocityModifier.setVelocity(player, velocity);
    }

    private boolean findTeleporterBelow(ServerWorld world, BlockPos.Mutable pos) {
        final int minY = world.getBottomY();

        for (int y = pos.getY() - 1; y >= minY; y--) {
            pos.setY(y);

            if (isTeleporter(world, pos)) {
                return true;
            }
        }

        return false;
    }

    private boolean findTeleporterAbove(ServerWorld world, BlockPos.Mutable pos) {
        final int maxY = world.getTopYInclusive();

        for (int y = pos.getY() + 1; y <= maxY; y++) {
            pos.setY(y);

            if (isTeleporter(world, pos)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasSpaceOn(World world, ServerPlayerEntity player, BlockPos target) {
        Vec3d pos = new Vec3d(target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5);
        Vec3d diff = pos.subtract(player.getPos());
        Box box = player.getBoundingBox().offset(diff);

        return StreamSupport.stream(world.getCollisions(null, box).spliterator(), false)
                .findAny().isEmpty();
    }

    private boolean isTeleporter(World world, BlockPos blockPos) {
        BlockState state = world.getBlockState(blockPos);

        return state.isOf(Blocks.LAPIS_BLOCK) && world.isReceivingRedstonePower(blockPos);
    }
}
