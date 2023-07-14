package work.lclpnet.pal.event;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import work.lclpnet.kibu.access.VelocityModifier;
import work.lclpnet.kibu.hook.ServerTickHooks;
import work.lclpnet.kibu.hook.entity.ServerLivingEntityHooks;
import work.lclpnet.kibu.hook.world.PressurePlateCallback;
import work.lclpnet.kibu.plugin.hook.HookListenerModule;
import work.lclpnet.kibu.plugin.hook.HookRegistrar;
import work.lclpnet.pal.config.ConfigAccess;
import work.lclpnet.pal.config.PalConfig;

import javax.inject.Inject;
import java.util.WeakHashMap;

public class PlateListener implements HookListenerModule {

    private final ConfigAccess configAccess;
    private final WeakHashMap<Entity, Void> noFall = new WeakHashMap<>();

    @Inject
    public PlateListener(ConfigAccess configAccess) {
        this.configAccess = configAccess;
    }

    @Override
    public void registerListeners(HookRegistrar registrar) {
        registrar.registerHook(PressurePlateCallback.HOOK, this::onPressurePlate);
        registrar.registerHook(ServerLivingEntityHooks.ALLOW_DAMAGE, this::allowDamage);
        registrar.registerHook(ServerTickHooks.END_SERVER_TICK, this::serverTickEnd);
    }

    private boolean onPressurePlate(World world, BlockPos pos, Entity entity) {
        PalConfig config = configAccess.getConfig();
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
}
