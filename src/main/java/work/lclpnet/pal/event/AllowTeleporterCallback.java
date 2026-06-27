package work.lclpnet.pal.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import work.lclpnet.kibu.hook.Hook;
import work.lclpnet.kibu.hook.HookFactory;

public interface AllowTeleporterCallback {

    Hook<AllowTeleporterCallback> HOOK = HookFactory.createArrayBacked(AllowTeleporterCallback.class, hooks -> (player, from, to) -> {
        boolean allow = true;

        for (var hook : hooks) {
            if (!hook.canUseTeleporter(player, from, to)) {
                allow = false;
            }
        }

        return allow;
    });

    boolean canUseTeleporter(ServerPlayer player, BlockPos from, BlockPos to);
}
