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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import work.lclpnet.kibu.access.VelocityModifier;
import work.lclpnet.kibu.hook.ServerTickHooks;
import work.lclpnet.kibu.hook.entity.ServerLivingEntityHooks;
import work.lclpnet.kibu.hook.player.PlayerMoveCallback;
import work.lclpnet.kibu.hook.player.PlayerSneakCallback;
import work.lclpnet.kibu.hook.util.PositionRotation;
import work.lclpnet.kibu.hook.world.PressurePlateCallback;
import work.lclpnet.kibu.plugin.hook.HookListenerModule;
import work.lclpnet.kibu.plugin.hook.HookRegistrar;
import work.lclpnet.kibu.scheduler.api.Scheduler;
import work.lclpnet.kibu.translate.TranslationService;
import work.lclpnet.pal.config.PalConfig;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.stream.StreamSupport;

public class PlateListener implements HookListenerModule {

    private final PalConfig config;
    private final Scheduler scheduler;
    private final WeakHashMap<Entity, Void> noFall = new WeakHashMap<>();
    private final Set<UUID> padCooldown = new HashSet<>(), teleporterCooldown = new HashSet<>();
    private final TranslationService translationService;

    @Inject
    public PlateListener(PalConfig config, Scheduler scheduler, TranslationService translationService) {
        this.config = config;
        this.scheduler = scheduler;
        this.translationService = translationService;
    }

    @Override
    public void registerListeners(HookRegistrar registrar) {
        registrar.registerHook(PressurePlateCallback.HOOK, this::onPressurePlate);
        registrar.registerHook(ServerLivingEntityHooks.ALLOW_DAMAGE, this::allowDamage);
        registrar.registerHook(ServerTickHooks.END_SERVER_TICK, this::serverTickEnd);

        registrar.registerHook(PlayerMoveCallback.HOOK, (player, from, to) -> {
            onMove(player, from, to);
            return false;
        });

        registrar.registerHook(PlayerSneakCallback.HOOK, (player, sneaking) -> {
            onSneak(player, sneaking);
            return false;
        });
    }

    private boolean onPressurePlate(World world, BlockPos pos, Entity entity) {
        if (!config.enablePlates) return false;

        if (!(entity instanceof PlayerEntity player) || !world.getBlockState(pos).isOf(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE)) return false;

        BlockState below = world.getBlockState(pos.down());
        if (!below.isOf(Blocks.GOLD_BLOCK)) return false;

        Vec3d rotation = player.getRotationVector();
        rotation.multiply(config.plateStrength);

        Vec3d velocity = new Vec3d(rotation.getX(), config.plateMotionY, rotation.getZ());
        VelocityModifier.setVelocity(player, velocity);

        synchronized (this) {
            noFall.put(player, null);
        }

        return true;
    }

