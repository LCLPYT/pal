package work.lclpnet.pal.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import work.lclpnet.kibu.hook.Hook;
import work.lclpnet.kibu.hook.HookFactory;

public interface AllowBoosterPlateCallback {

    Hook<AllowBoosterPlateCallback> HOOK = HookFactory.createArrayBacked(AllowBoosterPlateCallback.class, hooks -> (player, pos) -> {
        boolean allow = true;

        for (var hook : hooks) {
            if (!hook.canUseBoosterPlate(player, pos)) {
                allow = false;
            }
        }

        return allow;
    });

    boolean canUseBoosterPlate(ServerPlayer player, BlockPos pos);
}
