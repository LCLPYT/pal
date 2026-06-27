package work.lclpnet.pal.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import work.lclpnet.kibu.hook.Hook;
import work.lclpnet.kibu.hook.HookFactory;

public interface AllowElevatorCallback {

    Hook<AllowElevatorCallback> HOOK = HookFactory.createArrayBacked(AllowElevatorCallback.class, hooks -> (player, pos) -> {
        boolean allow = true;

        for (var hook : hooks) {
            if (!hook.canUseElevator(player, pos)) {
                allow = false;
            }
        }

        return allow;
    });

    boolean canUseElevator(ServerPlayer player, BlockPos pos);
}