    private boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!source.isOf(DamageTypes.FALL)) return true;

        synchronized (this) {
            if (!noFall.containsKey(entity)) return true;

            noFall.remove(entity);
            return false;
        }
    }

    private void serverTickEnd(MinecraftServer server) {
        synchronized (this) {
            noFall.keySet().removeIf(entity -> !entity.isAlive() || entity.isOnGround() && entity.fallDistance <= 0);
        }
    }

    private void onMove(ServerPlayerEntity player, PositionRotation from, PositionRotation to) {
        if (!player.isOnGround() || from.getY() >= to.getY() || !(player.getWorld() instanceof ServerWorld world)) return;  // no jump

        BlockPos down = player.getBlockPos().down();

        if (config.enablePads && isPad(world, down)) {
            handleJumpPad(player, world, down);
            return;
        }

        if (config.enableTeleporters && isTeleporter(world, down)) {
            if (teleporterCooldown.contains(player.getUuid())) return;

            BlockPos target = findTeleporterAbove(world, down);
            if (target == null) return;

            useTeleporter(player, world, target);
        }
    }

    private void onSneak(ServerPlayerEntity player, boolean sneaking) {
        if (!sneaking || player.getAbilities().flying || !(player.getWorld() instanceof ServerWorld world)) return;

        BlockPos down = player.getBlockPos().down();

        if (config.enableTeleporters && isTeleporter(world, down)) {
            if (teleporterCooldown.contains(player.getUuid())) return;

            BlockPos target = findTeleporterBelow(world, down);
            if (target == null) return;

            useTeleporter(player, world, target);
        }

        if (config.enableElevators && isElevator(world, down)) {
            useElevator(player, world, down);
        }
    }

    private void useElevator(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        double amount = calculatePadAmount(world, pos, config.elevatorLegacyAmount);

        player.removeStatusEffect(StatusEffects.LEVITATION);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 200, (int) (amount * 5) + 10));

        int startX = pos.getX(), startZ = pos.getZ();

        scheduler.interval(task -> {
            double x = player.getX(), y = player.getY(), z = player.getZ();

            if (Math.floor(x) != startX || Math.floor(z) != startZ) {
                player.removeStatusEffect(StatusEffects.LEVITATION);
            }

            StatusEffectInstance effect = player.getStatusEffect(StatusEffects.LEVITATION);

            if (effect == null) {
                task.cancel();

                var particle = new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, Blocks.PURPUR_BLOCK.getDefaultState());
                world.spawnParticles(particle, x, y, z, 100, 1, 1, 1, 0);
                world.playSound(null, x, y, z, SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 2, 1);

                synchronized (PlateListener.this) {
                    noFall.put(player, null);
                }

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

    private void handleJumpPad(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        UUID uuid = player.getUuid();

        if (padCooldown.contains(uuid)) return;

        double amount = calculatePadAmount(world, pos, config.padLegacyAmount);

        Vec3d velocity = new Vec3d(0, amount, 0);
        VelocityModifier.setVelocity(player, velocity);
        player.velocityModified = true;
        player.velocityDirty = true;

        synchronized (this) {
            noFall.put(player, null);
        }

        padCooldown.add(uuid);

        scheduler.timeout(() -> padCooldown.remove(uuid), 5);

        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 3, 2);
    }

    private double calculatePadAmount(World world, BlockPos pos, boolean legacy) {
        int emeraldBlocks = countBlocks(world, pos);

        if (legacy) {
            return emeraldBlocks + 1;
        }

        if (emeraldBlocks <= 0) return 1;

        return 1.25 + emeraldBlocks / 5d;
    }

    private int countBlocks(World world, BlockPos start) {
        BlockPos.Mutable pos = start.mutableCopy();
        BlockState state;
        int i = 0;

        for (int y = start.getY() - 1, minY = world.getBottomY(); y >= minY; y--) {
            pos.setY(y);
            state = world.getBlockState(pos);

            if (!state.isOf(Blocks.EMERALD_BLOCK)) break;

            i++;
        }

        return i;
    }

    private boolean isPad(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        if (!state.isOf(Blocks.PISTON) || state.get(PistonBlock.FACING) != Direction.UP || !isSurroundedByPistons(world, pos)) {
            return false;
        }

        return world.getBlockState(pos.add(1, 0, 1)).isOf(Blocks.IRON_BLOCK) &&
                world.getBlockState(pos.add(-1, 0, 1)).isOf(Blocks.IRON_BLOCK) &&
                world.getBlockState(pos.add(1, 0, -1)).isOf(Blocks.IRON_BLOCK) &&
                world.getBlockState(pos.add(-1, 0, -1)).isOf(Blocks.IRON_BLOCK);
    }

    private boolean isElevator(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!state.isOf(Blocks.BEACON) || !isSurroundedByPistons(world, pos)) return false;

        return world.getBlockState(pos.add(1, 0, 1)).isOf(Blocks.DIAMOND_BLOCK) &&
                world.getBlockState(pos.add(-1, 0, 1)).isOf(Blocks.DIAMOND_BLOCK) &&
                world.getBlockState(pos.add(1, 0, -1)).isOf(Blocks.DIAMOND_BLOCK) &&
                world.getBlockState(pos.add(-1, 0, -1)).isOf(Blocks.DIAMOND_BLOCK);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isSurroundedByPistons(World world, BlockPos pos) {
        Direction[] directions = new Direction[] { Direction.SOUTH, Direction.EAST, Direction.NORTH, Direction.WEST };
        BlockState state;

        for (Direction direction : directions) {
            state = world.getBlockState(pos.offset(direction));

            if (!state.isOf(Blocks.PISTON) || state.get(PistonBlock.FACING) != direction.getOpposite()) {
                return false;
            }
        }

        return true;
    }

    private void useTeleporter(ServerPlayerEntity player, ServerWorld world, BlockPos target) {
        if (!hasSpaceOn(world, player, target)) {
            player.sendMessage(translationService.translateText(player, "pal.teleporter.blocked").formatted(Formatting.RED));
            return;
        }

        double destX = target.getX() + 0.5, destY = target.getY() + 1, destZ = target.getZ() + 0.5;

        UUID uuid = player.getUuid();
        teleporterCooldown.add(uuid);

        scheduler.timeout(() -> teleporterCooldown.remove(uuid), 5);

        player.requestTeleport(destX, destY, destZ);
        world.playSound(null, destX, destY, destZ, SoundEvents.ENTITY_POLAR_BEAR_STEP, SoundCategory.PLAYERS, 0.5f, 2f);
        world.spawnParticles(ParticleTypes.CLOUD, destX, destY, destZ, 25, 0.2, 0.2, 0.2d, 0.05d);
    }

    @Nullable
    private BlockPos findTeleporterBelow(ServerWorld world, BlockPos start) {
        BlockPos.Mutable pos = start.mutableCopy();

        for (int y = start.getY() - 1, minY = world.getBottomY(); y >= minY; y--) {
            pos.setY(y);

            if (isTeleporter(world, pos)) {
                return pos.toImmutable();
            }
        }
        return null;
    }

    @Nullable
    private BlockPos findTeleporterAbove(ServerWorld world, BlockPos start) {
        BlockPos.Mutable pos = start.mutableCopy();

        for (int y = start.getY() + 1, maxY = world.getTopY(); y < maxY; y++) {
            pos.setY(y);

            if (isTeleporter(world, pos)) {
                return pos.toImmutable();
            }
        }

        return null;
    }

    private boolean hasSpaceOn(World world, ServerPlayerEntity player, BlockPos target) {
        Vec3d pos = new Vec3d(target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5);
        Vec3d diff = pos.subtract(player.getPos());
        Box box = player.getBoundingBox().offset(diff);

        return StreamSupport.stream(world.getCollisions(null, box).spliterator(), false)
                .findAny().isEmpty();
    }

    private boolean isTeleporter(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        return state.isOf(Blocks.LAPIS_BLOCK) && world.isReceivingRedstonePower(pos);
    }
}
