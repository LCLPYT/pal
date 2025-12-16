package work.lclpnet.pal.event;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import work.lclpnet.kibu.access.VelocityModifier;
import work.lclpnet.kibu.hook.HookListenerModule;
import work.lclpnet.kibu.hook.HookRegistrar;
import work.lclpnet.kibu.hook.ServerTickHooks;
import work.lclpnet.kibu.hook.entity.PlayerInteractionHooks;
import work.lclpnet.kibu.hook.entity.ServerEntityHooks;
import work.lclpnet.kibu.hook.entity.ServerLivingEntityHooks;
import work.lclpnet.kibu.hook.player.PlayerJumpCallback;
import work.lclpnet.kibu.hook.player.PlayerSneakCallback;
import work.lclpnet.kibu.hook.util.OnGroundDetector;
import work.lclpnet.kibu.hook.world.PressurePlateCallback;
import work.lclpnet.kibu.scheduler.api.Scheduler;
import work.lclpnet.kibu.translate.Translations;
import work.lclpnet.pal.config.PalConfig;
import work.lclpnet.pal.util.ContraptionService;
import work.lclpnet.pal.util.MarkerConfigurator;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static java.lang.Math.abs;
import static java.lang.Math.round;
import static net.minecraft.util.Mth.floor;

public class PlateListener implements HookListenerModule {

    private static final double PLATFORM_TRIGGER_DIST = 1.1d;

    private final PalConfig config;
    private final Scheduler scheduler;
    private final Set<UUID> noFall = new HashSet<>(), padCooldown = new HashSet<>(), teleporterCooldown = new HashSet<>();
    private final Translations translations;
    private final MarkerConfigurator markerConfigurator;
    private final ContraptionService contraptionService;

    @Inject
    public PlateListener(PalConfig config, Scheduler scheduler, Translations translations, MarkerConfigurator markerConfigurator,
                         ContraptionService contraptionService) {
        this.config = config;
        this.scheduler = scheduler;
        this.translations = translations;
        this.markerConfigurator = markerConfigurator;
        this.contraptionService = contraptionService;
    }

    @Override
    public void registerListeners(HookRegistrar registrar) {
        registrar.registerHook(PressurePlateCallback.HOOK, this::onPressurePlate);
        registrar.registerHook(ServerLivingEntityHooks.ALLOW_DAMAGE, this::allowDamage);
        registrar.registerHook(ServerTickHooks.END_SERVER_TICK, this::serverTickEnd);
        registrar.registerHook(ServerEntityHooks.ENTITY_LOAD, this::cleanUpMarker);

        registrar.registerHook(PlayerJumpCallback.HOOK, (player) -> {
            onJump(player);
            return false;
        });

        registrar.registerHook(PlayerSneakCallback.HOOK, (player, sneaking) -> {
            onSneak(player, sneaking);
            return false;
        });

        registrar.registerHook(PlayerInteractionHooks.USE_BLOCK, this::onRightClickBlock);
    }

    private void cleanUpMarker(Entity entity, ServerLevel world) {
        if (entity.isRemoved() || !markerConfigurator.isPalMarker(entity)) return;

        BlockPos pos = entity.blockPosition();

        if (contraptionService.isBoosterPlate(world, pos)) return;

        var mutPos = pos.mutable();

        if (contraptionService.isJumpPad(world, mutPos)) return;

        mutPos.set(pos);

        if (contraptionService.isElevator(world, mutPos)) return;

        // host no longer present
        entity.discard();
    }

    private boolean onPressurePlate(Level world, BlockPos pos, Entity entity) {
        if (!config.enablePlates
                || !(entity instanceof ServerPlayer player)
                || !contraptionService.isBoosterPlate(world, pos)) {
            return false;
        }

        var markerData = markerConfigurator.getMarkerData(world, pos);
        double horizontal = markerConfigurator.getStrength(markerData, MarkerConfigurator.Property.HORIZONTAL_STRENGTH);
        double vertical = markerConfigurator.getStrength(markerData, MarkerConfigurator.Property.VERTICAL_STRENGTH);

        Vec3 rotation = player.getLookAngle();
        Vec3 velocity = rotation.scale(config.plateStrength * horizontal)
                .with(Direction.Axis.Y, config.plateStrength * vertical);

        VelocityModifier.setVelocity(player, velocity);

        preventFallDamageOnce(player);

        return true;
    }

