package work.lclpnet.pal.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import work.lclpnet.kibu.hook.Hook;
import work.lclpnet.kibu.hook.HookFactory;

public interface AllowJumpPadCallback {

    Hook<AllowJumpPadCallback> HOOK = HookFactory.createArrayBacked(AllowJumpPadCallback.class, hooks -> (player, pos) -> {
        boolean allow = true;

        for (var hook : hooks) {
            if (!hook.canUseJumPad(player, pos)) {
                allow = false;
            }
        }

        return allow;
    });

    boolean canUseJumPad(ServerPlayer player, BlockPos pos);
}