    private boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayer player) || !source.is(DamageTypes.FALL)) {
            return true;
        }

        synchronized (this) {
            return !noFall.remove(player.getUUID());
        }
    }

    private synchronized void preventFallDamageOnce(ServerPlayer player) {
        noFall.add(player.getUUID());
    }

    private synchronized void serverTickEnd(MinecraftServer server) {
        PlayerList manager = server.getPlayerList();

        noFall.removeIf(uuid -> {
            ServerPlayer player = manager.getPlayer(uuid);

            return player == null || player.hasDisconnected() || !player.isAlive() || (OnGroundDetector.isOnGroundServer(player) && (player.fallDistance <= 0));
        });
    }

    private void onJump(ServerPlayer player) {
        if (!player.onGround() || !(player.level() instanceof ServerLevel world)) return;

        Vec3 pos = player.position();
        var blockPos = new BlockPos.MutableBlockPos();

        if (config.enablePads && contraptionService.findJumpPad(world, pos, blockPos, PLATFORM_TRIGGER_DIST)) {
            handleJumpPad(player, world, blockPos);
            return;
        }

        if (config.enableTeleporters) {
            blockPos.set(floor(pos.x()), floor(pos.y()) - 1, floor(pos.z()));

            if (contraptionService.isTeleporter(world, blockPos)
                    && !teleporterCooldown.contains(player.getUUID())
                    && findTeleporterAbove(world, blockPos)) {

                useTeleporter(player, world, blockPos);
            }
        }
    }

    private void onSneak(ServerPlayer player, boolean sneaking) {
        if (!sneaking || player.getAbilities().flying || !(player.level() instanceof ServerLevel world)) return;

        Vec3 pos = player.position();
        var blockPos = new BlockPos.MutableBlockPos();

        if (config.enableElevators && contraptionService.findElevator(world, pos, blockPos, PLATFORM_TRIGGER_DIST)) {
            useElevator(player, world, blockPos);
            return;
        }

        if (config.enableTeleporters) {
            blockPos.set(floor(pos.x()), floor(pos.y()) - 1, floor(pos.z()));

            if (contraptionService.isTeleporter(world, blockPos)
                    && !teleporterCooldown.contains(player.getUUID())
                    && findTeleporterBelow(world, blockPos)) {

                useTeleporter(player, world, blockPos);
            }
        }
    }

    private void useElevator(ServerPlayer player, ServerLevel world, BlockPos.MutableBlockPos pos) {
        var markerData = markerConfigurator.getMarkerData(world, pos);

        double durationSeconds = Optional.ofNullable(markerData)
                .flatMap(data -> data.value(MarkerConfigurator.Property.DURATION))
                .orElse(10.0);

        int durationTicks = (int) round(durationSeconds * 20);

        if (durationTicks <= 0) return;

        double strength = calculatePadStrength(world, pos, markerData, config.elevatorLegacyAmount);
        int amplifier = (int) (strength * 5) + 10;

        player.removeEffect(MobEffects.LEVITATION);
        player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, durationTicks, amplifier));

        Vec3 velocity = player.getDeltaMovement();
        velocity = new Vec3(0, velocity.y(), 0);
        VelocityModifier.setVelocity(player, velocity);

        final double startX = player.getX(), startZ = player.getZ();

        scheduler.interval(task -> {
            double x = player.getX(), y = player.getY(), z = player.getZ();

            if (abs(x - startX) > PLATFORM_TRIGGER_DIST || abs(z - startZ) > PLATFORM_TRIGGER_DIST) {
                player.removeEffect(MobEffects.LEVITATION);
            }

            MobEffectInstance effect = player.getEffect(MobEffects.LEVITATION);

            if (effect == null) {
                task.cancel();

                var particle = new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.PURPUR_BLOCK.defaultBlockState());
                world.sendParticles(particle, x, y, z, 100, 1, 1, 1, 0);
                world.playSound(null, x, y, z, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.PLAYERS, 2, 1);

                preventFallDamageOnce(player);

                return;
            }

            int duration = effect.getDuration();

            if (duration > 45) {
                world.sendParticles(ParticleTypes.FIREWORK, x, y + 0.75, z, 5, 0.1, 0.1, 0.1, 0.25);
            } else if (duration == 40) {
                world.sendParticles(ParticleTypes.FIREWORK, x, y, z, 20, 0.1, 0.1, 0.1, 0);
            } else if (duration == 30) {
                world.sendParticles(ParticleTypes.FIREWORK, x, y, z, 15, 0.1, 0.1, 0.1, 0);
            } else if (duration == 20) {
                world.sendParticles(ParticleTypes.FIREWORK, x, y, z, 10, 0.1, 0.1, 0.1, 0);
            } else if (duration == 10) {
                world.sendParticles(ParticleTypes.FIREWORK, x, y, z, 5, 0.1, 0.1, 0.1, 0);
            }
        }, 1, 0);
    }

    private void handleJumpPad(ServerPlayer player, ServerLevel world, BlockPos.MutableBlockPos pos) {
        UUID uuid = player.getUUID();

        if (padCooldown.contains(uuid)) return;

        var markerData = markerConfigurator.getMarkerData(world, pos);
        double amount = calculatePadStrength(world, pos, markerData, config.padLegacyAmount);

        Vec3 velocity = player.getDeltaMovement();
        velocity = new Vec3(velocity.x(), amount, velocity.z());
        VelocityModifier.setVelocity(player, velocity);
        player.hurtMarked = true;
        player.hasImpulse = true;

        preventFallDamageOnce(player);

        padCooldown.add(uuid);

        scheduler.timeout(() -> padCooldown.remove(uuid), 5);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 3, 2);
    }

    private double calculatePadStrength(ServerLevel world, BlockPos.MutableBlockPos pos, MarkerConfigurator.Data data, boolean legacy) {
        double scale = markerConfigurator.getStrength(data, MarkerConfigurator.Property.STRENGTH);

        int emeraldBlocks = countBlocks(world, pos);

        double base;

        if (legacy) {
            base = emeraldBlocks + 1;
        } else if (emeraldBlocks <= 0) {
            base = 1;
        } else {
            base = 1.25 + emeraldBlocks / 5d;
        }

        return base * scale;
    }

    private int countBlocks(Level world, BlockPos.MutableBlockPos pos) {
        final int minY = world.getMinY();

        int i = 0;

        for (int y = pos.getY() - 1; y >= minY; y--) {
            pos.setY(y);
            BlockState state = world.getBlockState(pos);

            if (!state.is(Blocks.EMERALD_BLOCK)) break;

            i++;
        }

        return i;
    }


    private void useTeleporter(ServerPlayer player, ServerLevel world, BlockPos target) {
        if (!hasSpaceOn(world, player, target)) {
            player.sendSystemMessage(translations.translateText(player, "pal.teleporter.blocked").formatted(ChatFormatting.RED));
            return;
        }

        double destX = target.getX() + 0.5, destY = target.getY() + 1, destZ = target.getZ() + 0.5;

        UUID uuid = player.getUUID();
        teleporterCooldown.add(uuid);

        scheduler.timeout(() -> teleporterCooldown.remove(uuid), 5);

        player.teleportTo(destX, destY, destZ);
        world.playSound(null, destX, destY, destZ, SoundEvents.POLAR_BEAR_STEP, SoundSource.PLAYERS, 0.5f, 2f);
        world.sendParticles(ParticleTypes.CLOUD, destX, destY, destZ, 25, 0.2, 0.2, 0.2d, 0.05d);

        Vec3 velocity = player.getDeltaMovement();
        velocity = new Vec3(velocity.x(), 0, velocity.z());
        VelocityModifier.setVelocity(player, velocity);
    }

    private boolean findTeleporterBelow(ServerLevel world, BlockPos.MutableBlockPos pos) {
        final int minY = world.getMinY();

        for (int y = pos.getY() - 1; y >= minY; y--) {
            pos.setY(y);

            if (contraptionService.isTeleporter(world, pos)) {
                return true;
            }
        }

        return false;
    }

    private boolean findTeleporterAbove(ServerLevel world, BlockPos.MutableBlockPos pos) {
        final int maxY = world.getMaxY();

        for (int y = pos.getY() + 1; y <= maxY; y++) {
            pos.setY(y);

            if (contraptionService.isTeleporter(world, pos)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasSpaceOn(Level world, ServerPlayer player, BlockPos target) {
        Vec3 pos = new Vec3(target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5);
        Vec3 diff = pos.subtract(player.position());
        AABB box = player.getBoundingBox().move(diff);

        return StreamSupport.stream(world.getCollisions(null, box).spliterator(), false)
                .findAny().isEmpty();
    }

    private InteractionResult onRightClickBlock(Player _player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        if (_player instanceof ServerPlayer player && player.isCreative() && hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty()) {
            return checkEditClick(world, hitResult, player);
        }

        return InteractionResult.PASS;
    }

    private InteractionResult checkEditClick(Level world, BlockHitResult hitResult, ServerPlayer player) {
        var blockPos = hitResult.getBlockPos().mutable();

        if (contraptionService.isBoosterPlate(world, blockPos)) {
            markerConfigurator.editBoosterPlate(player, blockPos);
            return InteractionResult.SUCCESS_SERVER;
        }

        if (contraptionService.findJumpPad(world, hitResult.getLocation(), blockPos, 1.51)) {
            markerConfigurator.editStrength(player, blockPos, MarkerConfigurator.Property.STRENGTH);

            return InteractionResult.SUCCESS_SERVER;
        }

        if (contraptionService.findElevator(world, hitResult.getLocation(), blockPos, 1.51)) {
            markerConfigurator.editElevator(player, blockPos);
        }

        return InteractionResult.PASS;
    }
}
